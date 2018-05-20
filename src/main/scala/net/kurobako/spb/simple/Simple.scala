package net.kurobako.spb.simple

import java.util.concurrent.TimeUnit

import net.kurobako.spb.Collectors._
import net.kurobako.spb.simple.Simple.Context
import org.openjdk.jmh.annotations._

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

}


@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class Simple {


	// baselines
	@Benchmark def baselineTailRecursiveTry(ctx: Context): Result[Int] = !!(Baselines.tailRecursiveTry(ctx.token))
	@Benchmark def baselineRecursiveDecent(ctx: Context): Result[Int] = !!(Baselines.recursiveDecent(ctx.token))

	// scala-parser-combinator
	@Benchmark def warmScalaParserCombinatorChainL(ctx: Context): Result[Int] = !!(collectSPC(ScalaParserCombinatorFixtures._chainL, ctx.token))
	@Benchmark def coldScalaParserCombinatorChainL(ctx: Context): Result[Int] = !!(collectSPC(ScalaParserCombinatorFixtures.chainL(), ctx.token))

	// atto
	@Benchmark def warmAttoChainL(ctx: Context): Result[Int] = !!(collectAtto(AttoFixtures._chainL, ctx.token))
	@Benchmark def coldAttoChainL(ctx: Context): Result[Int] = !!(collectAtto(AttoFixtures.chainL(), ctx.token))

	// parsec-for-scala
	@Benchmark def warmParsecForScalaChainL(ctx: Context): Result[Int] = !!(collectParsec(ParsecForScalaFixtures._chainL, ctx.token))
	@Benchmark def coldParsecForScalaChainL(ctx: Context): Result[Int] = !!(collectParsec(ParsecForScalaFixtures.chainL(), ctx.token))

	// meerkat
	@Benchmark def warmMeerkatBnf(ctx: Context): Result[Int] = !!(collectMeerkat(MeerkatFixtures._bnf, ctx.token))
	@Benchmark def coldMeerkatBnf(ctx: Context): Result[Int] = !!(collectMeerkat(MeerkatFixtures.bnf(), ctx.token))

	// parsely
	@Benchmark def warmParsleyChainL(ctx: Context): Result[Int] = !!(collectParsley(ParsleyFixtures._chainL, ctx.token))
	@Benchmark def coldParsleyChainL(ctx: Context): Result[Int] = !!(collectParsley(ParsleyFixtures.chainL(), ctx.token))

	// fastparse
	@Benchmark def warmFastParseBindFoldCurried(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures._foldC, ctx.token))
	@Benchmark def coldFastParseBindFoldCurried(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures.foldC(), ctx.token))
	@Benchmark def warmFastParseBindFoldTuple2(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures._foldT2, ctx.token))
	@Benchmark def coldFastParseBindFoldTuple2(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures.foldT2(), ctx.token))
	@Benchmark def warmFastParseBindRecursiveCurried(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures._recursiveC, ctx.token))
	@Benchmark def coldFastParseBindRecursiveCurried(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures.recursiveC(), ctx.token))
	@Benchmark def warmFastParseBindRecursiveTuple2(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures._recursiveT2, ctx.token))
	@Benchmark def coldFastParseBindRecursiveTuple2(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures.recursiveT2(), ctx.token))
	@Benchmark def warmFastParseRightRecursive(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures._rightRecursive, ctx.token))
	@Benchmark def coldFastParseRightRecursive(ctx: Context): Result[Int] = !!(collectFastParse(FastParseFixtures.rightRecursive(), ctx.token))

}

