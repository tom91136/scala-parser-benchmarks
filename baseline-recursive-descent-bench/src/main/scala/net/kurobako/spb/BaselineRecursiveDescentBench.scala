package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

import scala.annotation.{switch, tailrec}
import scala.util.Try

object BaselineRecursiveDescentBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[BrainFuckBench])

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def baselineTailRecursiveTry(ctx: SimpleContext): Result[Int] = !!(Simple.tailRecursiveTry(ctx.token))
		@Benchmark def baselineRecursiveDecent(ctx: SimpleContext): Result[Int] = !!(Simple.recursiveDecent(ctx.token))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def baselineRecursiveDecent(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(BrainFuck.parse(ctx.input))
	}

	object Simple {
		@tailrec final def parseSimple(input: String, index: Int, sum: Int): Int = {
			if (index >= input.length) return sum
			input.charAt(index) match {
				case c@'1' => parseSimple(input, index + 1, sum + c)
				case '+'   => parseSimple(input, index + 1, sum)
				case x     => sys.error(s"unexpected $x") // intentional
			}
		}

		@inline
		final def tailRecursiveTry(input: String): Either[String, Int] = Try {parseSimple(input, 0, 0)}
			.toEither.left.map(_.getMessage)

		@inline
		final def one(input: String, index: Int): (Either[String, Int], Int) = {
			if (index < input.length && input(index) == '1') (Right('1'.toInt), index + 1)
			else (Left(s"$index: Expected 1, got ${if (index < input.length) input(index) else "end of input"}"), index)
		}

		@inline
		final def plus(input: String, index: Int): (Either[String, (Int, Int) => Int], Int) = {
			if (index < input.length && input(index) == '+') (Right(_ + _), index + 1)
			else (Left(s"$index: Expected +, got ${if (index < input.length) input(index) else "end of input"}"), index)
		}

		// FIXME excessive underscores
		@inline
		final def expr(input: String, index: Int): (Either[String, Int], Int) = {
			one(input, index) match {
				case (Right(x), index_) => plus(input, index_) match {
					case (Right(op), index__) => expr(input, index__) match {
						case (Right(y), index___) => (Right(op(x, y)), index___)
						case (err, index___)      => (err, index___)
					}
					case (_, index__)         => (Right(x), index__)
				}
				case err                => err
			}
		}

		@inline
		final def recursiveDecent(input: String): Either[String, Int] = expr(input: String, 0)._1
	}

	object BrainFuck {

		val recursiveDescent: String => Either[String, Int] = (input: String) => expr(input, 0)._1
		def expr(input: String, index: Int): (Either[String, Int], Int) = {
			one(input, index) match {
				case (Right(x), index_) => plus(input, index_) match {
					case (Right(op), index__) => expr(input, index__) match {
						case (Right(y), index___) => (Right(op(x)(y)), index___)
						case (err, index___)      => (err, index___)
					}
					case (_, index__)         => (Right(x), index__)
				}
				case (err, index_)      => (err, index_)
			}
		}
		def exprl(input: String, index: Int): (Either[String, Int], Int) = {
			one(input, index) match {
				case (Right(x), index_) =>
					val (ops, index__) = rep(plusone)(input, index_)
					(Right(ops.foldLeft(x)((acc, op) => op(acc))), index__)
				case err                => err
			}
		}
		@tailrec def rep[A](p: (String, Int) => (Either[String, A], Int))
						   (input: String, index: Int, acc: List[A] = Nil): (List[A], Int) = p(input, index) match {
			case (Right(x), index_) => rep(p)(input, index_, x :: acc)
			case (_, index_)        => (acc.reverse, index_)
		}
		def one(input: String, index: Int): (Either[String, Int], Int) = {
			if (index < input.length && input(index) == '1') (Right('1'.toInt), index + 1)
			else (Left(s"$index: Expected 1, got ${if (index < input.length) input(index) else "end of input"}"), index)
		}
		def plus(input: String, index: Int): (Either[String, Int => Int => Int], Int) = {
			if (index < input.length && input(index) == '+') (Right((x: Int) => (y: Int) => x + y), index + 1)
			else (Left(s"$index: Expected +, got ${if (index < input.length) input(index) else "end of input"}"), index)
		}
		def plusone(input: String, index: Int): (Either[String, Int => Int], Int) = {
			plus(input, index) match {
				case (Right(op), index_)  => one(input, index_) match {
					case (Right(y), index__)  => (Right((z: Int) => op(z)(y)), index__)
					case (Left(err), index__) => (Left(err), index__)
				}
				case (Left(err), index__) => (Left(err), index__)
			}
		}

		val parseTail: String => Int = (input: String) => parseTail_(input, 0, 0)
		@tailrec def parseTail_(input: String, index: Int, sum: Int): Int = {
			if (index >= input.length) sum
			else input(index) match {
				case c@'1' => parseTail_(input, index + 1, sum + c)
				case '+'   => parseTail_(input, index + 1, sum)
			}
		}

		def parse(in: String): Result[List[BrainFuckOp]] = {
			@tailrec def walk(in: List[Char], acc: List[BrainFuckOp] = Nil): Either[String, (List[BrainFuckOp], List[Char])] = in match {
				case (']' :: _) | Nil => Right((acc.reverse, in))
				case c :: rest        => (c: @switch) match {
					case '>' => walk(rest, RightPointer :: acc)
					case '<' => walk(rest, LeftPointer :: acc)
					case '+' => walk(rest, Increment :: acc)
					case '-' => walk(rest, Decrement :: acc)
					case '.' => walk(rest, Output :: acc)
					case ',' => walk(rest, Input :: acc)
					case '[' =>
						loop(rest) match {
							case Right((body, rest_)) => walk(rest_, Loop(body) :: acc)
							case x                    => x
						}
					case _   => walk(rest, acc)
				}
			}
			def loop(in: List[Char]): Either[String, (List[BrainFuckOp], List[Char])] = for {
				result <- walk(in)
				out <- result._2 match {
					case ']' :: rest_ => Right(result._1 -> rest_)
					case _            => Left("Unclosed loop :(")
				}
			} yield out
			walk(in.toList).right.map {_._1}
		}

	}

}