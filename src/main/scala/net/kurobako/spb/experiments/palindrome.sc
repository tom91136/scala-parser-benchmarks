
import fastparse.{all, core}
import fastparse.all.{CharIn, P, Parser}
import fastparse.all._

import scala.annotation.tailrec
import scala.collection.immutable

val letters = 'a' to 'z'
val example = (letters ++ "a" ++ letters.reverse).mkString


val letter: Parser[String] = P(CharIn('a' to 'z')).!



def palindrome(): Parser[String] = {
	def rest(x: Parser[String]): Parser[String] = {
		val ps = (for {
			l <- (x.!.log("l"))
			r <- rest(x).log("c") ~ P(l).!.log("r")
		} yield l + r) | (x.log("pivot"))
		ps.!
	}


	Start ~ rest(letter) ~ End
}


private val good = Seq("a", "aaa", "aaaaa", "abbbbba", "bbbbb", "abcba", "abcdcba")
private val bad = Seq("", "aa", "aaaa")
good.foreach {
	x =>
		println(s"$x -> ${
			palindrome().parse(x)
		}\n")

}

bad.foreach {
	x =>
		println(s"$x -> ${
			palindrome().parse(x)
		}\n")

}


