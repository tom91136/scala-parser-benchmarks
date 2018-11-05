//package net.kurobako.spb
//
//import java.util.regex.Pattern
//
//import AttoBench
//import BaselineRecursiveDescentBench
//import net.kurobako.spb.fastparse1_bench.FastParse1Bench
//import FastParse2Bench
//import MeerkatBench
//import Parboiled2Bench
//import ParsebackBench
//import ParsecForScalaBench
//import net.kurobako.spb.parsley_bench.ParsleyBench
//import net.kurobako.spb.scala_parser_combinator_bench.ScalaParserCombinatorsBench
//import org.openjdk.jmh.profile.GCProfiler
//import org.openjdk.jmh.results.format.ResultFormatType
//import org.openjdk.jmh.runner.options.OptionsBuilder
//import org.openjdk.jmh.runner.{Runner, RunnerException}
//
//object BenchmarkAll {
//
//	final val Iterations = 2
//	final val Forks      = 1
//
//	final val Benches = Seq(
//				FastParse1Bench,
//		FastParse2Bench,
//		//		Parboiled2Bench,
//		//		AttoBench,
//		//		ScalaParserCombinatorsBench,
//		//		MeerkatBench,
//		//		ParsecForScalaBench,
//		//		ParsleyBench,
//		//		ParsebackBench,
//		//		BaselineRecursiveDescentBench,
//	)
//
//	final val Benchmarks = Benches.flatMap(_.classes)
//
//	@throws[RunnerException]
//	def main(args: Array[String]): Unit = {
//		import cats.implicits._
//
//		import scala.collection.JavaConverters._
//
//		val builder = new OptionsBuilder()
//			.warmupIterations(Iterations)
//			.measurementIterations(Iterations)
//			.forks(Forks)
//			//    		.addProfiler(classOf[StackProfiler])
//			//			.addProfiler(classOf[GCProfiler])
//			//    		.addProfiler(classOf[HotspotCompilationProfiler])
//			.shouldFailOnError(true)
//			.resultFormat(ResultFormatType.JSON)
//			//    		.exclude(".*")
//			.result("docs/data.json")
//
//
//		val options = Benchmarks.foldLeft(builder)((acc, x) => acc.include(Pattern.quote(x.getCanonicalName))).build
//
//		println(s"Using classes: \n${Benchmarks.mkString("\t\n")}")
//		println(s"JMH include  : \n${options.getIncludes.asScala.mkString("\t\n")}")
//
//		val result = new Runner(options).run.asScala.toSeq
//
//		val table = result
//			.map { v =>
//				val result = v.getPrimaryResult
//				(result.getLabel, result.getScore, result.getScoreError)
//			}
//			.sortBy { case (_, score, _) => score }
//			.zipWithIndex
//			.map { case ((method, score, error), i) =>
//				List(i.toString, method, score.toString, error.toString)
//			}
//
//		// for latex
//		println("Result:\n" +
//				(Seq("id", "method", "score", "error") +: table)
//					.map {_.mkString(" ")}.mkString("\n"))
//	}
//
//}
