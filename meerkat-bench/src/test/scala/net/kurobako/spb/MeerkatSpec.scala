package net.kurobako.spb

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import net.kurobako.spb.MeerkatBench.{BrainFuckBench, SimpleBench}
import net.kurobako.spb.fixture.BrainFuck.BrainFuckContext
import net.kurobako.spb.fixture.BrainFuck.BrainFuckSpec.BrainFuckRuntime
import net.kurobako.spb.fixture.Simple.SimpleContext
import net.kurobako.spb.fixture.Simple.SimpleSpec.{Expected, TestString}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{EitherValues, FlatSpec, Matchers}


class MeerkatSpec extends FlatSpec with Matchers with EitherValues {


	"simple" should "parse according to spec" in {
		val simple = new SimpleBench
		forAll(Table(
			("def", "input", "expected"),
			(simple.warmMeerkatBnf _, TestString, Expected),
			(simple.coldMeerkatBnf _, TestString, Expected),
		)) { (method, input, expected) =>
			val context = new SimpleContext
			context.token = TestString
			method(context).right.value shouldBe expected
		}


	}

	"brainfuck" should "parse according to spec" in {
		val bf = new BrainFuckBench
		forAll(Table(
			"def",
			bf.warmMeerkat _,
			bf.coldMeerkat _,
		)) { method =>

			forAll(Table(
				("file", "expected"),
				("brainfuck/helloworld.bf", "brainfuck/helloworld.txt"),
				("brainfuck/helloworld_comments.bf", "brainfuck/helloworld_comments.txt"),
				("brainfuck/fizzbuzz.bf", "brainfuck/fizzbuzz.txt"),
				//				("brainfuck/mandelbrot.bf", "brainfuck/mandelbrot.txt"), // too slow
			)) { (file: String, expected: String) =>
				val context = new BrainFuckContext()
				context.file = file
				context.setup()

				method(context).map { ast =>
					val in = new ByteArrayInputStream(Array.empty)
					val out = new ByteArrayOutputStream()
					//BrainFuckSpec.executeFixedMut(ast, in, out)
					BrainFuckRuntime().execute(ast, in, out)
					out.toString(StandardCharsets.UTF_8.toString)
				}.map(normaliseLn).right.value shouldBe normaliseLn(readFully(expected))

			}
		}
	}


}

