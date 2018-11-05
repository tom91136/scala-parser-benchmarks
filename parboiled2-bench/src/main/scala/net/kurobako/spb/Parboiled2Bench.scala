package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck._
import net.kurobako.spb.fixture.Json._
import net.kurobako.spb.fixture.Simple.SimpleBenchSpec
import org.openjdk.jmh.annotations.Benchmark

import scala.annotation.switch
import scala.util.Either

object Parboiled2Bench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[JsonBench], classOf[BrainFuckBench])

	// parboiled2 expands to a macro so there is no way to execute a generic parser
	@inline final def collect[A](parser: String => Either[String, A], input: String): Result[A] = parser(input)

	class SimpleBench extends SimpleBenchSpec {
		// TODO write me
	}

	class JsonBench extends JsonBenchSpec {
		@Benchmark def warmParboiled2(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json._parser, ctx.input))
		@Benchmark def coldParboiled2(ctx: JsonContext): Result[JsonExpr] = !!(collect(Json.parser(), ctx.input))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def warmParboiled2(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck._parser, ctx.input))
		@Benchmark def coldParboiled2(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collect(BrainFuck.parser(), ctx.input))
	}

	object Simple {
		// TODO write me
	}

	object BrainFuck {

		import org.parboiled2._

		class PB2Parser(val input: ParserInput) extends Parser {

			def full: Rule1[Seq[BrainFuckOp]] = rule {WhiteSpace ~ expr ~ EOI}

			/*_*/
			def expr: Rule1[Seq[BrainFuckOp]] = rule {op *}
			/*_*/

			def op: Rule1[BrainFuckOp] = rule {
				// optimised single char lookahead
				run {
					(cursorChar: @switch) match {
						case '<' => tok('<') ~> { () => LeftPointer }
						case '>' => tok('>') ~> { () => RightPointer }
						case '+' => tok('+') ~> { () => Increment }
						case '-' => tok('-') ~> { () => Decrement }
						case '.' => tok('.') ~> { () => Output }
						case ',' => tok(',') ~> { () => Input }
						case '[' => (tok('[') ~ expr ~ tok(']')) ~> { xs: Seq[BrainFuckOp] => Loop(xs.toList) }
						case _   => MISMATCH
					}
				}
			}

			//		def op: Rule1[BrainFuckOp] = rule {
			//			(tok('>') ~> { () => RightPointer }
			//			 | tok('<') ~> { () => LeftPointer }
			//			 | tok('+') ~> { () => Increment }
			//			 | tok('-') ~> { () => Decrement }
			//			 | tok('.') ~> { () => Output }
			//			 | tok(',') ~> { () => Input }
			//			 | (tok('[') ~ expr ~ tok(']')) ~> { xs: Seq[BrainFuckOp] => Loop(xs.toList) })
			//		}

			def tok(c: Char): Rule0 = rule {c ~ WhiteSpace}

			def WhiteSpace: Rule0 = rule {
				CharPredicate.from { c =>
					(c: @switch) match {
						case '>' | '<' | '+' | '-' | '.' | ',' | '[' | ']' => false
						// XXX implementation detail, using value instead of EOI because of identifier issues
						case '\uFFFF' => false
						case _        => true
					}
				} *
			}

		}

		import org.parboiled2.Parser.DeliveryScheme.Either

		// XXX cold/hot doesn't make a lot of sense here
		final def _parser: String => Either[String, List[BrainFuckOp]] = parser()
		final def parser(): String => Either[String, List[BrainFuckOp]] = { s: String =>
			val parser = new PB2Parser(s)
			parser.full.run().map {_.toList}.left.map {parser.formatError(_)}
		}

	}

	object Json {

		import org.parboiled2._

		class PB2Parser(val input: ParserInput) extends Parser with StringBuilding {

			import CharPredicate.{Digit, Digit19, HexDigit}

			def full: Rule1[JsonExpr] = rule {WhiteSpace ~ (JsonObject | JsonArray) ~ EOI}

			/*_*/
			def JsonObject: Rule1[Obj] = rule {
				ws('{') ~ zeroOrMore(Pair).separatedBy(ws(',')) ~ ws('}') ~> { xs: Seq[(Str, JsonExpr)] => Obj(xs.toList) }
			}
			/*_*/
			def Pair = rule {JsonString ~ ws(':') ~ Value ~> ((_, _))}

			def Value: Rule1[JsonExpr] = rule {
				// as an optimization of the equivalent rule:
				//   JsonString | JsonNumber | JsonObject | JsonArray | JsonTrue | JsonFalse | JsonNull
				// we make use of the fact that one-char lookahead is enough to discriminate the cases
				run {
					(cursorChar: @switch) match {
						case '"'                                                             => JsonString
						case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' => JsonNumber
						case '{'                                                             => JsonObject
						case '['                                                             => JsonArray
						case 't'                                                             => JsonTrue
						case 'f'                                                             => JsonFalse
						case 'n'                                                             => JsonNull
						case _                                                               => MISMATCH
					}
				}
			}


			def JsonString: Rule1[Str] = rule {'"' ~ clearSB() ~ Characters ~ ws('"') ~ push(sb.toString) ~> Str}
			def JsonNumber: Rule1[Num] = rule {capture(Integer ~ optional(Frac) ~ optional(Exp)) ~> { n: String => Num(n.toDouble) } ~ WhiteSpace}
			/*_*/
			def JsonArray: Rule1[Arr] = rule {
				ws('[') ~ zeroOrMore(Value).separatedBy(ws(',')) ~ ws(']') ~> { xs: Seq[JsonExpr] => Arr(xs.toList) }
			}
			/*_*/

			def Characters: Rule0 = rule {zeroOrMore(NormalChar | '\\' ~ EscapedChar)}
			def NormalChar: Rule0 = rule {!QuoteBackslash ~ ANY ~ appendSB()}


			def EscapedChar = rule(
				QuoteSlashBackSlash ~ appendSB()
				| 'b' ~ appendSB('\b')
				| 'f' ~ appendSB('\f')
				| 'n' ~ appendSB('\n')
				| 'r' ~ appendSB('\r')
				| 't' ~ appendSB('\t')
				| Unicode ~> { code => sb.append(code.asInstanceOf[Char]); () }
			)

			def Unicode = rule {'u' ~ capture(HexDigit ~ HexDigit ~ HexDigit ~ HexDigit) ~> (java.lang.Integer.parseInt(_, 16))}
			def Integer: Rule0 = rule {optional('-') ~ (Digit19 ~ Digits | Digit)}
			def Digits: Rule0 = rule {oneOrMore(Digit)}
			def Frac: Rule0 = rule {"." ~ Digits}
			def Exp: Rule0 = rule {ignoreCase('e') ~ optional(anyOf("+-")) ~ Digits}

			def JsonTrue: Rule1[Bool] = rule {"true" ~ WhiteSpace ~ push(Bool(true))}
			def JsonFalse: Rule1[Bool] = rule {"false" ~ WhiteSpace ~ push(Bool(false))}
			def JsonNull: Rule1[Null.type] = rule {"null" ~ WhiteSpace ~ push(Null)}

			def WhiteSpace: Rule0 = rule {zeroOrMore(WhiteSpaceChar)}

			def ws(c: Char): Rule0 = rule {c ~ WhiteSpace}

			val WhiteSpaceChar      = CharPredicate(" \n\r\t\f")
			val QuoteBackslash      = CharPredicate("\"\\")
			val QuoteSlashBackSlash = QuoteBackslash ++ "/"
		}

		import org.parboiled2.Parser.DeliveryScheme.Either

		// XXX cold/hot doesn't make a lot of sense here
		final def _parser: String => Either[String, JsonExpr] = parser()
		final def parser(): String => Either[String, JsonExpr] = { s: String =>
			val parser = new PB2Parser(s)
			parser.full.run().left.map {parser.formatError(_)}
		}

	}

}
