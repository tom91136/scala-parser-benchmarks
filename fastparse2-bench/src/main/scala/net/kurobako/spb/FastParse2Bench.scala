package net.kurobako.spb

import fastparse.P
import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Json.{JsonExpr, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

object FastParse2Bench extends BenchProvider {


	type Parser[A] = P[_] => P[A]

	def mkParser[A](f: P[_] => P[A]): Parser[A] = f


	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

	@inline final def collect[A](parser: Parser[A], input: String): Result[A] = {
		fastparse.parse(input, parser) match {
			case fastparse.Parsed.Success(value, _)            => Right(value)
			case fastparse.Parsed.Failure(label, index, extra) =>
				val traced = extra.traced
				Left(s"${traced.msg} -> \n\t${traced.stack.mkString("\n\t")}")
		}
	}

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def warmFastParse2BindFoldCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple._foldC, ctx.token))
		@Benchmark def coldFastParse2BindFoldCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple.foldC, ctx.token))
		@Benchmark def warmFastParse2BindFoldTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple._foldT2, ctx.token))
		@Benchmark def coldFastParse2BindFoldTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple.foldT2, ctx.token))
		@Benchmark def warmFastParse2BindRecursiveCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple._recursiveC, ctx.token))
		@Benchmark def coldFastParse2BindRecursiveCurried(ctx: SimpleContext): Result[Int] = !!(collect(Simple.recursiveC, ctx.token))
		@Benchmark def warmFastParse2BindRecursiveTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple._recursiveT2, ctx.token))
		@Benchmark def coldFastParse2BindRecursiveTuple2(ctx: SimpleContext): Result[Int] = !!(collect(Simple.recursiveT2, ctx.token))
		@Benchmark def warmFastParse2RightRecursive(ctx: SimpleContext): Result[Int] = !!(collect(Simple._rightRecursive, ctx.token))
		@Benchmark def coldFastParse2RightRecursive(ctx: SimpleContext): Result[Int] = !!(collect(Simple.rightRecursive, ctx.token))
	}

	class JsonBench extends JsonBenchSpec {
		@Benchmark def warmFastParse2(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json._parser, ctx.input))
		@Benchmark def coldFastParse2(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json.parser, ctx.input))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def warmFastParse2(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck._parser, ctx.input))
		@Benchmark def coldFastParse2(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck.parser, ctx.input))
	}

	object Simple {

		import fastparse._
		import NoWhitespace._

		final val _foldC = foldC

		@inline final def foldC: Parser[Int] = {

			def chainlf[A](p: Parser[A], op: Parser[A => A => A])(implicit ev: P[_]): P[A] = {
				for (x <- p(ev);
					 fs <- (for (f <- op(ev);
								 y <- p(ev))
						 yield (x: A) => f(x)(y)).rep)
					yield fs.foldLeft(x)((y, f) => f(y))
			}

			def x: Parser[Int] = mkParser { implicit ctx => P("1").!.map(_ (0).toInt) }
			def y: Parser[Int => (Int => Int)] = mkParser { implicit ctx => P("+").!.map(_ => (x: Int) => (y: Int) => x + y) }
			mkParser { implicit ctx => chainlf(x, y) }
		}

		final val _foldT2 = foldT2
		@inline final def foldT2: Parser[Int] = {
			def chainlf[A](p: Parser[A], op: Parser[(A, A) => A])(implicit ev: P[_]): P[A] = {
				for (x <- p(ev);
					 fs <- (for (f <- op(ev);
								 y <- p(ev))
						 yield (x: A) => f(x, y)).rep)
					yield fs.foldLeft(x)((y, f) => f(y))
			}

			def x: Parser[Int] = mkParser { implicit ctx => P("1").!.map(_ (0).toInt) }
			def y: Parser[(Int, Int) => Int] = mkParser { implicit ctx => P("+").!.map(_ => (x: Int, y: Int) => x + y) }
			mkParser { implicit ctx => chainlf(x, y) }
		}

		final val _recursiveC = recursiveC
		@inline final def recursiveC: Parser[Int] = {
			type BinaryOp[A] = (A, A) => A

			def chainl[B, _: P](p: Parser[B], op: Parser[BinaryOp[B]]): P[B] = {
				// TODO make tail recursive
				def rest(x: B)(implicit ev: P[_]): P[B] = // XXX look at this abomination!
					op(ev).flatMap(f => p(ev).flatMap(y => rest(f(x, y))(ev))).|(Pass(x)(ev))(ev)
				p(implicitly[P[_]]).flatMapX(rest(_))
			}

			def x: Parser[Int] = mkParser { implicit ctx => P("1").!.map(_ (0).toInt) }
			def y: Parser[BinaryOp[Int]] = mkParser { implicit ctx => P("+").map { _ => (x, y) => x + y } }
			mkParser { implicit ctx => chainl(x, y) }
		}

		final val _recursiveT2 = recursiveT2
		@inline final def recursiveT2: Parser[Int] = {
			type BinaryOp[A] = A => A => A

			def chainl[B, _: P](p: Parser[B], op: Parser[BinaryOp[B]]): P[B] = {
				// TODO make tail recursive
				def rest(x: B)(implicit ev: P[_]): P[B] = // XXX look at this abomination!
					op(ev).flatMap { f => p(ev).flatMap(y => rest(f(x)(y))(ev)) }.|(Pass(x)(ev))(ev)
				p(implicitly[P[_]]).flatMap(rest(_))
			}

			def x: Parser[Int] = mkParser { implicit ctx => P("1").!.map(_ (0).toInt) }
			def y: Parser[BinaryOp[Int]] = mkParser { implicit ctx => P("+").map { _ => x => y => x + y } }
			mkParser { implicit ctx => chainl(x, y) }
		}
		final val _rightRecursive = rightRecursive
		@inline final def rightRecursive: Parser[Int] = {
			def x[_: P] = P("1").!.map(_ (0).toInt)
			mkParser { implicit ctx => (x ~ ("+" ~/ x).rep).map { case (l, xs) => l + xs.sum } }
		}
	}

	object BrainFuck {


		import fastparse._

		implicit val whitespace: P[_] => P[Unit] = { implicit ctx: ParsingRun[_] =>
			val Reserved = "<>+-.,[]".toSet
			NoTrace(CharsWhile(!Reserved.contains(_)).rep)
		}

		final val _parser: Parser[List[BrainFuckOp]] = parser
		final def parser: Parser[List[BrainFuckOp]] = {
			def ops[_: P]: P[BrainFuckOp] =
				CharIn("<>+-.,").!.map {
					case "<" => LeftPointer
					case ">" => RightPointer
					case "+" => Increment
					case "-" => Decrement
					case "." => Output
					case "," => Input
				}.opaque(s"keywords(<>+-.,)")

			def loop[_: P]: P[List[BrainFuckOp]] =
				P("[".opaque("Opening bracket '['") ~/
				  (expr | Pass(Nil)).opaque("expression") ~ // [] is ok
				  "]".opaque("']' Closing bracket"))
					.map {Loop(_) :: Nil}

			def expr[_: P]: P[List[BrainFuckOp]] = (loop | ops.rep(1)).rep
				.map {_.flatten.toList} // empty should fail
			mkParser { implicit ctx => Start ~ expr ~ End }
		}
	}

	object Json {

		import fastparse._
		import NoWhitespace._

		final val _parser: Parser[JsonExpr] = parser
		final def parser: Parser[JsonExpr] = {

			def space[_: P] = P(CharsWhileIn(" \r\n", 0))
			def digits[_: P] = P(CharsWhileIn("0-9"))
			def exponent[_: P] = P(CharIn("eE") ~ CharIn("+\\-").? ~ digits)
			def fractional[_: P] = P("." ~ digits)
			def integral[_: P] = P("0" | CharIn("1-9") ~ digits.?)


			def hexDigit[_: P] = P(CharIn("0-9a-fA-F"))
			def strChars[_: P] = P(CharsWhile(stringChars))
			def unicodeEscape[_: P]: P[String] = P("\\u" ~ hexDigit ~ hexDigit ~ hexDigit ~ hexDigit).!
				.map(ds => Integer.parseInt(new String(ds.toArray), 16).toString)
			def escape[_: P]: P[String] = P("\\" ~ CharIn("\"/\\\\bfnrt")).!.map {
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

			def stringChars(c: Char) = c != '\"' && c != '\\'
			def string[_: P]: P[Str] =
				P(space ~ "\"" ~/ (strChars.! | escape | unicodeEscape).rep ~ "\"").map(xs => Str(xs.mkString))

			def number[_: P]: P[Num] = P(CharIn("+\\-").? ~ integral ~ fractional.? ~ exponent.?)
				.!.map(x => Num(x.toDouble))

			def jsonValue[_: P]: P[JsonExpr] = P(
				space ~ (string | number | array | obj
						 | P("true").map(_ => Bool(true))
						 | P("false").map(_ => Bool(false))
						 | P("null").map(_ => Null)) ~ space
			)

			def array[_: P]: P[Arr] =
				P("[" ~/ jsonValue.rep(sep = ","./) ~ space ~ "]").map(xs => Arr(xs.toList))

			def obj[_: P]: P[Obj] =
				P("{" ~/ P(string ~ space ~/ ":" ~/ jsonValue).rep(sep = ","./) ~ space ~ "}").map(xs => Obj(xs.toList))

			mkParser { implicit ctx => P(Start ~ space ~ (obj | array) ~ space ~ End) }
		}

	}

}