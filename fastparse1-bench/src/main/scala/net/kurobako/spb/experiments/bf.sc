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


val Bracket = "[]"
val Keywords = "<>+-.,"

val White = WhitespaceApi.Wrapper {
	import fastparse.all._
	val Reserved = (Bracket + Keywords).toSet
	NoTrace(ElemsWhile(!Reserved.contains(_)).rep)
}

import White._
import fastparse.noApi._

lazy val ops: Parser[BrainFuckOp] =
	CharIn(Keywords).!.map {
		case "<" => LeftPointer
		case ">" => RightPointer
		case "+" => Increment
		case "-" => Decrement
		case "." => Output
		case "," => Input
	}.opaque(s"keywords($Keywords)")

lazy val loop: Parser[List[BrainFuckOp]] =
	P("[".opaque("Opening bracket '['") ~/
	  (expr | PassWith(Nil)).opaque("expression") ~ // [] is ok
	  "]".opaque("']' Closing bracket"))
		.map {Loop(_) :: Nil}

lazy val expr: Parser[List[BrainFuckOp]] = (loop | ops.rep(1)).rep
	.map {_.flatten.toList} // empty should fail

lazy val parser: Parser[List[BrainFuckOp]] = Start ~ expr ~ End


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

// taken from https://en.wikipedia.org/wiki/Brainfuck#Hello_World!

val withComments = parser.parse(
	"""
	  |[ This program prints "Hello World!" and a newline to the screen, its
	  |  length is 106 active command characters. [It is not the shortest.]
	  |
	  |  This loop is an "initial comment loop", a simple way of adding a comment
	  |  to a BF program such that you don't have to worry about any command
	  |  characters. Any ".", ",", "+", "-", "<" and ">" characters are simply
	  |  ignored, the "[" and "]" characters just have to be balanced. This
	  |  loop and the commands it contains are ignored because the current cell
	  |  defaults to a value of 0; the 0 value causes this loop to be skipped.
	  |]
	  |++++++++               Set Cell #0 to 8
	  |[
	  |    >++++               Add 4 to Cell #1; this will always set Cell #1 to 4
	  |    [                   as the cell will be cleared by the loop
	  |        >++             Add 2 to Cell #2
	  |        >+++            Add 3 to Cell #3
	  |        >+++            Add 3 to Cell #4
	  |        >+              Add 1 to Cell #5
	  |        <<<<-           Decrement the loop counter in Cell #1
	  |    ]                   Loop till Cell #1 is zero; number of iterations is 4
	  |    >+                  Add 1 to Cell #2
	  |    >+                  Add 1 to Cell #3
	  |    >-                  Subtract 1 from Cell #4
	  |    >>+                 Add 1 to Cell #6
	  |    [<]                 Move back to the first zero cell you find; this will
	  |                        be Cell #1 which was cleared by the previous loop
	  |    <-                  Decrement the loop Counter in Cell #0
	  |]                       Loop till Cell #0 is zero; number of iterations is 8
	  |
	  |The result of this is:
	  |Cell No :   0   1   2   3   4   5   6
	  |Contents:   0   0  72 104  88  32   8
	  |Pointer :   ^
	  |
	  |>>.                     Cell #2 has value 72 which is 'H'
	  |>---.                   Subtract 3 from Cell #3 to get 101 which is 'e'
	  |+++++++..+++.           Likewise for 'llo' from Cell #3
	  |>>.                     Cell #5 is 32 for the space
	  |<-.                     Subtract 1 from Cell #4 for 87 to give a 'W'
	  |<.                      Cell #3 was set to 'o' from the end of 'Hello'
	  |+++.------.--------.    Cell #3 for 'rl' and 'd'
	  |>>+.                    Add 1 to Cell #5 gives us an exclamation point
	  |>++.                    And finally a newline from Cell #6
	""".stripMargin)

val withoutComments = parser.parse("++++++++" +
						  "[>++++" +
						  "[>++>+++>+++>+<<<<-]" +
						  ">+>+>->>+" +
						  "[<]" +
						  "<-]" +
						  ">>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++.")

BrainFuckRuntime().execute(withComments.get.value, System.in, System.out)
BrainFuckRuntime().execute(withoutComments.get.value, System.in, System.out)




