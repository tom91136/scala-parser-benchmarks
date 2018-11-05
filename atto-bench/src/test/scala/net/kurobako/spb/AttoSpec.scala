package net.kurobako.spb

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets

import net.kurobako.spb.AttoBench.{BrainFuckBench, JsonBench, SimpleBench}
import net.kurobako.spb.fixture.BrainFuck.BrainFuckContext
import net.kurobako.spb.fixture.BrainFuck.BrainFuckSpec.BrainFuckRuntime
import net.kurobako.spb.fixture.Json.JsonContext
import net.kurobako.spb.fixture.Simple.SimpleContext
import net.kurobako.spb.fixture.Simple.SimpleSpec.{Expected, TestString}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{EitherValues, FlatSpec, Matchers}


class AttoSpec extends FlatSpec with Matchers with EitherValues {


	"simple" should "parse according to spec" in {
		val simple = new SimpleBench
		forAll(Table(
			("def", "input", "expected"),
			(simple.warmAttoChainL _, TestString, Expected),
			(simple.coldAttoChainL _, TestString, Expected),
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
			bf.warmAtto _,
			bf.coldAtto _,
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

	"json" should "parse according to spec" in {
		val bf = new JsonBench
		forAll(Table(
			"def",
			bf.warmAtto _,
			bf.coldAtto _,
		)) { method =>

			forAll(Table(
				("file", "expected"),
				("json/arr0.json", "json/arr0.txt"),
				("json/arr_nums.json", "json/arr_nums.txt"),
				("json/arr_strs.json", "json/arr_strs.txt"),
				("json/mixed.json", "json/mixed.txt"),
				("json/obj0.json", "json/obj0.txt"),
				("json/obj1.json", "json/obj1.txt"),
				("json/obj2.json", "json/obj2.txt"),
				//				("json/medium.json", "json/medium.txt"),
				//				("json/long.json", ""),
			)) { (file: String, expected: String) =>
				val context = new JsonContext
				context.file = file
				context.setup()
				method(context).map { c => c.toString }.right.value shouldBe readFully(expected)
			}
		}
	}

}

