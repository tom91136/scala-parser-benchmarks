package net.kurobako.spb

import net.kurobako.spb.Simple.Result


// fixtures cannot be nested, see https://github.com/ktoso/sbt-jmh/issues/69

object ScalaParserCombinatorFixtures {

	import scala.util.parsing.combinator._

	final val p = new SPCParser

	final class SPCParser extends RegexParsers {
		// XXX we need those implicit conversions but NOT the regex stuff
		@inline def x: Parser[Int] = literal("1") ^^ { x: String => x.head.toInt }
		@inline def y: Parser[(Int, Int) => Int] = literal("+") ^^ { _ => (x: Int, y: Int) => x + y }
		@inline def chainl[B](p: Parser[B], op: Parser[(B, B) => B]): Parser[B] = {
			// TailRecM ???
			def rest(x: B): Parser[B] = (for {
				f <- op
				y <- p
				z <- rest(f(x, y))
			} yield z) | success(x)

			p.flatMap {rest}
		}
	}

	@inline final def collect(parser_ : p.Parser[Int], input: String): Result = {
		p.parse(parser_, input) match {
			case p.Success(v: Int, _) => Right(v)
			case v: p.NoSuccess       => Left(v.msg)
		}
	}

	final val _chainL = chainL()
	@inline final def chainL(): p.Parser[Int] = p.chainl(p.x, p.y)

}

object AttoFixtures {

	import atto._
	import atto.parser.all._

	@inline final def collect(_parser: Parser[Int], input: String): Result = {
		import Atto._
		// XXX apparently, naming the parser `parser` shadows some import...
		_parser.parseOnly(input).either
	}
	final val _chainL = chainL()
	@inline final def chainL(): Parser[Int] = {
		def chainl[B](p: Parser[B], op: Parser[(B, B) => B]): Parser[B] = {
			// TailRecM ???
			def rest(x: B): Parser[B] = choice(for {
				f <- op
				y <- p
				z <- rest(f(x, y))
			} yield z, ok(x))

			p.flatMap {rest}
		}

		val x = string("1").map(_ (0).toInt)
		val y = char('+').map { _ => (x: Int, y: Int) => x + y }
		chainl(x, y)
	}
}

object ParserForScalaFixtures {

	import parsec.Char._
	import parsec.Combinator.chainl1
	import parsec._

	@inline final def collect(parser: Parser[Int], input: String): Simple.Result = {
		runParser[Stream[String, Char], Unit, Int](parser, (), "", input)
			.left.map {_.toString()}
	}
	final val _chainL = chainL()
	@inline final def chainL(): Parser[Int] = {
		val x: Parser[Int] = string("1") <#> {_ (0).toInt}
		val y: Parser[Int => Int => Int] = char('+') <#> { _ => (x: Int) => (y: Int) => x + y }
		chainl1(x, y)
	}
}

object ParsleyFixtures {

	import parsley.Parsley.chainl1
	import parsley._

	@inline final def collect(parser: Parsley[Int], input: String): Simple.Result = {
		runParser(parser, input) match {
			case Success(x)   => Right(x)
			case Failure(msg) => Left(msg)
		}
	}
	final val _chainL = chainL()
	@inline final def chainL(): Parsley[Int] = {
		chainl1('1' <#> (_.toInt), '+' #> ((x: Int) => (y: Int) => x + y))
	}
}

object FastParseFixtures {

	import fastparse.all
	import fastparse.all._
	import fastparse.core.Parsed.{Failure, Success}

	@inline final def collect[B](parser: Parser[Int], input: String): Result = {
		parser.parse(input) match {
			case Success(value, _)        => Right(value)
			case f: Failure[Char, String] => Left(f.msg)
		}
	}
	final val _foldC = foldC()
	@inline final def foldC(): Parser[Int] = {
		def chainlf[A](p: Parser[A], op: Parser[A => A => A]): Parser[A] = {
			for (x <- p;
				 fs <- (for (f <- op;
							 y <- p)
					 yield (x: A) => f(x)(y)).rep)
				yield fs.foldLeft(x)((y, f) => f(y))
		}

		val x = P("1").!.map(_ (0).toInt)
		val y = P("+").!.map(_ => (x: Int) => (y: Int) => x + y)
		chainlf(x, y)
	}
	final val _foldT2 = foldT2()
	@inline final def foldT2(): Parser[Int] = {
		def chainlf[A](p: Parser[A], op: Parser[(A, A) => A]): Parser[A] = {
			for (x <- p;
				 fs <- (for (f <- op;
							 y <- p)
					 yield (x: A) => f(x, y)).rep)
				yield fs.foldLeft(x)((y, f) => f(y))
		}

		val x = P("1").!.map(_ (0).toInt)
		val y = P("+").!.map(_ => (x: Int, y: Int) => x + y)
		chainlf(x, y)
	}
	final val _recursiveC = recursiveC()
	@inline final def recursiveC(): Parser[Int] = {
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
		chainl(x, y)
	}
	final val _recursiveT2 = recursiveT2()
	@inline final def recursiveT2(): Parser[Int] = {
		type BinaryOp[A] = A => A => A

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
		chainl(x, y)
	}
	final val _rightRecursive = rightRecursive()
	@inline final def rightRecursive(): Parser[Int] = {
		val x = P("1").!.map(_ (0).toInt)
		(x ~ ("+" ~/ x).rep).map { case (l, xs) => l + xs.sum }
	}
}