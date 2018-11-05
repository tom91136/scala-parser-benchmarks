package net.kurobako.spb

import org.scalatest.{EitherValues, FlatSpec, Matchers}


class ParsebackSpec extends FlatSpec with Matchers with EitherValues {

	// FIXME resync and check
//	"brainfuck" should "parse according to spec" in {
//		val bf = new BrainFuckBench
//		forAll(Table(
//			"def",
//			bf.warmParseback _,
//			bf.coldParseback _,
//		)) { method =>
//
//			forAll(Table(
//				("file", "expected"),
//				("brainfuck/helloworld.bf", "brainfuck/helloworld.txt"),
//				("brainfuck/helloworld_comments.bf", "brainfuck/helloworld_comments.txt"),
//				("brainfuck/fizzbuzz.bf", "brainfuck/fizzbuzz.txt"),
								("brainfuck/mandelbrot.bf", "brainfuck/mandelbrot.txt"), // too slow
//			)) { (file: String, expected: String) =>
//				val context = new BrainFuckContext()
//				context.file = file
//				context.setup()
//
//				method(context).map { ast =>
//					val in = new ByteArrayInputStream(Array.empty)
//					val out = new ByteArrayOutputStream()
//					BrainFuckRuntime().execute(ast, in, out)
//					out.toString(StandardCharsets.UTF_8.toString)
//				}.map(normaliseLn).right.value shouldBe normaliseLn(readFully(expected))
//
//			}
//		}
//	}

}

