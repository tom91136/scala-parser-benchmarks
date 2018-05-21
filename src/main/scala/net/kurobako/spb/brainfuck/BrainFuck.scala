package net.kurobako.spb.brainfuck

import java.util.concurrent.TimeUnit

import net.kurobako.spb.Collectors.Result
import net.kurobako.spb.Collectors._
import net.kurobako.spb.Collectors.!!
import net.kurobako.spb.brainfuck.BrainFuck.{BrainFuckOp, Context}
import org.openjdk.jmh.annotations._

import scala.io.Source

object BrainFuck {

	trait BrainFuckOp
	case object RightPointer extends BrainFuckOp
	case object LeftPointer extends BrainFuckOp
	case object Increment extends BrainFuckOp
	case object Decrement extends BrainFuckOp
	case object Output extends BrainFuckOp
	case object Input extends BrainFuckOp
	case class Loop(p: List[BrainFuckOp]) extends BrainFuckOp

	@State(Scope.Thread)
	class Context {
		@Param(Array(
			"brainfuck/helloworld.bf",
			"brainfuck/helloworld_comments.bf",
			"brainfuck/mandelbrot.bf"
		))
		var file : String = _
		var input: String = _
		@Setup def setup(): Unit = {input = Source.fromResource(file).mkString}
	}

}
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class BrainFuck {

	type BFOps = List[BrainFuckOp]
	// baselines
	@Benchmark def baselineRecursiveDecent(ctx: Context): Result[BFOps] = !!(Baselines.parse(ctx.input))

	// scala-parser-combinator
	@Benchmark def warmScalaParserCombinator(ctx: Context): Result[BFOps] = !!(collectSPC(ScalaParserCombinatorFixtures._parser, ctx.input))
	@Benchmark def coldScalaParserCombinator(ctx: Context): Result[BFOps] = !!(collectSPC(ScalaParserCombinatorFixtures.parser(), ctx.input))

	// atto
	@Benchmark def warmAtto(ctx: Context): Result[BFOps] = !!(collectAtto(AttoFixtures._parser, ctx.input))
	@Benchmark def coldAtto(ctx: Context): Result[BFOps] = !!(collectAtto(AttoFixtures.parser(), ctx.input))


	// parsely
	@Benchmark def warmParsley(ctx: Context): Result[BFOps] = !!(collectParsley(ParsleyFixtures._parser, ctx.input))
	@Benchmark def coldParsley(ctx: Context): Result[BFOps] = !!(collectParsley(ParsleyFixtures.parser(), ctx.input))

	// fastparse
	@Benchmark def warmFastParse(ctx: Context): Result[BFOps] = !!(collectFastParse(FastParseFixtures._parser, ctx.input))
	@Benchmark def coldFastParse(ctx: Context): Result[BFOps] = !!(collectFastParse(FastParseFixtures.parser(), ctx.input))

	// fastparse
	@Benchmark def warmParboiled2(ctx: Context): Result[BFOps] = !!(collectParboiled2(Parboiled2Fixtures._parser, ctx.input))
	@Benchmark def coldParboiled2(ctx: Context): Result[BFOps] = !!(collectParboiled2(Parboiled2Fixtures.parser(), ctx.input))

}

