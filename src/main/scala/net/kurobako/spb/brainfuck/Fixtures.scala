package net.kurobako.spb.brainfuck

import net.kurobako.spb.Collectors.Result
import net.kurobako.spb.brainfuck.BrainFuck._

import scala.annotation.{switch, tailrec}

object Baselines {

	val recursiveDescent: String => Either[String, Int] = (input: String) => expr(input, 0)._1
	def expr(input: String, index: Int): (Either[String, Int], Int) = {
		one(input, index) match {
			case (Right(x), index_) => plus(input, index_) match {
				case (Right(op), index__) => expr(input, index__) match {
					case (Right(y), index___) => (Right(op(x)(y)), index___)
					case (err, index___)      => (err, index___)
				}
				case (_, index__)         => (Right(x), index__)
			}
			case (err, index_)      => (err, index_)
		}
	}
	def exprl(input: String, index: Int): (Either[String, Int], Int) = {
		one(input, index) match {
			case (Right(x), index_) =>
				val (ops, index__) = rep(plusone)(input, index_)
				(Right(ops.foldLeft(x)((acc, op) => op(acc))), index__)
			case err                => err
		}
	}
	@tailrec def rep[A](p: (String, Int) => (Either[String, A], Int))
					   (input: String, index: Int, acc: List[A] = Nil): (List[A], Int) = p(input, index) match {
		case (Right(x), index_) => rep(p)(input, index_, x :: acc)
		case (_, index_)        => (acc.reverse, index_)
	}
	def one(input: String, index: Int): (Either[String, Int], Int) = {
		if (index < input.length && input(index) == '1') (Right('1'.toInt), index + 1)
		else (Left(s"$index: Expected 1, got ${if (index < input.length) input(index) else "end of input"}"), index)
	}
	def plus(input: String, index: Int): (Either[String, Int => Int => Int], Int) = {
		if (index < input.length && input(index) == '+') (Right((x: Int) => (y: Int) => x + y), index + 1)
		else (Left(s"$index: Expected +, got ${if (index < input.length) input(index) else "end of input"}"), index)
	}
	def plusone(input: String, index: Int): (Either[String, Int => Int], Int) = {
		plus(input, index) match {
			case (Right(op), index_)  => one(input, index_) match {
				case (Right(y), index__)  => (Right((z: Int) => op(z)(y)), index__)
				case (Left(err), index__) => (Left(err), index__)
			}
			case (Left(err), index__) => (Left(err), index__)
		}
	}

	val parseTail: String => Int = (input: String) => parseTail_(input, 0, 0)
	@tailrec def parseTail_(input: String, index: Int, sum: Int): Int = {
		if (index >= input.length) sum
		else input(index) match {
			case c@'1' => parseTail_(input, index + 1, sum + c)
			case '+'   => parseTail_(input, index + 1, sum)
		}
	}

	def parse(in: String): Result[List[BrainFuckOp]] = {
		@tailrec def walk(in: List[Char], acc: List[BrainFuckOp] = Nil): Either[String, (List[BrainFuckOp], List[Char])] = in match {
			case (']' :: _) | Nil => Right((acc.reverse, in))
			case c :: rest        => (c: @switch) match {
				case '>' => walk(rest, RightPointer :: acc)
				case '<' => walk(rest, LeftPointer :: acc)
				case '+' => walk(rest, Increment :: acc)
				case '-' => walk(rest, Decrement :: acc)
				case '.' => walk(rest, Output :: acc)
				case ',' => walk(rest, Input :: acc)
				case '[' =>
					loop(rest) match {
						case Right((body, rest_)) => walk(rest_, Loop(body) :: acc)
						case x                    => x
					}
				case _   => walk(rest, acc)
			}
		}
		def loop(in: List[Char]): Either[String, (List[BrainFuckOp], List[Char])] = for {
			result <- walk(in)
			out <- result._2 match {
				case ']' :: rest_ => Right(result._1 -> rest_)
				case _            => Left("Unclosed loop :(")
			}
		} yield out
		walk(in.toList).right.map {_._1}
	}

}

object ParsleyFixtures {


	import parsley.Char._
	import parsley.Combinator._
	import parsley.Parsley._
	import parsley.{LanguageDef, Parsley, Predicate, TokenParser}

	final val _parser: Parsley[List[BrainFuckOp]] = parser()
	final def parser(): Parsley[List[BrainFuckOp]] = {
		val bflang = LanguageDef.plain.copy(space = Predicate(c => (c: @switch) match {
			case '>' | '<' | '+' | '-' | '.' | ',' | '[' | ']' => false
			case _                                             => true
		}))
		val tok = new TokenParser(bflang)
		lazy val bf: Parsley[List[BrainFuckOp]] =
			many(tok.lexeme('>' #> RightPointer)
				 <|> tok.lexeme('<' #> LeftPointer)
				 <|> tok.lexeme('+' #> Increment)
				 <|> tok.lexeme('-' #> Decrement)
				 <|> tok.lexeme('.' #> Output)
				 <|> tok.lexeme(',' #> Input)
				 <|> tok.brackets(bf.map(Loop)))
		tok.whiteSpace *> attempt(bf <* eof) <|> fail("\"]\" closes a loop, but there isn't one open")
	}
}

object FastParseFixtures {

	import fastparse.WhitespaceApi

	private final val White: WhitespaceApi.Wrapper = WhitespaceApi.Wrapper {
		import fastparse.all._
		val Reserved = "<>+-.,[]".toSet
		NoTrace(ElemsWhile(!Reserved.contains(_)).rep)
	}

	import White._
	import fastparse.noApi._

	type Parser[A] = fastparse.noApi.Parser[A]

	final val _parser: Parser[List[BrainFuckOp]] = parser()
	final def parser(): Parser[List[BrainFuckOp]] = {
		lazy val ops: Parser[BrainFuckOp] =
			CharIn("<>+-.,").!.map {
				case "<" => LeftPointer
				case ">" => RightPointer
				case "+" => Increment
				case "-" => Decrement
				case "." => Output
				case "," => Input
			}.opaque(s"keywords(<>+-.,)")

		lazy val loop: Parser[List[BrainFuckOp]] =
			P("[".opaque("Opening bracket '['") ~/
			  (expr | PassWith(Nil)).opaque("expression") ~ // [] is ok
			  "]".opaque("']' Closing bracket"))
				.map {Loop(_) :: Nil}

		lazy val expr: Parser[List[BrainFuckOp]] = (loop | ops.rep(1)).rep
			.map {_.flatten.toList} // empty should fail
		Start ~ expr ~ End
	}
}

object AttoFixtures {

	import atto.parser.character._
	import atto.parser.combinator._
	import atto.syntax.parser._

	type Parser[A] = atto.Parser[A]

	final val _parser: Parser[List[BrainFuckOp]] = parser()
	final def parser(): Parser[List[BrainFuckOp]] = {
		val ws = skipMany(noneOf("<>+-.,[]"))
		def tok[A](p: Parser[A]): Parser[A] = p <~ ws
		lazy val bf: Parser[List[BrainFuckOp]] =
			many(choice[BrainFuckOp](
				tok(char('>')) >| RightPointer,
				tok(char('<')) >| LeftPointer,
				tok(char('+')) >| Increment,
				tok(char('-')) >| Decrement,
				tok(char('.')) >| Output,
				tok(char(',')) >| Input
				| (tok(char('[')) ~> (bf -| (xs => Loop(xs): BrainFuckOp))) <~ tok(char(']'))))
		(ws ~> bf <~ endOfInput) | err("] closes a loop but there isn't one open")
	}

}

object ScalaParserCombinatorFixtures {

	import scala.util.parsing.combinator._

	final class SPCParser extends RegexParsers {

		override def skipWhitespace: Boolean = false

		val ws: Parser[List[Char]] = rep(acceptIf(c => (c: @switch) match {
			case '>' | '<' | '+' | '-' | '.' | ',' | '[' | ']' => false
			case _                                             => true
		})(_ => ""))

		val bf: Parser[List[BrainFuckOp]] = rep((accept("operator", {
			case '>' => RightPointer
			case '<' => LeftPointer
			case '+' => Increment
			case '-' => Decrement
			case '.' => Output
			case ',' => Input
		}) | (accept('[') ~> ws ~> (bf ^^ Loop) <~ accept(']'))) <~ ws)

	}

	final val _parser: SPCParser#Parser[List[BrainFuckOp]] = parser()
	final def parser(): SPCParser#Parser[List[BrainFuckOp]] = {
		val p = new SPCParser
		p.ws ~> p.bf
	}

}