package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Json.{JsonExpr, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

object FastParse1Bench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

	@inline final def collect[A](parser: fastparse.all.Parser[A], input: String): Result[A] = {
		import fastparse.core.Parsed.{Failure, Success}
		parser.parse(input) match {
			case Success(value, _)        => Right(value)
			case f: Failure[Char, String] => Left(s"${f.msg} -> \n\t${f.extra.traced.fullStack.mkString("\n\t")}")
		}
	}

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def warmFastParse1BindFoldCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple._foldC, ctx.token))
		@Benchmark def coldFastParse1BindFoldCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple.foldC(), ctx.token))
		@Benchmark def warmFastParse1BindFoldTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple._foldT2, ctx.token))
		@Benchmark def coldFastParse1BindFoldTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple.foldT2(), ctx.token))
		@Benchmark def warmFastParse1BindRecursiveCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple._recursiveC, ctx.token))
		@Benchmark def coldFastParse1BindRecursiveCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple.recursiveC(), ctx.token))
		@Benchmark def warmFastParse1BindRecursiveTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple._recursiveT2, ctx.token))
		@Benchmark def coldFastParse1BindRecursiveTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple.recursiveT2(), ctx.token))
		@Benchmark def warmFastParse1RightRecursive(ctx: SimpleContext): Result[Int] = !!(collect(Simple._rightRecursive, ctx.token))
		@Benchmark def coldFastParse1RightRecursive(ctx: SimpleContext): Result[Int] = !!(collect(Simple.rightRecursive(), ctx.token))
	}

	class JsonBench extends JsonBenchSpec {
		@Benchmark def warmFastParse1(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json._parser, ctx.input))
		@Benchmark def coldFastParse1(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json.parser(), ctx.input))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def warmFastParse1(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck._parser, ctx.input))
		@Benchmark def coldFastParse1(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck.parser(), ctx.input))
	}

	object Simple {

		import fastparse.all
		import fastparse.all._

		final val _foldC = foldC()
		@inline final def foldC(): Parser[Int] = {
			def chainlf[A](p: Parser[A], op: Parser[A => A => A]): Parser[A] = {
				for (x <- p;
					 fs <- (for (f <- op;
								 y <- p)
						 yield (x: A) => f(x)(y)).rep)
					yield fs.foldLeft(x)((y, f) => f(y))
			}

			val x = P("1").!.map(_ (0).toInt)
			val y = P("+").!.map(_ => (x: Int) => (y: Int) => x + y)
			chainlf(x, y)
		}
		final val _foldT2 = foldT2()
		@inline final def foldT2(): Parser[Int] = {
			def chainlf[A](p: Parser[A], op: Parser[(A, A) => A]): Parser[A] = {
				for (x <- p;
					 fs <- (for (f <- op;
								 y <- p)
						 yield (x: A) => f(x, y)).rep)
					yield fs.foldLeft(x)((y, f) => f(y))
			}

			val x = P("1").!.map(_ (0).toInt)
			val y = P("+").!.map(_ => (x: Int, y: Int) => x + y)
			chainlf(x, y)
		}
		final val _recursiveC = recursiveC()
		@inline final def recursiveC(): Parser[Int] = {
			type BinaryOp[A] = (A, A) => A

			def chainl[B](p: all.Parser[B], op: all.Parser[BinaryOp[B]]): all.Parser[B] = {
				// TODO make tail recursive
				def rest(x: B): all.Parser[B] = (for {
					f <- op
					y <- p
					z <- rest(f(x, y))
				} yield z) | PassWith(x)

				p.flatMap {rest}
			}

			val x = P("1").!.map(_ (0).toInt)
			val y: Parser[BinaryOp[Int]] = P("+").map { _ => (x, y) => x + y }
			chainl(x, y)
		}
		final val _recursiveT2 = recursiveT2()
		@inline final def recursiveT2(): Parser[Int] = {
			type BinaryOp[A] = A => A => A

			def chainl[B](p: all.Parser[B], op: all.Parser[BinaryOp[B]]): all.Parser[B] = {
				// TODO make tail recursive
				def rest(x: B): all.Parser[B] = (for {
					f <- op
					y <- p
					z <- rest(f(x)(y)) ~/
				} yield z) | PassWith(x)

				p.flatMap {rest}
			}

			val x = P("1").!.map(_ (0).toInt)
			val y: Parser[BinaryOp[Int]] = P("+").map { _ => x => y => x + y }
			chainl(x, y)
		}
		final val _rightRecursive = rightRecursive()
		@inline final def rightRecursive(): Parser[Int] = {
			val x = P("1").!.map(_ (0).toInt)
			(x ~ ("+" ~/ x).rep).map { case (l, xs) => l + xs.sum }
		}
	}

	object BrainFuck {


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

	object Json {


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

			lazy val jsonValue: Parser[JsonExpr] = P(space ~ (string | number | array | obj
															  | P("true").map { _ => Bool(true) }
															  | P("false").map { _ => Bool(false) }
															  | P("null").map { _ => Null }) ~ space)

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

}