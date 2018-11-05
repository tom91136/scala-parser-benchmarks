package net.kurobako.spb.fixture

import java.util.concurrent.TimeUnit

import net.kurobako.spb.BenchSpec
import org.openjdk.jmh.annotations._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
object Simple {

	@State(Scope.Thread)
	class SimpleContext {
		//		@Param(Array("10", "100", "1000", "10000", "100000", "1000000"))
		@Param(Array("17"))
		var N    : Int    = 0
		var token: String = _
		@Setup def setup(): Unit = {token = List.tabulate(N) { _ => "1" }.mkString("+")}
	}

	object SimpleSpec {
		final val TestChar  : Char   = '1'
		final val RepN      : Int    = 17
		final val TestString: String = List.fill(RepN) {TestChar}.mkString("+")
		final val Expected  : Int    = TestChar * RepN
	}

	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Array(Mode.AverageTime))
	abstract class SimpleBenchSpec extends BenchSpec 

}
