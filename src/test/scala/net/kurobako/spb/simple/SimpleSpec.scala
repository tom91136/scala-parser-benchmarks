package net.kurobako.spb.simple

import net.kurobako.spb
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class SimpleSpec extends FlatSpec with Matchers with EitherValues {


	final val TestChar   = '1'
	final val RepC       = 17
	final val TestString = List.fill(RepC) {TestChar}.mkString("+")
	final val Expected   = TestChar * RepC


	"all parsing def" should "give the same result" in {
		val simple = new Simple
		forAll(Table(
			("def", "input", "expected"),
			(simple.baselineTailRecursiveTry _, TestString, Expected),
			(simple.baselineRecursiveDecent _, TestString, Expected),
			(simple.warmScalaParserCombinatorChainL _, TestString, Expected),
			(simple.warmAttoChainL _, TestString, Expected),
			(simple.warmParsecForScalaChainL _, TestString, Expected),
			(simple.warmMeerkatBnf _, TestString, Expected),
			(simple.warmParsleyChainL _, TestString, Expected),
			(simple.warmFastParseBindFoldCurried _, TestString, Expected),
			(simple.warmFastParseBindFoldTuple2 _, TestString, Expected),
			(simple.warmFastParseBindRecursiveCurried _, TestString, Expected),
			(simple.warmFastParseBindRecursiveTuple2 _, TestString, Expected),
			(simple.warmFastParseRightRecursive _, TestString, Expected),

			(simple.coldScalaParserCombinatorChainL _, TestString, Expected),
			(simple.coldAttoChainL _, TestString, Expected),
			(simple.coldParsecForScalaChainL _, TestString, Expected),
			(simple.coldMeerkatBnf _, TestString, Expected),
			(simple.coldParsleyChainL _, TestString, Expected),
			(simple.coldFastParseBindFoldCurried _, TestString, Expected),
			(simple.coldFastParseBindFoldTuple2 _, TestString, Expected),
			(simple.coldFastParseBindRecursiveCurried _, TestString, Expected),
			(simple.coldFastParseBindRecursiveTuple2 _, TestString, Expected),
			(simple.coldFastParseRightRecursive _, TestString, Expected)
		)) { (method, input, expected) =>
			val context = new spb.simple.Simple.Context()
			context.token = TestString
			val actual = method(context)
			actual.right.value shouldBe expected
		}


	}

}