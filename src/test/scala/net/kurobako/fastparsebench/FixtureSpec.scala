package net.kurobako.fastparsebench

import org.scalatest.{EitherValues, FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks._

class FixtureSpec extends FlatSpec with Matchers with EitherValues {


	final val TestChar   = '1'
	final val RepC       = 17
	final val TestString = List.fill(RepC) {TestChar}.mkString("+")
	final val Expected   = TestChar * RepC

	"all parsing def" should "give the same result" in {
		forAll(Table(
			("def", "input", "expected"),
			(Fixtures.parseParsecForScalaChainL1 _, TestString, Expected),
			(Fixtures.parseBindFoldCurried _, TestString, Expected),
			(Fixtures.parseBindFoldTuple2 _, TestString, Expected),
			(Fixtures.parseBindRecursiveTuple2 _, TestString, Expected),
			(Fixtures.parseBindRecursiveCurried _, TestString, Expected),
			(Fixtures.parseRightRecursive _, TestString, Expected),
			(Fixtures.parseHandRollRD _, TestString, Expected),
		)) { (method, input, expected) =>
			val actual = method(input)
			actual.right.value shouldBe expected
		}

	}

}