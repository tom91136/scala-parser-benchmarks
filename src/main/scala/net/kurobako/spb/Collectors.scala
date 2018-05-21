package net.kurobako.spb


import scala.util.Either

object Collectors {


	type Result[A] = Either[String, A]

	// XXX this has to be as optimised as possible hence the abomination
	@inline def !![A](r: Result[A]): Result[A] = {
		if (r.isLeft) throw new AssertionError(r.left.get)
		r
	}

	@inline final def collectParsley[A](parser: parsley.Parsley[A],
										input: String): Result[A] = {
		// XXX if SMP, uncomment line below
		// implicit val ctx = parsley.giveContext
		import parsley._
		// XXX parsley
		runParserFastUnsafe(parser, input) match {
			case Success(x)   => Right(x)
			case Failure(msg) => Left(msg)
		}
	}

	@inline final def collectAtto[A](_parser: atto.Parser[A],
									 input: String): Result[A] = {
		import atto._
		import Atto._
		// XXX apparently, naming the parser `parser` shadows some import...
		_parser.parseOnly(input).either
	}


	// SPC uses type members so we have this odd type signature
	@inline final def collectSPC[
	P <: scala.util.parsing.combinator.Parsers {type Elem = Char},
	A](parser_ : P#Parser[A], input: String): Result[A] = {
		import scala.util.parsing.input.CharSequenceReader
		val x: P#ParseResult[A] = parser_.apply(new CharSequenceReader(input))
		x match {
			case _: P#Success[_] => Right(x.get)
			case f: P#NoSuccess  => Left(f.msg)
			case e               => Left(s"Unexpected parse error $e")
		}
	}


	@inline final def collectParsec[A](parser: parsec.Parser[A],
									   input: String): Result[A] = {
		import parsec._
		runParser[parsec.Stream[String, Char], Unit, A](parser, (), "", input)
			.left.map {_.toString()}
	}


	@inline final def collectFastParse[A](parser: fastparse.all.Parser[A],
										  input: String): Result[A] = {
		import fastparse.core.Parsed.{Failure, Success}
		parser.parse(input) match {
			case Success(value, _)        => Right(value)
			case f: Failure[Char, String] => Left(f.msg)
		}
	}

	@inline final def collectParseback[A](parser: parseback.Parser[A], input: String): Result[A] = {
		import cats._
		import parseback.util.Catenable
		import parseback.{LineStream, ParseError}
		val foo = LineStream[Eval](input)
		val value: Either[List[ParseError], Catenable[A]] = parser(foo).value
		value.fold({ es => Left(es.toString) }, { xs => println(xs); Right(xs.toList.head) })
	}


	// parboiled2 expands to a macro so there is no way to execute a generic parser
	@inline final def collectParboiled2[A](parser: String => Either[String, A],
										   input: String): Result[A] = {
		parser(input)
	}


	@inline final def collectMeerkat[A](parser: org.meerkat.parsers.&[org.meerkat.parsers.Parsers.Nonterminal, A],
										input: String): Result[A] = {
		import org.meerkat.parsers.exec
		exec(parser, input).left.map {_.toString}
	}

}
