package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Json.{JsonExpr, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

object AttoBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

	@inline final def collect[A](_parser: atto.Parser[A],
									 input: String): Result[A] = {
		import atto._
		import Atto._
		// XXX apparently, naming the parser `parser` shadows some import...
		_parser.parseOnly(input).either
	}

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def warmAttoChainL(ctx: SimpleContext): Result[Int] = !!(collect(Simple._chainL, ctx.token))
		@Benchmark def coldAttoChainL(ctx: SimpleContext): Result[Int] = !!(collect(Simple.chainL(), ctx.token))
	}

	class JsonBench extends JsonBenchSpec {
		@Benchmark def warmAtto(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json._parser, ctx.input))
		@Benchmark def coldAtto(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json.parser(), ctx.input))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def warmAtto(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck._parser, ctx.input))
		@Benchmark def coldAtto(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck.parser(), ctx.input))
	}

	object Simple {
		import atto._
		import Atto._

		final val _chainL = chainL()
		@inline final def chainL(): Parser[Int] = {

			def chainl[B](p: Parser[B], op: Parser[(B, B) => B]): Parser[B] = {
				def rest(x: B): Parser[B] =
					orElse((for (f <- op; y <- p) yield f(x, y)).flatMap(rest), ok(x))

				p.flatMap(rest)
			}

			val x = string("1").map(_ (0).toInt)
			val y = char('+').map { _ => (x: Int, y: Int) => x + y }
			chainl(x, y)
		}
	}

	object BrainFuck {
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

	object Json {
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

}