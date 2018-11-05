package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Json.{JsonExpr, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

import scala.annotation.switch

object ParsleyBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

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

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def warmParsleyChainL(ctx: SimpleContext): Result[Int] = !!(collectParsley(Simple._chainL, ctx.token))
		@Benchmark def coldParsleyChainL(ctx: SimpleContext): Result[Int] = !!(collectParsley(Simple.chainL(), ctx.token))
	}

	class JsonBench extends JsonBenchSpec {
		@Benchmark def warmParsley(ctx: JsonContext): Result[JsonExpr] = !!(collectParsley(Json._parser, ctx.input))
		@Benchmark def coldParsley(ctx: JsonContext): Result[JsonExpr] = !!(collectParsley(Json.parser(), ctx.input))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def warmParsley(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collectParsley(BrainFuck._parser, ctx.input))
		@Benchmark def coldParsley(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collectParsley(BrainFuck.parser(), ctx.input))
	}

	object Simple {

		import parsley.Char._
		import parsley.Combinator.chainl1
		import parsley.Parsley._
		import parsley._

		final val _chainL = chainL()
		@inline final def chainL(): Parsley[Int] = {
			chainl1('1' <#> (_.toInt), '+' #> ((x: Int, y: Int) => x + y))
		}
	}

	object BrainFuck {

		import parsley.Char._
		import parsley.Combinator._
		import parsley.Parsley._
		import parsley.{LanguageDef, Parsley, Predicate, TokenParser}

		final val _parser: Parsley[List[BrainFuckOp]] = parser()
		final def parser(): Parsley[List[BrainFuckOp]] = {
			val bflang = LanguageDef.plain.copy(space = Predicate(c => (c: @switch) match {
				case '>' | '<' | '+' | '-' | '.' | ',' | '[' | ']' => false
				case _                                             => true
			}))
			val tok = new TokenParser(bflang)
			lazy val bf: Parsley[List[BrainFuckOp]] =
				many(tok.lexeme('>' #> RightPointer)
					 <|> tok.lexeme('<' #> LeftPointer)
					 <|> tok.lexeme('+' #> Increment)
					 <|> tok.lexeme('-' #> Decrement)
					 <|> tok.lexeme('.' #> Output)
					 <|> tok.lexeme(',' #> Input)
					 <|> tok.brackets(bf.map(Loop)))
			tok.whiteSpace *> attempt(bf <* eof) <|> fail("\"]\" closes a loop, but there isn't one open")
		}
	}

	object Json {

		import parsley.Combinator._
		import parsley.Parsley._
		import parsley.{Char, LanguageDef, Parsley, Predicate, TokenParser}

		final val _parser: Parsley[JsonExpr] = parser()

		final def parser(): Parsley[JsonExpr] = {
			val jsontoks = LanguageDef.plain.copy(space = Predicate(Char.isWhitespace))
			val tok = new TokenParser(jsontoks)
			lazy val obj: Parsley[Obj] = tok.braces(
				tok.commaSep(+(string <~> tok.colon *> value)).map(Obj))
			lazy val array: Parsley[Arr] = tok.brackets(tok.commaSep(value)).map(Arr)
			lazy val string: Parsley[Str] = tok.stringLiteral.map(Str)
			lazy val value: Parsley[JsonExpr] = (string
												 <|> tok.symbol("true") #> Bool(true)
												 <|> tok.symbol("false") #> Bool(false)
												 <|> tok.symbol("null") #> Null
												 <|> attempt(tok.float.map {Num})
												 <|> attempt(tok.integer.map { x => Num(x.toDouble) })
												 <|> array
												 <|> obj)
			tok.whiteSpace *> (obj <|> array) <* eof
		}
	}

}