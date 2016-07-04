package io.getquill

class QueryProbingSpec extends Spec {

  "fails if the context can't be resolved at compile time" in {
    object c extends MirrorContext with QueryProbing
    "c.run(qr1)" mustNot compile
  }

  "doesn't warn if query probing is disabled and the context can't be resolved at compile time" in {
    object c extends MirrorContext with TestEntities
    import c._
    c.run(qr1.delete)
    ()
  }

  "fails compilation if the query probing fails" - {
    case class Fail()
    "object context" in {
      import mirrorWithQueryProbing._
      "mirrorWithQueryProbing.run(query[Fail].delete)" mustNot compile
    }
    "class context" in {
      def test(s: MirrorContextWithQueryProbing) = {
        import s._
        "s.run(query[Fail].delete)" mustNot compile
      }
    }
    "singleton type" in {
      val ctx = new MirrorContextWithQueryProbing
      import ctx._
      "ctx.run(query[Fail].delete)" mustNot compile
    }
  }
}
