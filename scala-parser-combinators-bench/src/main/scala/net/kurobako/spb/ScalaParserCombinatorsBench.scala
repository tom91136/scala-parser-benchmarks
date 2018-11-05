package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Json.{JsonExpr, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

import scala.annotation.switch
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.json.Lexer

object ScalaParserCombinatorsBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

	// SPC uses type members so we have this odd type signature
	@inline final def collect[
	P <: scala.util.parsing.combinator.Parsers,
	A](parser_ : String => P#ParseResult[A], input: String): Result[A] = {
		parser_(input) match {
			case _: P#Success[_] => Right(parser_(input).get)
			case f: P#NoSuccess  => Left(f.msg)
			case e               => Left(s"Unexpected parse error $e")
		}
	}

	class SimpleBench extends SimpleBenchSpec {
		// scala-parser-combinator
		@Benchmark def warmScalaParserCombinatorChainL(ctx: SimpleContext): Result[Int] = !!(collect(Simple._chainL, ctx.token))
		@Benchmark def coldScalaParserCombinatorChainL(ctx: SimpleContext): Result[Int] = !!(collect(Simple.chainL(), ctx.token))
	}

	class JsonBench extends JsonBenchSpec {
		@Benchmark def warmScalaParserCombinator(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json._parser, ctx.input))
		@Benchmark def coldScalaParserCombinator(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json.parser(), ctx.input))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		// scala-parser-combinator
		@Benchmark def warmScalaParserCombinator(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck._parser, ctx.input))
		@Benchmark def coldScalaParserCombinator(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck.parser(), ctx.input))
	}

	object Simple {

		import scala.util.parsing.combinator._

		final class SPCParser extends RegexParsers {
			// XXX we need those implicit conversions but NOT the regex stuff
			@inline def x: Parser[Int] = literal("1") ^^ { x: String => x.head.toInt }
			@inline def y: Parser[(Int, Int) => Int] = literal("+") ^^ { _ => (x: Int, y: Int) => x + y }
			@inline def chainl[B](p: Parser[B], op: Parser[(B, B) => B]): Parser[B] = {
				// TailRecM ???
				def rest(x: B): Parser[B] = (for {
					f <- op
					y <- p
					z <- rest(f(x, y))
				} yield z) | success(x)

				p.flatMap {rest}
			}
		}

		final val _chainL = chainL()
		@inline final def chainL(): String => SPCParser#ParseResult[Int] = {
			val p = new SPCParser
			val x = { s: String => p.chainl(p.x, p.y)(new CharSequenceReader(s)) }
			x
		}
	}

	object BrainFuck {

		import scala.util.parsing.combinator._
		import scala.util.parsing.input.CharSequenceReader

		final class SPCParser extends RegexParsers {

			override def skipWhitespace: Boolean = false

			private val ws: Parser[List[Char]] = rep(acceptIf(c => (c: @switch) match {
				case '>' | '<' | '+' | '-' | '.' | ',' | '[' | ']' => false
				case _                                             => true
			})(_ => ""))

			private val bf: Parser[List[BrainFuckOp]] = rep((accept("operator", {
				case '>' => RightPointer
				case '<' => LeftPointer
				case '+' => Increment
				case '-' => Decrement
				case '.' => Output
				case ',' => Input
			}) | (accept('[') ~> ws ~> (bf ^^ Loop) <~ accept(']'))) <~ ws)

			val root: Parser[List[BrainFuckOp]] = ws ~> bf

		}

		final val _parser: String => SPCParser#ParseResult[List[BrainFuckOp]] = parser()
		final def parser(): String => SPCParser#ParseResult[List[BrainFuckOp]] = {
			val p = new SPCParser()
			val x = { s: String => p.phrase(p.root)(new CharSequenceReader(s)) }
			x
		}
	}

	object Json {

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

}