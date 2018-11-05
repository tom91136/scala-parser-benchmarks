package net.kurobako.spb

import net.kurobako.spb.fixture.BrainFuck.{BrainFuckContext, _}
import net.kurobako.spb.fixture.Simple.{SimpleBenchSpec, SimpleContext}
import org.openjdk.jmh.annotations.Benchmark

import scala.annotation.switch

object MeerkatBench extends BenchProvider {

	override val classes: Seq[Class[_ <: BenchSpec]] = Seq(classOf[SimpleBench], classOf[BrainFuckBench])

	@inline final def collectMeerkat[A](parser: org.meerkat.parsers.&[org.meerkat.parsers.Parsers.Nonterminal, A],
										input: String): Result[A] = {
		import org.meerkat.parsers.exec
		exec(parser, input).left.map {_.toString}
	}

	class SimpleBench extends SimpleBenchSpec {
		@Benchmark def warmMeerkatBnf(ctx: SimpleContext): Result[Int] = !!(collectMeerkat(Simple._bnf, ctx.token))
		@Benchmark def coldMeerkatBnf(ctx: SimpleContext): Result[Int] = !!(collectMeerkat(Simple.bnf(), ctx.token))
	}

	class BrainFuckBench extends BrainFuckBenchSpec {
		@Benchmark def warmMeerkat(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collectMeerkat(BrainFuck._parser, ctx.input))
		@Benchmark def coldMeerkat(ctx: BrainFuckContext): Result[List[BrainFuckOp]] = !!(collectMeerkat(BrainFuck.parser(), ctx.input))
	}

	object Simple {
		import org.meerkat.Syntax._
		import org.meerkat.parsers.Parsers._
		import org.meerkat.parsers._
		final val _bnf = bnf()
		@inline final def bnf(): Nonterminal & Int = {
			/*_*/
			// XXX so we can't do String.apply(Int) because it got shadowed
			val One = syn {"1" ^ { x: String => x.charAt(0).toInt }}
			//XXX need to improve this
			lazy val P: Nonterminal & Int = syn(P ~ "+" ~ One & { case a ~ b => a + b } | One)
			/*_*/
			P
		}
	 
	}

	object BrainFuck {
		import org.meerkat.Syntax._
		import org.meerkat.parsers.Parsers._
		import org.meerkat.parsers._
		import org.meerkat.sppf.SPPFLookup
		import org.meerkat.tree.TerminalSymbol

		@inline private def fastMatch(c: Char): Boolean = (c: @switch) match {
			case '>' | '<' | '+' | '-' | '.' | ',' | '[' | ']' => true
			case _                                             => false
		}

		final val _parser = parser()
		@inline final def parser(): Nonterminal & List[BrainFuckOp] = {

			implicit val L: Layout = layout(new Terminal {

				import org.meerkat.util.Input

				def apply(input: Input, i: Int, sppfLookup: SPPFLookup) = {
					val len = input.length
					if (i >= len) CPSResult.success(sppfLookup.getTerminalNode(name, i, i))
					else if (fastMatch(input.charAt(i))) CPSResult.success(sppfLookup.getTerminalNode(name, i, i))
					else {
						val end = input.s.indexWhere(c => fastMatch(c), i)
						if (end == -1) CPSResult.success(sppfLookup.getTerminalNode(name, len, len))
						else CPSResult.success(sppfLookup.getTerminalNode(name, i, end))
					}
				}
				def name = "<CTRL chars>"
				def symbol = TerminalSymbol(name)
			})

			/*_*/
			lazy val P: Nonterminal & List[BrainFuckOp] = syn(
				">" ^ { _ => RightPointer }
				| "<" ^ { _ => LeftPointer }
				| "+" ^ { _ => Increment }
				| "-" ^ { _ => Decrement }
				| "." ^ { _ => Output }
				| "," ^ { _ => Input }
				| ("[" ~ P ~ "]") & {Loop(_)}
				// XXX  need this instead of just doing * at the end otherwise it stackoverflows or 
				// throws a CCE during the application of `&`, as seen at 
				// `org/meerkat/parsers/Parsers.scala` where asInstanceOf[T] is used
				| ("[" ~ "]") ^ { _ => Loop(Nil) }
			).+
			/*_*/
			start(P)
		}
	}


}