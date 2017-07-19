package io.getquill

class QueryProbingSpec extends Spec {

  "fails if the context can't be resolved at compile time" in {
    object c extends MirrorContext[MirrorIdiom, Literal] with TestEntities with QueryProbing
    import c._
    "c.run(qr1)" mustNot compile
  }

  "doesn't warn if query probing is disabled and the context can't be resolved at compile time" in {
    object c extends MirrorContext[MirrorIdiom, Literal] with TestEntities
    import c._
    c.run(qr1.delete)
    ()
  }

  "fails compilation if the query probing fails" - {
    case class Fail()
    "object context" in {
      val ctx = new MirrorContextWithQueryProbing[MirrorIdiom, Literal]
      "mirrorWithQueryProbing.run(query[Fail].delete)" mustNot compile
    }
    "class context" in {
      def test(s: MirrorContextWithQueryProbing[MirrorIdiom, Literal]) = {
        import s._
        "s.run(query[Fail].delete)" mustNot compile
      }
    }
    "singleton type" in {
      val ctx = new MirrorContextWithQueryProbing[MirrorIdiom, Literal]
      import ctx._
      "ctx.run(query[Fail].delete)" mustNot compile
    }
  }
}
