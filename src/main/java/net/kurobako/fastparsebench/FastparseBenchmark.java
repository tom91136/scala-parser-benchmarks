package net.kurobako.fastparsebench;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import scala.util.Either;

import static java.util.stream.Collectors.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class FastparseBenchmark {

	@State(Scope.Benchmark)
	public static class N {
		//		@Param({"10", "100", "1000", "10000", "100000", "1000000"})
		@Param({"17",})
		public int N;
		private String TOKEN;
		@Setup
		public void setup() {
			TOKEN = Stream.generate(() -> "1")
					.limit(N)
					.collect(joining("+"));
		}
	}
	@Benchmark public Either<String, Object> parseParsecForScalaChainL1(N n) {
		return Fixtures.parseParsecForScalaChainL1(n.TOKEN);
	}

	@Benchmark public Either<String, Object> parseBindFoldCurried(N n) {
		return Fixtures.parseBindFoldCurried(n.TOKEN);
	}

	@Benchmark public Either<String, Object> parseBindFoldTuple2(N n) {
		return Fixtures.parseBindFoldTuple2(n.TOKEN);
	}

	@Benchmark public Either<String, Object> parseBindRecursiveTuple2(N n) {
		return Fixtures.parseBindRecursiveTuple2(n.TOKEN);
	}

	@Benchmark public Either<String, Object> parseBindRecursiveCurried(N n) {
		return Fixtures.parseBindRecursiveCurried(n.TOKEN);
	}

	@Benchmark public Either<String, Object> parseRightRecursive(N n) {
		return Fixtures.parseRightRecursive(n.TOKEN);
	}

	@Benchmark public Either<String, Object> parseHandRollRD(N n) {
		return Fixtures.parseHandRollRD(n.TOKEN);
	}


	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(FastparseBenchmark.class.getSimpleName())
				.warmupIterations(10)
				.measurementIterations(10)
				.forks(1)
				.build();

		new Runner(opt).run();
	}
}

