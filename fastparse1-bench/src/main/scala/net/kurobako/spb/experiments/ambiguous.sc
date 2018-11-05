import fastparse.all.{P, _}


val b = P("b")
val l1 = (b.! ~ b.!).map { case (l, r) => l + r }
val l2 = (l1.! ~ l1.! ~ l1.!).map { case (l, c, r) => l + c + r }

val p = Start ~ (b | l1 | l2) ~ End

p.parse("bbbbbbbbbb")
