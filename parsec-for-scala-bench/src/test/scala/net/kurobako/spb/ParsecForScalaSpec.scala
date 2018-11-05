package net.kurobako.spb

import net.kurobako.spb.ParsecForScalaBench.SimpleBench
import net.kurobako.spb.fixture.Simple.SimpleContext
import net.kurobako.spb.fixture.Simple.SimpleSpec.{Expected, TestString}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{EitherValues, FlatSpec, Matchers}


class ParsecForScalaSpec extends FlatSpec with Matchers with EitherValues {


	"simple" should "parse according to spec" in {
		val simple = new SimpleBench
		forAll(Table(
			("def", "input", "expected"),
			(simple.warmParsecForScalaChainL _, TestString, Expected),
			(simple.coldParsecForScalaChainL _, TestString, Expected),
		)) { (method, input, expected) =>
			val context = new SimpleContext
			context.token = TestString
			method(context).right.value shouldBe expected
		}


	}


}

