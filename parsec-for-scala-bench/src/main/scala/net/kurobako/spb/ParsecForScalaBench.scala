package net.kurobako.spb

import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

object ParsecForScalaBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench])

	@inline final def collectParsec[A](parser: parsec.Parser[A],
									   input: String): Result[A] = {
		import parsec._
		runParser[parsec.Stream[String, Char], Unit, A](parser, (), "", input)
			.left.map {_.toString()}
	}

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def warmParsecForScalaChainL(ctx: SimpleContext): Result[Int] = !!(collectParsec(Simple._chainL, ctx.token))
		@Benchmark def coldParsecForScalaChainL(ctx: SimpleContext): Result[Int] = !!(collectParsec(Simple.chainL(), ctx.token))
	}


	object Simple {

		import parsec.Char._
		import parsec.Combinator.chainl1
		import parsec._

		final val _chainL = chainL()
		@inline final def chainL(): Parser[Int] = {
			val x: Parser[Int] = string("1") <#> {_ (0).toInt}
			val y: Parser[Int => Int => Int] = char('+') <#> { _ => (x: Int) => (y: Int) => x + y }
			chainl1(x, y)
		}
	}


}