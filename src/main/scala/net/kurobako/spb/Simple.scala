package net.kurobako.spb

import java.util.concurrent.TimeUnit

import net.kurobako.spb.Simple._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.{Runner, RunnerException}

import scala.annotation.tailrec
import scala.util.Either

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
object Simple {

	@State(Scope.Thread)
	class Context {
		//		@Param(Array("10", "100", "1000", "10000", "100000", "1000000"))
		@Param(Array("17"))
		var N    : Int    = 0
		var token: String = _
		@Setup def setup(): Unit = {token = List.tabulate(N) { _ => "1" }.mkString("+")}
	}
	@throws[RunnerException]
	def main(args: Array[String]): Unit = {
		import cats.implicits._

		import scala.collection.JavaConverters._

		val iter = 15
		val result = new Runner(new OptionsBuilder()
			.include(classOf[Simple].getSimpleName)
			.warmupIterations(iter)
			.measurementIterations(iter)
			.forks(2)
			.shouldFailOnError(true)
			.build).run.asScala.toSeq


		val table = result
			.map { v =>
				val result = v.getPrimaryResult
				(result.getLabel, result.getScore, result.getScoreError)
			}
			.sortBy { case (_, score, _) => score }
			.zipWithIndex
			.map { case ((method, score, error), i) =>
				List(i.toString, method, score.toString, error.toString)
			}

		println("Result:\n" +
				(Seq("id", "method", "score", "error") +: table)
					.map {_.mkString(" ")}.mkString("\n"))
	}

	type Result = Either[String, Int]

}


@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class Simple {

	// baseline tailrec(not recursive decent)
	@Benchmark def tailRecursiveBaseline(ctx: Context): Result = {
		@tailrec def parseSimple(input: String, index: Int, sum: Int): Int = {
			if (index >= ctx.token.length) return sum
			input.charAt(index) match {
				case c@'1' => parseSimple(input, index + 1, sum + c)
				case '+'   => parseSimple(input, index + 1, sum)
			}
		}

		Right(parseSimple(ctx.token, 0, 0))
	}

	// scala-parser-combinator
	@Benchmark def warmScalaParserCombinatorChainL(ctx: Context): Result = ScalaParserCombinatorFixtures.collect(ScalaParserCombinatorFixtures._chainL, ctx.token)
	@Benchmark def coldScalaParserCombinatorChainL(ctx: Context): Result = ScalaParserCombinatorFixtures.collect(ScalaParserCombinatorFixtures.chainL(), ctx.token)

	// atto
	@Benchmark def warmAttoChainL(ctx: Context): Result = AttoFixtures.collect(AttoFixtures._chainL, ctx.token)
	@Benchmark def coldAttoChainL(ctx: Context): Result = AttoFixtures.collect(AttoFixtures.chainL(), ctx.token)

	// parsec-for-scala
	@Benchmark def warmParsecForScalaChainL(ctx: Context): Result = ParserForScalaFixtures.collect(ParserForScalaFixtures._chainL, ctx.token)
	@Benchmark def coldParsecForScalaChainL(ctx: Context): Result = ParserForScalaFixtures.collect(ParserForScalaFixtures.chainL(), ctx.token)

	// parsely
	@Benchmark def warmParsleyChainL(ctx: Context): Result = ParsleyFixtures.collect(ParsleyFixtures._chainL, ctx.token)
	@Benchmark def coldParsleyChainL(ctx: Context): Result = ParsleyFixtures.collect(ParsleyFixtures.chainL(), ctx.token)

	// fastparse
	@Benchmark def warmFastParseBindFoldCurried(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures._foldC, ctx.token)
	@Benchmark def coldFastParseBindFoldCurried(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures.foldC(), ctx.token)
	@Benchmark def warmFastParseBindFoldTuple2(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures._foldT2, ctx.token)
	@Benchmark def coldFastParseBindFoldTuple2(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures.foldT2(), ctx.token)
	@Benchmark def warmFastParseBindRecursiveCurried(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures._recursiveC, ctx.token)
	@Benchmark def coldFastParseBindRecursiveCurried(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures.recursiveC(), ctx.token)
	@Benchmark def warmFastParseBindRecursiveTuple2(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures._recursiveT2, ctx.token)
	@Benchmark def coldFastParseBindRecursiveTuple2(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures.recursiveT2(), ctx.token)
	@Benchmark def warmFastParseRightRecursive(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures._rightRecursive, ctx.token)
	@Benchmark def coldFastParseRightRecursive(ctx: Context): Result = FastParseFixtures.collect(FastParseFixtures.rightRecursive(), ctx.token)

}

