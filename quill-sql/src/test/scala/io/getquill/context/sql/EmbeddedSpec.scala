package io.getquill.context.sql

import io.getquill._

class EmbeddedSpec extends Spec {

  val ctx = new SqlMirrorContext(MirrorSqlDialect, Literal) with TestEntities
  import ctx._

  "queries with embedded entities should" - {
    "function property inside of nested distinct queries" in {
      case class Parent(id: Int, emb1: Emb)
      case class Emb(a: Int, b: Int) extends Embedded
      val q = quote {
        query[Emb].map(e => Parent(1, e)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT 1, e.a, e.b FROM Emb e"
    }

    "function property inside of nested distinct queries - tuple" in {
      case class Parent(id: Int, emb1: Emb)
      case class Emb(a: Int, b: Int) extends Embedded
      val q = quote {
        query[Emb].map(e => Parent(1, e)).distinct.map(p => (2, p)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT 2, e.id, e.emb1a, e.emb1b FROM (SELECT DISTINCT 1 AS id, e.a AS emb1a, e.b AS emb1b FROM Emb e) AS e"
    }

    "function property inside of nested distinct queries through tuples" in {
      case class Parent(id: Int, emb1: Emb)
      case class Emb(a: Int, b: Int) extends Embedded
      val q = quote {
        query[Emb].map(e => (1, e)).distinct.map(t => Parent(t._1, t._2)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT e._1, e._2a, e._2b FROM (SELECT DISTINCT 1 AS _1, e.a AS _2a, e.b AS _2b FROM Emb e) AS e"
    }

    "function property inside of nested distinct queries - twice" in {
      case class Grandparent(idG: Int, par: Parent)
      case class Parent(idP: Int, emb1: Emb) extends Embedded
      case class Emb(a: Int, b: Int) extends Embedded
      val q = quote {
        query[Emb].map(e => Parent(1, e)).distinct.map(p => Grandparent(2, p)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT 2, e.idP, e.emb1a, e.emb1b FROM (SELECT DISTINCT 1 AS idP, e.a AS emb1a, e.b AS emb1b FROM Emb e) AS e"
    }

    "function property inside of nested distinct queries - twice - into tuple" in {
      case class Grandparent(idG: Int, par: Parent)
      case class Parent(idP: Int, emb1: Emb) extends Embedded
      case class Emb(a: Int, b: Int) extends Embedded
      val q = quote {
        query[Emb].map(e => Parent(1, e)).distinct.map(p => Grandparent(2, p)).distinct.map(g => (3, g)).distinct
      }
      ctx.run(q).string mustEqual "SELECT DISTINCT 3, p.idG, p.paridP, p.paremb1a, p.paremb1b FROM (SELECT DISTINCT 2 AS idG, p.idP AS paridP, p.emb1a AS paremb1a, p.emb1b AS paremb1b FROM (SELECT DISTINCT 1 AS idP, e.a AS emb1a, e.b AS emb1b FROM Emb e) AS p) AS p"
    }
  }

}
