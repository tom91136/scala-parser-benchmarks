import java.io.{InputStream, OutputStream}
import BrainFuckRuntime.UInt8
import fastparse.WhitespaceApi
import scala.annotation.tailrec


sealed trait BrainFuckOp
case object RightPointer extends BrainFuckOp
case object LeftPointer extends BrainFuckOp
case object Increment extends BrainFuckOp
case object Decrement extends BrainFuckOp
case object Output extends BrainFuckOp
case object Input extends BrainFuckOp
case class Loop(p: List[BrainFuckOp]) extends BrainFuckOp


val White = WhitespaceApi.Wrapper {
	import fastparse.all._
	NoTrace(" ".rep)
}

import White._
import fastparse.noApi._

lazy val ops: Parser[BrainFuckOp] =
	CharIn("<>+-.,").!.map {
		case "<" => LeftPointer
		case ">" => RightPointer
		case "+" => Increment
		case "-" => Decrement
		case "." => Output
		case "," => Input
	}.opaque("operators(<>+-.,)")

lazy val loop: Parser[Seq[BrainFuckOp]] =
	P("[".opaque("Opening bracket '['") ~/
	  (expr | PassWith(Nil)).opaque("expression") ~ // [] is ok
	  "]".opaque("']' Closing bracket"))
		.map { l => Loop(l.toList) :: Nil }

lazy val expr = (loop | ops.rep(1)).rep.map {_.flatten} // empty should fail

lazy val parser: Parser[Seq[BrainFuckOp]] = Start ~ expr ~ End


object BrainFuckRuntime {
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
}


case class BrainFuckRuntime(cells: Map[Int, UInt8] = Map(), pointer: Int = 0) {
	def update[A, B](xs: Map[A, B], a: A, default: B)(f: B => B): Map[A, B] = {
		xs + (a -> f(xs.getOrElse(a, default)))
	}
	def execute(xs: Seq[BrainFuckOp], in: InputStream, out: OutputStream): BrainFuckRuntime = {
		@tailrec
		def step(that: BrainFuckRuntime, xs: List[BrainFuckOp]): BrainFuckRuntime = {
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

val parsed = parser.parse("++++++++" +
						  "[>++++" +
						  "[>++>+++>+++>+<<<<-]" +
						  ">+>+>->>+" +
						  "[<]" +
						  "<-]" +
						  ">>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.")

BrainFuckRuntime().execute(parsed.get.value, System.in, System.out)




