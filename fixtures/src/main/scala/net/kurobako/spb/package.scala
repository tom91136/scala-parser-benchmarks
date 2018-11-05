package net.kurobako

import scala.io.Source
import scala.util.Either

package object spb {

	// some scripts output CRLF so we clean that up
	def normaliseLn(s: String): String = s.replaceAll("\r\n", "\n")

	// read file to string
	def readFully(resource: String): String = Source.fromResource(resource).mkString


	trait BenchSpec

	trait BenchProvider {
		def classes: Seq[Class[_ <: BenchSpec]]
	}

	type Result[A] = Either[String, A]

	// XXX this has to be as optimised as possible hence the abomination
	@inline def !![A](r: Result[A]): Result[A] = {
		if (r.isLeft) throw new AssertionError(r.left.get)
		r
	}


}
