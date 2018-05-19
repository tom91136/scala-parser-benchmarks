package net.kurobako.spb

import net.kurobako.spb.simple.Simple
import org.openjdk.jmh.runner.{Runner, RunnerException}
import org.openjdk.jmh.runner.options.OptionsBuilder

object BenchmarkAll {

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

}
