package net.kurobako.spb.fixture

import java.io.{InputStream, OutputStream}
import java.util.concurrent.TimeUnit

import net.kurobako.spb.BenchSpec
import org.openjdk.jmh.annotations._

import scala.annotation.tailrec
import scala.collection.immutable.LongMap
import scala.io.Source

object BrainFuck {

	sealed trait BrainFuckOp
	case object RightPointer extends BrainFuckOp
	case object LeftPointer extends BrainFuckOp
	case object Increment extends BrainFuckOp
	case object Decrement extends BrainFuckOp
	case object Output extends BrainFuckOp
	case object Input extends BrainFuckOp
	case class Loop(p: List[BrainFuckOp]) extends BrainFuckOp

	@State(Scope.Thread)
	class BrainFuckContext {
		@Param(Array(
			"brainfuck/helloworld.bf",
			"brainfuck/helloworld_comments.bf",
			"brainfuck/fizzbuzz.bf",
			"brainfuck/mandelbrot.bf",
		))
		var file : String = _
		var input: String = _
		@Setup def setup(): Unit = {input = Source.fromResource(file).mkString}
	}

	@OutputTimeUnit(TimeUnit.NANOSECONDS)
	@BenchmarkMode(Array(Mode.AverageTime))
	abstract class BrainFuckBenchSpec extends BenchSpec
	
	object BrainFuckSpec {

		// BF needs uint8, JVM doesn't have that
		final case class UInt8 private(value: Int) extends AnyVal {
			@inline def ++ : UInt8 = UInt8(if (value == 255) 0 else value + 1)
			@inline def -- : UInt8 = UInt8(if (value == 0) 255 else value - 1)
		}
		final object UInt8 {
			final lazy val Zero = UInt8(0)
			@inline def coerce(value: Int): UInt8 = UInt8(value)
		}

		case class BrainFuckRuntime(cells: Map[Long, UInt8] = LongMap(), pointer: Long = 0) {
			@inline def update[A, B](xs: Map[A, B], a: A, default: B)(f: B => B): Map[A, B] = {
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

}
