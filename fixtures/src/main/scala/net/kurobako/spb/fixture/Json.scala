package net.kurobako.spb.fixture

import java.util.concurrent.TimeUnit

import net.kurobako.spb.BenchSpec
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
	class JsonContext {
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

	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Array(Mode.AverageTime))
	abstract class JsonBenchSpec extends BenchSpec  

}
