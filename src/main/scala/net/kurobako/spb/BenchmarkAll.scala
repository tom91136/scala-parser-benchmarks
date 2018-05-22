package net.kurobako.spb

import net.kurobako.spb.brainfuck.BrainFuck
import net.kurobako.spb.simple.Simple
import org.openjdk.jmh.profile.{GCProfiler, HotspotCompilationProfiler, StackProfiler}
import org.openjdk.jmh.results.format.ResultFormatType
import org.openjdk.jmh.runner.{Runner, RunnerException}
import org.openjdk.jmh.runner.options.OptionsBuilder

object BenchmarkAll {

	final val Iterations = 15
	final val Forks      = 2

	final val Benchmarks = Seq[Class[_]](
		classOf[Simple],
		classOf[BrainFuck],
	)

	@throws[RunnerException]
	def main(args: Array[String]): Unit = {
		import cats.implicits._

		import scala.collection.JavaConverters._

		val builder = new OptionsBuilder()
			.warmupIterations(Iterations)
			.measurementIterations(Iterations)
			.forks(Forks)
//    		.addProfiler(classOf[StackProfiler])
//    		.addProfiler(classOf[GCProfiler])
//    		.addProfiler(classOf[HotspotCompilationProfiler])
			.shouldFailOnError(true)
			.resultFormat(ResultFormatType.JSON)
			.result("docs/data.json")

		val result = new Runner(
			Benchmarks.foldLeft(builder) { (acc, x) => acc.include(x.getSimpleName) }.build)
			.run.asScala.toSeq

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

		// for latex
		println("Result:\n" +
				(Seq("id", "method", "score", "error") +: table)
					.map {_.mkString(" ")}.mkString("\n"))
	}

}
