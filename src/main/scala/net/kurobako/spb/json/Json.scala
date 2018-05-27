package net.kurobako.spb.json

import java.util.concurrent.TimeUnit

import net.kurobako.spb.Collectors.Result
import net.kurobako.spb.Collectors._
import net.kurobako.spb.Collectors.!!
import net.kurobako.spb.json.Json.{Context, JsonExpr}
import org.openjdk.jmh.annotations._

import scala.io.Source

object Json {

	sealed trait JsonExpr
	case class Str(value: String) extends JsonExpr
	case class Obj(value: List[(Str, JsonExpr)]) extends JsonExpr
	case class Arr(value: List[JsonExpr]) extends JsonExpr
	case class Num(value: Double) extends JsonExpr
	case class Bool(value: Boolean) extends JsonExpr
	case object Null extends JsonExpr

	@State(Scope.Thread)
	class Context {
		@Param(Array(
			"json/bench_long.json",
			"json/medium.json",
			"json/mixed.json",
			"json/arr0.json",
			"json/obj0.json",
		))
		var file : String = _
		var input: String = _
		@Setup def setup(): Unit = {input = Source.fromResource(file).mkString}
	}

}
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class Json {

	@Benchmark def warmScalaParserCombinator(ctx: Context): Result[JsonExpr] = !!(collectSPC(ScalaParserCombinatorFixtures._parser, ctx.input))
	@Benchmark def coldScalaParserCombinator(ctx: Context): Result[JsonExpr] = !!(collectSPC(ScalaParserCombinatorFixtures.parser(), ctx.input))

	@Benchmark def warmAtto(ctx: Context): Result[JsonExpr] = !!(collectAtto(AttoFixtures._parser, ctx.input))
	@Benchmark def coldAtto(ctx: Context): Result[JsonExpr] = !!(collectAtto(AttoFixtures.parser(), ctx.input))

	@Benchmark def warmParsley(ctx: Context): Result[JsonExpr] = !!(collectParsley(ParsleyFixtures._parser, ctx.input))
	@Benchmark def coldParsley(ctx: Context): Result[JsonExpr] = !!(collectParsley(ParsleyFixtures.parser(), ctx.input))

	@Benchmark def warmFastParse(ctx: Context): Result[JsonExpr] = !!(collectFastParse(FastParseFixtures._parser, ctx.input))
	@Benchmark def coldFastParse(ctx: Context): Result[JsonExpr] = !!(collectFastParse(FastParseFixtures.parser(), ctx.input))

	@Benchmark def warmParboiled2(ctx: Context): Result[JsonExpr] = !!(collectParboiled2(Parboiled2Fixtures._parser, ctx.input))
	@Benchmark def coldParboiled2(ctx: Context): Result[JsonExpr] = !!(collectParboiled2(Parboiled2Fixtures.parser(), ctx.input))

}

