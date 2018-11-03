package net.kurobako.spb.json

import net.kurobako.spb
import net.kurobako.spb.Collectors.Result
import net.kurobako.spb.json.Json.JsonExpr
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{EitherValues, FlatSpec, Matchers}

import scala.io.Source

class JsonSpec extends FlatSpec with Matchers with EitherValues {


	"all parsing def" should "give the same result" in {
		val bf = new Json
		forAll(Table(
			"def",
			bf.warmScalaParserCombinator _,
			bf.warmAtto _,
			bf.warmParsley _,
			bf.warmFastParse _,
			bf.warmParboiled2 _,
			bf.coldScalaParserCombinator _,
			bf.coldAtto _,
			bf.coldParsley _,
			bf.coldFastParse _,
			bf.coldParboiled2 _,
		)) { method =>

			forAll(Table(
				("file", "expected"),
				("json/arr0.json", "json/arr0.txt"),
				("json/arr_nums.json", "json/arr_nums.txt"),
				("json/arr_strs.json", "json/arr_strs.txt"),
				("json/mixed.json", "json/mixed.txt"),
				("json/obj0.json", "json/obj0.txt"),
				("json/obj1.json", "json/obj1.txt"),
				("json/obj2.json", "json/obj2.txt"),
//				("json/medium.json", "json/medium.txt"),
				//				("json/long.json", ""),
			)) { (file: String, expected: String) =>
				val context = new spb.json.Json.Context()
				context.file = file

				context.setup()

				val actual: Result[JsonExpr] = method(context)

				val expectedString = Source.fromResource(expected).mkString

				actual.map { c => c.toString }.right.value shouldBe expectedString


			}
		}
	}

}
