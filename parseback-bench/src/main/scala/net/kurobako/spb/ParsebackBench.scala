package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck._
import net.kurobako.spb.fixture.Json._
import net.kurobako.spb.fixture.Simple.SimpleBenchSpec

import scala.util.Either

object ParsebackBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

	@inline final def collect[A](parser: parseback.Parser[A], input: String): Result[A] = {
		import cats._
		import parseback.util.Catenable
		import parseback.{LineStream, ParseError}
		val foo = LineStream[Eval](input)
		val value: Either[List[ParseError], Catenable[A]] = parser(foo).value
		value.fold({ es => Left(es.toString) }, { xs => println(xs); Right(xs.toList.head) })
	}


	class SimpleBench extends SimpleBenchSpec {
		// TODO 
	}

	class JsonBench extends JsonBenchSpec {
		// TODO 
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		// FIXME resync and check
//		@Benchmark def warmParseback(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck._parser, ctx.input))
//		@Benchmark def coldParseback(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck.parser(), ctx.input))
	}

	object Simple {}

	object BrainFuck {

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

			// TODO resync with stable to see if it works at all

			//		lazy val loop: Parser[List[BrainFuckOp]] =
			//			("[" ~> (expr | Parser.Epsilon(Nil)) <~ "]") ^^ { (_, xs) => Loop(xs) :: Nil }
			//
			//		lazy val expr: Parser[List[BrainFuckOp]] =
			//			((loop | (ops +)) *).map {_.flatten.toList} // empty should fail
			//		expr
			ops
		}
	}

	object Json {}

}