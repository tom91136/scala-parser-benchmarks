package net.kurobako.spb.brainfuck

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import net.kurobako.spb
import net.kurobako.spb.Collectors.Result
import net.kurobako.spb.brainfuck.BrainFuck._
import net.kurobako.spb.brainfuck.BrainFuckSpec.BrainFuckRuntime
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{EitherValues, FlatSpec, Matchers}

import scala.annotation.tailrec
import scala.collection.immutable.LongMap
import scala.io.Source

class BrainFuckSpec extends FlatSpec with Matchers with EitherValues {


	"all parsing def" should "give the same result" in {
		val bf = new BrainFuck
		forAll(Table(
			"def",
			bf.baselineRecursiveDecent _,
			bf.warmScalaParserCombinator _,
			bf.warmAtto _,
			bf.warmParsley _,
			bf.warmFastParse _,
//			bf.warmParseback _,

			bf.coldScalaParserCombinator _,
			bf.coldAtto _,
			bf.coldParsley _,
			bf.coldFastParse _,
//			bf.coldParseback _,
		)) { method =>

			forAll(Table(
				("file", "expected"),
				("brainfuck/helloworld_comments.bf", "brainfuck/helloworld_comments.txt"),
				("brainfuck/helloworld.bf", "brainfuck/helloworld.txt"),
				("brainfuck/fizzbuzz.bf", "brainfuck/fizzbuzz.txt"),
				//				("brainfuck/mandelbrot.bf", "brainfuck/mandelbrot.txt"), // too slow
			)) { (file: String, expected: String) =>
				val context = new spb.brainfuck.BrainFuck.Context()
				context.file = file

				context.setup()

				val actual: Result[bf.BFOps] = method(context)


				val expectedString = Source.fromResource(expected).mkString

				// some scripts output CRLF so we clean that up
				def normaliseLn(s: String) = s.replaceAll("\r\n", "\n")

				actual.map { ast =>
					val in = new ByteArrayInputStream(Array.empty)
					val out = new ByteArrayOutputStream()
					BrainFuckRuntime().execute(ast, in, out)
					out.toString(StandardCharsets.UTF_8.toString)
				}.map(normaliseLn).right.value shouldBe normaliseLn(expectedString)

			}
		}
	}

}
object BrainFuckSpec {

	// BF needs uint8, JVM doesn't have that
	//XXX should extend AnyVal but toplevel worksheets don't support this for some reason
	final case class UInt8 private(value: Int) {
		def ++ : UInt8 = UInt8(if (value == 255) 0 else value + 1)
		def -- : UInt8 = UInt8(if (value == 0) 255 else value - 1)
	}
	final object UInt8 {
		final lazy val Zero = UInt8(0)
		def coerce(value: Int): UInt8 = UInt8(value)
	}

	LongMap
	case class BrainFuckRuntime(cells: Map[Long, UInt8] = LongMap(), pointer: Long = 0) {
		def update[A, B](xs: Map[A, B], a: A, default: B)(f: B => B): Map[A, B] = {
			xs + (a -> f(xs.getOrElse(a, default)))
		}
		def execute(xs: Seq[BrainFuckOp], in: InputStream, out: OutputStream): BrainFuckRuntime = {

			@tailrec def step(that: BrainFuckRuntime, xs: List[BrainFuckOp]): BrainFuckRuntime = {
				xs match {
					case LeftPointer :: ys  => step(that.copy(pointer = that.pointer - 1), ys)
					case RightPointer :: ys => step(that.copy(pointer = that.pointer + 1), ys)
					case Increment :: ys    =>
						step(that.copy(cells = update(that.cells, that.pointer, UInt8.Zero) {_ ++}), ys)
					case Decrement :: ys    =>
						step(that.copy(cells = update(that.cells, that.pointer, UInt8.Zero) {_ --}), ys)
					case Output :: ys       =>
						out.write(that.cells.getOrElse(that.pointer, UInt8.Zero).value)
						step(that, ys)
					case Input :: ys        =>
						val read = in.read()
						// write 0 for EOF
						val coerced = UInt8.coerce(if (read == -1) 0 else read)
						step(that.copy(cells = cells.updated(that.pointer, coerced)), ys)
					case (l@Loop(ys)) :: zs =>
						val current = that.cells.getOrElse(that.pointer, UInt8.Zero)
						if (current == UInt8.Zero) step(that, zs) // JZ
						else step(that, ys ::: (l :: zs)) // JNZ
					case Nil                => that

				}
			}

			step(this, xs.toList)

		}
	}
}