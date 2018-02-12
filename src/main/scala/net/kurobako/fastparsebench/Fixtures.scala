package net.kurobako.fastparsebench

import fastparse.all
import fastparse.core.Parsed.{Failure, Success}

import scala.annotation.tailrec

object Fixtures {


	def collect[B](parser: all.Parser[Int], input: String): Either[String, Int] = {
		parser.parse(input) match {
			case Success(value, _)        => Right(value)
			case f: Failure[Char, String] => Left(f.msg)
		}
	}


	def parseParsecForScalaChainL1(input: String): Either[String, Int] = {
		import parsec._
		import parsec.Char._
		import parsec.Combinator._
		val x: Parser[Int] = string("1") <#> {_ (0).toInt}
		val y: Parser[Int => Int => Int] = char('+') <#> { _ => (x: Int) => (y: Int) => x + y }
		val value = runParser[Stream[String, Char], Unit, Int](chainl1(x, y), (), "", input)
		value.left.map {_.toString()}
	}

	def parseBindFoldCurried(input: String): Either[String, Int] = {
		import fastparse.all._
		def chainlf[A](p: Parser[A], op: Parser[A => A => A]): Parser[A] = {
			for (x <- p;
				 fs <- (for (f <- op;
							 y <- p)
					 yield (x: A) => f(x)(y)).rep)
				yield fs.foldLeft(x)((y, f) => f(y))
		}

		val x = P("1").!.map(_ (0).toInt)
		val y = P("+").!.map(_ => (x: Int) => (y: Int) => x + y)
		collect(chainlf(x, y), input)
	}

	def parseBindFoldTuple2(input: String): Either[String, Int] = {
		import fastparse.all._
		def chainlf[A](p: Parser[A], op: Parser[(A, A) => A]): Parser[A] = {
			for (x <- p;
				 fs <- (for (f <- op;
							 y <- p)
					 yield (x: A) => f(x, y)).rep)
				yield fs.foldLeft(x)((y, f) => f(y))
		}

		val x = P("1").!.map(_ (0).toInt)
		val y = P("+").!.map(_ => (x: Int, y: Int) => x + y)
		collect(chainlf(x, y), input)
	}

	def parseBindRecursiveTuple2(input: String): Either[String, Int] = {
		import fastparse.all._
		type BinaryOp[A] = (A, A) => A

		def chainl[B](p: all.Parser[B], op: all.Parser[BinaryOp[B]]): all.Parser[B] = {
			// TODO make tail recursive
			def rest(x: B): all.Parser[B] = (for {
				f <- op
				y <- p
				z <- rest(f(x, y))
			} yield z) | PassWith(x)

			p.flatMap {rest}
		}

		val x = P("1").!.map(_ (0).toInt)
		val y: Parser[BinaryOp[Int]] = P("+").map { _ => (x, y) => x + y }
		collect(chainl(x, y), input)
	}

	def parseBindRecursiveCurried(input: String): Either[String, Int] = {
		import fastparse.all._
		type BinaryOp[A] = A => A => A


		//		def tailRecP[A, B](a: A)(f: A => Parser[Either[A, B]]): Parser[B] = {
		//			import fastparse.parsers.Terminals
		//
		//			f(a) match {
		//				case term @ Terminals.Pass()
		//					 | Terminals.Fail()
		//					 | Terminals.AnyElem(_)
		//					 | Terminals.Start()
		//					 | Terminals.End()
		//					 | Terminals.IgnoreCase(_)
		//					 | Terminals.Index()
		//				case _                   => ???
		//			}
		//		}

		def chainl[B](p: all.Parser[B], op: all.Parser[BinaryOp[B]]): all.Parser[B] = {
			// TODO make tail recursive
			def rest(x: B): all.Parser[B] = (for {
				f <- op
				y <- p
				z <- rest(f(x)(y)) ~/
			} yield z) | PassWith(x)

			p.flatMap {rest}
		}

		val x = P("1").!.map(_ (0).toInt)
		val y: Parser[BinaryOp[Int]] = P("+").map { _ => x => y => x + y }
		collect(chainl(x, y), input)
	}

	def parseRightRecursive(input: String): Either[String, Int] = {
		import fastparse.all._
		val x = P("1").!.map(_ (0).toInt)
		val y = (x ~ ("+" ~/ x).rep).map { case (l, xs) => l + xs.sum }
		collect(y, input)
	}

	def parseHandRollRD(input: String): Either[String, Int] = {
		@tailrec def parseTR(input: String, index: Int, sum: Int): Int = {
			if (index >= input.length) return sum
			input.charAt(index) match {
				case c@'1' => parseTR(input, index + 1, sum + c)
				case '+'   => parseTR(input, index + 1, sum)
			}
		}

		Right(parseTR(input, 0, 0))
	}


}
