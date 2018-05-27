package net.kurobako.spb.json

import net.kurobako.spb.brainfuck.BrainFuck._
import net.kurobako.spb.json.Json._

import scala.annotation.switch
import scala.util.parsing.json.Lexer


object ParsleyFixtures {


	import parsley.Combinator._
	import parsley.Parsley._
	import parsley.{Char, LanguageDef, Parsley, Predicate, TokenParser}

	final val _parser: Parsley[JsonExpr] = parser()

	final def parser(): Parsley[JsonExpr] = {
		val jsontoks = LanguageDef.plain.copy(space = Predicate(Char.isWhitespace))
		val tok = new TokenParser(jsontoks)
		lazy val obj: Parsley[Obj] = tok.braces(
			tok.commaSep(+(string <~> tok.colon *> value)).map(Obj))
		lazy val array: Parsley[Arr] = tok.brackets(tok.commaSep(value)).map(Arr)
		lazy val string: Parsley[Str] = tok.stringLiteral.map(Str)
		lazy val value: Parsley[JsonExpr] = (string
											 <|> tok.symbol("true") #> Bool(true)
											 <|> tok.symbol("false") #> Bool(false)
											 <|> tok.symbol("null") #> Null
											 <|> attempt(tok.float.map {Num})
											 <|> attempt(tok.integer.map { x => Num(x.toDouble) })
											 <|> array
											 <|> obj)
		tok.whiteSpace *> (obj <|> array) <* eof
	}
}

object FastParseFixtures {


	import fastparse.all._

	final val _parser: Parser[JsonExpr] = parser()

	final def parser(): Parser[JsonExpr] = {


		val space = P(CharsWhileIn(" \r\n").?)
		val digits = P(CharsWhileIn("0123456789"))
		val exponent = P(CharIn("eE") ~ CharIn("+-").? ~ digits)
		val fractional = P("." ~ digits)
		val integral = P("0" | CharIn('1' to '9') ~ digits.?)

		val hexDigit = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
		val strChars = P(CharsWhile(!"\"\\".contains(_: Char)))
		val unicodeEscape: Parser[String] = P("\\u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit).!
			.map(ds => Integer.parseInt(new String(ds.toArray), 16).toString)
		val escape: Parser[String] = P("\\" ~ CharIn("\"/\\bfnrt")).!.map {
			// XXX doesn't work with @switch :(
			case "\\\"" => "\""
			case "\\\\" => "\\"
			case "\\/"  => "/"
			case "\\b"  => "\b"
			case "\\f"  => "\f"
			case "\\n"  => "\n"
			case "\\r"  => "\r"
			case "\\t"  => "\t"
			case x      => x
		}

		val string: Parser[Str] = P(space ~ "\"" ~/ (strChars.! | escape | unicodeEscape).rep ~ "\"")
			.map { x => Str(x.mkString) }
		val number: Parser[Num] = P(CharIn("+-").? ~
									integral ~
									fractional.? ~
									exponent.?).!.map(n => Num(n.toDouble))

		lazy val jsonValue: Parser[JsonExpr] = P(space ~ (string
														  | P("true").map { _ => Bool(true) }
														  | P("false").map { _ => Bool(false) }
														  | P("null").map { _ => Null }
														  | number
														  | obj
														  | array) ~ space)

		lazy val array: Parser[Arr] = P("["
										~/ jsonValue.rep(sep = ",".~/) ~ space ~
										"]").map(xs => Arr(xs.toList))
		lazy val obj: Parser[Obj] = P("{"
									  ~/ P(string ~ space ~/ ":" ~/ jsonValue)
										  .rep(sep = ",".~/) ~ space ~
									  "}").map(xs => Obj(xs.toList))

		P(Start ~ space ~ (obj | array) ~ space ~ End)
	}

}

object AttoFixtures {

	import atto.parser._
	import atto.parser.character._
	import atto.parser.combinator._
	import atto.parser.text._
	import atto.syntax.parser._


	type Parser[A] = atto.Parser[A]

	final val _parser: Parser[JsonExpr] = parser()

	final def parser(): Parser[JsonExpr] = {


		// because atto's Parser[A] is invariant on A for some reason
		@inline def str[A >: Str]: Parser[A] = token(stringLiteral).map[A](Str)

		val num: Parser[JsonExpr] = token(attempt(numeric.double).map {Num}
										  | numeric.int.map { x => Num(x.toDouble) })

		lazy val value: Parser[JsonExpr] = token(choice[JsonExpr](
			str,
			token(string("true")) >| Bool(true),
			token(string("false")) >| Bool(false),
			token(string("null")) >| Null,
			num,
			array,
			obj)).named("value") // XXX name this or else [] or {} causes StackOverflow

		lazy val obj: Parser[JsonExpr] = braces(
			sepBy(pairBy(str[Str], token(char(':')), value), token(char(','))).map[JsonExpr](Obj))
		lazy val array: Parser[JsonExpr] = squareBrackets(
			sepBy(value, token(char(','))).map[JsonExpr](Arr))


		skipWhitespace ~> token(orElse(obj, array)) <~ endOfInput
	}

}


object Parboiled2Fixtures {

	import org.parboiled2._

	class PB2Parser(val input: ParserInput) extends Parser with StringBuilding {

		import CharPredicate.{Digit, Digit19, HexDigit}

		def full: Rule1[JsonExpr] = rule {WhiteSpace ~ (JsonObject | JsonArray) ~ EOI}

		/*_*/
		def JsonObject: Rule1[Obj] = rule {
			ws('{') ~ zeroOrMore(Pair).separatedBy(ws(',')) ~ ws('}') ~> { xs: Seq[(Str, JsonExpr)] => Obj(xs.toList) }
		}
		/*_*/
		def Pair = rule {JsonString ~ ws(':') ~ Value ~> ((_, _))}

		def Value: Rule1[JsonExpr] = rule {
			// as an optimization of the equivalent rule:
			//   JsonString | JsonNumber | JsonObject | JsonArray | JsonTrue | JsonFalse | JsonNull
			// we make use of the fact that one-char lookahead is enough to discriminate the cases
			run {
				(cursorChar: @switch) match {
					case '"'                                                             => JsonString
					case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' => JsonNumber
					case '{'                                                             => JsonObject
					case '['                                                             => JsonArray
					case 't'                                                             => JsonTrue
					case 'f'                                                             => JsonFalse
					case 'n'                                                             => JsonNull
					case _                                                               => MISMATCH
				}
			}
		}


		def JsonString: Rule1[Str] = rule {'"' ~ clearSB() ~ Characters ~ ws('"') ~ push(sb.toString) ~> Str}
		def JsonNumber: Rule1[Num] = rule {capture(Integer ~ optional(Frac) ~ optional(Exp)) ~> { n: String => Num(n.toDouble) } ~ WhiteSpace}
		/*_*/
		def JsonArray: Rule1[Arr] = rule {
			ws('[') ~ zeroOrMore(Value).separatedBy(ws(',')) ~ ws(']') ~> { xs: Seq[JsonExpr] => Arr(xs.toList) }
		}
		/*_*/

		def Characters: Rule0 = rule {zeroOrMore(NormalChar | '\\' ~ EscapedChar)}
		def NormalChar: Rule0 = rule {!QuoteBackslash ~ ANY ~ appendSB()}


		def EscapedChar = rule(
			QuoteSlashBackSlash ~ appendSB()
			| 'b' ~ appendSB('\b')
			| 'f' ~ appendSB('\f')
			| 'n' ~ appendSB('\n')
			| 'r' ~ appendSB('\r')
			| 't' ~ appendSB('\t')
			| Unicode ~> { code => sb.append(code.asInstanceOf[Char]); () }
		)

		def Unicode = rule {'u' ~ capture(HexDigit ~ HexDigit ~ HexDigit ~ HexDigit) ~> (java.lang.Integer.parseInt(_, 16))}
		def Integer: Rule0 = rule {optional('-') ~ (Digit19 ~ Digits | Digit)}
		def Digits: Rule0 = rule {oneOrMore(Digit)}
		def Frac: Rule0 = rule {"." ~ Digits}
		def Exp: Rule0 = rule {ignoreCase('e') ~ optional(anyOf("+-")) ~ Digits}

		def JsonTrue: Rule1[Bool] = rule {"true" ~ WhiteSpace ~ push(Bool(true))}
		def JsonFalse: Rule1[Bool] = rule {"false" ~ WhiteSpace ~ push(Bool(false))}
		def JsonNull: Rule1[Null.type] = rule {"null" ~ WhiteSpace ~ push(Null)}

		def WhiteSpace: Rule0 = rule {zeroOrMore(WhiteSpaceChar)}

		def ws(c: Char): Rule0 = rule {c ~ WhiteSpace}

		val WhiteSpaceChar      = CharPredicate(" \n\r\t\f")
		val QuoteBackslash      = CharPredicate("\"\\")
		val QuoteSlashBackSlash = QuoteBackslash ++ "/"


	}

	import org.parboiled2.Parser.DeliveryScheme.Either

	// XXX cold/hot doesn't make a lot of sense here
	final def _parser: String => Either[String, JsonExpr] = parser()
	final def parser(): String => Either[String, JsonExpr] = { s: String =>
		val parser = new PB2Parser(s)
		parser.full.run().left.map {parser.formatError(_)}
	}


}


object ScalaParserCombinatorFixtures {

	import scala.util.parsing.combinator._
	import scala.util.parsing.combinator.syntactical.StdTokenParsers

	final class SPCParser extends StdTokenParsers with ImplicitConversions {
		// for kicks
		type Tokens = Lexer
		val lexical = new Tokens

		// Configure lexical parsing
		lexical.reserved ++= List("true", "false", "null")
		lexical.delimiters ++= List("{", "}", "[", "]", ":", ",")

		private lazy val jsonObj  : Parser[Obj]             = "{" ~> repsep(objEntry, ",") <~ "}" ^^ Obj
		private lazy val jsonArray: Parser[Arr]             = "[" ~> repsep(value, ",") <~ "]" ^^ Arr
		private lazy val objEntry : Parser[(Str, JsonExpr)] = str ~ (":" ~> value) ^^ { case x ~ y => (x, y) }
		private lazy val value    : Parser[JsonExpr]        = (jsonObj
															   | jsonArray
															   | number
															   | "true" ^^^ Bool(true)
															   | "false" ^^^ Bool(false)
															   | "null" ^^^ Null
															   | str)

		private val str   : Parser[Str] = accept("string", { case lexical.StringLit(n) => Str(n) })
		private val number: Parser[Num] = accept("number", { case lexical.NumericLit(n) => Num(n.toDouble) })

		val root: Parser[JsonExpr] = jsonObj | jsonArray

	}

	final val _parser: String => SPCParser#ParseResult[JsonExpr] = parser()
	final def parser(): String => SPCParser#ParseResult[JsonExpr] = {
		val p = new SPCParser()
		val x = { s: String => p.phrase(p.root)(new p.lexical.Scanner(s)) }
		x
	}

}


// TODO resync with stable to see if it works at all
object ParsebackFixtures {

	import parseback.{Whitespace, _}


	implicit val W: Whitespace = Whitespace(literal("\n+ "))

	type Parser[A] = parseback.Parser[A]

	final val _parser: Parser[List[BrainFuckOp]] = parser()
	final def parser(): Parser[List[BrainFuckOp]] = {
		lazy val ops: Parser[List[BrainFuckOp]] =
			("<" ^^^ LeftPointer
			 | ">" ^^^ RightPointer
			 | "+" ^^^ Increment
			 | "-" ^^^ Decrement
			 | "." ^^^ Output
			 | "," ^^^ Input
			 | (("[" ~> (ops | Parser.Epsilon(Nil)) <~ "]") ^^ { (_, xs) => Loop(xs) })) *

		//		lazy val loop: Parser[List[BrainFuckOp]] =
		//			("[" ~> (expr | Parser.Epsilon(Nil)) <~ "]") ^^ { (_, xs) => Loop(xs) :: Nil }
		//
		//		lazy val expr: Parser[List[BrainFuckOp]] =
		//			((loop | (ops +)) *).map {_.flatten.toList} // empty should fail
		//		expr
		ops
	}
}
