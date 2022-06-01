package io.getquill.dsl

import io.getquill.Spec
import io.getquill.testContext._
import io.getquill.context.mirror.{ MirrorSession, Row }
import io.getquill.Query
import io.getquill.util.PrintMac

class MetaDslSpec extends Spec {

  case class MoreThan22(
    v0: Int, v1: Int, v2: Int, v3: Int, v4: Int, v5: Int, v6: Int, v7: Int, v8: Int, v9: Int,
    x0: Int, x1: Int, x2: Int, x3: Int, x4: Int, x5: Int, x6: Int, x7: Int, x8: Int, x9: Int,
    y0: Int, y1: Int, y2: Int, y3: Int, y4: Int, y5: Int, y6: Int, y7: Int, y8: Int, y9: Int
  )

  case class EmbValue(i: Int) extends Embedded

  "schema meta" - {
    "materialized" in {
      val meta = materializeSchemaMeta[TestEntity]
      meta.entity.toString mustEqual """querySchema("TestEntity")"""
    }
    "custom" in {
      val meta = schemaMeta[TestEntity]("test_entity", _.i -> "ii")
      meta.entity.toString mustEqual """`querySchema`("test_entity", _.i -> "ii")"""
    }
    "custom with embedded" in {
      case class Entity(emb: EmbValue)
      val meta = schemaMeta[Entity]("test_entity", _.emb.i -> "ii")
      meta.entity.toString mustEqual """`querySchema`("test_entity", _.emb.i -> "ii")"""
    }
    "custom with optional embedded" in {
      case class Entity(emb: Option[EmbValue])
      val meta = schemaMeta[Entity]("test_entity", _.emb.map(_.i) -> "ii")
      meta.entity.toString mustEqual """`querySchema`("test_entity", _.emb.i -> "ii")"""
    }
  }

  "query meta" - {
    "materialized" - {
      "simple" in {
        case class Entity(a: String, b: Int)
        val meta = materializeQueryMeta[Entity]
        meta.extract(Row("1", 2), MirrorSession.default) mustEqual Entity("1", 2)
      }
      "with embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        case class Entity(a: String, b: Nested)
        val meta = materializeQueryMeta[Entity]
        meta.extract(Row("1", 2, 3L), MirrorSession.default) mustEqual Entity("1", Nested(2, 3L))
      }
      "tuple" in {
        val meta = materializeQueryMeta[(String, Int)]
        meta.extract(Row("1", 2), MirrorSession.default) mustEqual (("1", 2))
      }
      "tuple + embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        val meta = materializeQueryMeta[(String, Nested)]
        meta.extract(Row("1", 2, 3L), MirrorSession.default) mustEqual (("1", Nested(2, 3L)))
      }
      "tuple + nested embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        case class Entity(a: String, b: Nested)
        val meta = materializeQueryMeta[(String, Entity)]
        meta.extract(Row("a", "1", 2, 3L), MirrorSession.default) mustEqual (("a", Entity("1", Nested(2, 3L))))
      }
      "optional nested" - {
        "extracts Some if all columns are defined" in {
          case class Entity(a: String, b: Int)
          val meta = materializeQueryMeta[(String, Option[Entity])]
          meta.extract(Row("a", "1", 2), MirrorSession.default) mustEqual (("a", Some(Entity("1", 2))))
        }
        "extracts Some if at least one column is defined" in {
          case class Entity(a: String, b: Int)
          val meta = materializeQueryMeta[(String, Option[Entity])]
          meta.extract(Row("a", "1", null), MirrorSession.default) mustEqual (("a", Some(Entity("1", 0))))
        }
        "extracts Some if at least one column is defined - alternate" in {
          case class Entity(a: String, b: Int)
          val meta = materializeQueryMeta[(String, Option[Entity])]
          meta.extract(Row("a", null, 2), MirrorSession.default) mustEqual (("a", Some(Entity(null, 2))))
        }
        "extracts None if no columns are defined" in {
          case class Entity(a: String, b: Int)
          val meta = materializeQueryMeta[(String, Option[Entity])]
          meta.extract(Row("a", null, null), MirrorSession.default) mustEqual (("a", None))
        }
      }
      "optional deep nested" - {
        case class Entity1(a: String, b: Int)
        case class Entity2(a: Option[Int])
        val meta = materializeQueryMeta[(String, Option[(Entity1, Entity2)])]

        "extracts Some if all columns are defined" in {
          meta.extract(Row("a", "1", 2, 3), MirrorSession.default) mustEqual
            (("a", Some((Entity1("1", 2), Entity2(Some(3))))))
        }
        "extracts Some if optional column is undefined" in {
          meta.extract(Row("a", "1", 2, null), MirrorSession.default) mustEqual
            (("a", Some((Entity1("1", 2), Entity2(None)))))
        }
        "extracts Some at least one column is defined" in {
          meta.extract(Row("a", "1", null, 3), MirrorSession.default) mustEqual
            (("a", Some((Entity1("1", 0), Entity2(Some(3))))))
        }
        "extracts None if no columns are defined" in {
          meta.extract(Row("a", null, null, null), MirrorSession.default) mustEqual
            (("a", None))
        }
      }
      "> 22 fields" in {
        val meta = materializeQueryMeta[MoreThan22]
        meta.extract(Row(0 until 30: _*), MirrorSession.default) mustEqual
          MoreThan22(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29)
      }
    }
    "custom" in {
      case class Person(id: Int, name: String, age: Int, phone: String)
      case class Contact(personId: Int, phone: String)
      implicit val meta =
        queryMeta(
          (q: Query[Person]) =>
            for {
              t <- q
              c <- query[Contact] if c.personId == t.id
            } yield {
              (t.id, t.name, t.age, c.phone)
            }
        )((Person.apply _).tupled)

      meta.expand.toString mustEqual """(q) => q.flatMap(t => querySchema("Contact").filter(c => c.personId == t.id).map(c => (t.id, t.name, t.age, c.phone)))"""
      meta.extract(Row(1, "a", 2, "b"), MirrorSession.default) mustEqual Person(1, "a", 2, "b")
    }
  }

  "update meta" - {
    "materialized" - {
      "simple" in {
        case class Entity(a: String, b: Int)
        val meta = materializeUpdateMeta[Entity]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v.a -> value.a, v => v.b -> value.b)"
      }
      "with embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        case class Entity(a: String, b: Nested)
        val meta = materializeUpdateMeta[Entity]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v.a -> value.a, v => v.b.i -> value.b.i, v => v.b.l -> value.b.l)"
      }
      "tuple" in {
        val meta = materializeUpdateMeta[(String, Int)]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v._1 -> value._1, v => v._2 -> value._2)"
      }
      "tuple + embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        val meta = materializeUpdateMeta[(String, Nested)]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v._1 -> value._1, v => v._2.i -> value._2.i, v => v._2.l -> value._2.l)"
      }
      "tuple + nested embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        case class Entity(a: String, b: Nested)
        val meta = materializeUpdateMeta[(String, Entity)]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v._1 -> value._1, v => v._2.a -> value._2.a, v => v._2.b.i -> value._2.b.i, v => v._2.b.l -> value._2.b.l)"
      }
      "optional nested" in {
        case class Entity(a: String, b: Int)
        val meta = materializeUpdateMeta[(String, Option[Entity])]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v._1 -> value._1, v => v._2.map((v) => v.a) -> value._2.map((v) => v.a), v => v._2.map((v) => v.b) -> value._2.map((v) => v.b))"
      }
      "> 22 fields" in {
        val meta = materializeUpdateMeta[MoreThan22]
        meta.expand.toString mustEqual "(q, value) => q.update(v => v.v0 -> value.v0, v => v.v1 -> value.v1, v => v.v2 -> value.v2, v => v.v3 -> value.v3, v => v.v4 -> value.v4, v => v.v5 -> value.v5, v => v.v6 -> value.v6, v => v.v7 -> value.v7, v => v.v8 -> value.v8, v => v.v9 -> value.v9, v => v.x0 -> value.x0, v => v.x1 -> value.x1, v => v.x2 -> value.x2, v => v.x3 -> value.x3, v => v.x4 -> value.x4, v => v.x5 -> value.x5, v => v.x6 -> value.x6, v => v.x7 -> value.x7, v => v.x8 -> value.x8, v => v.x9 -> value.x9, v => v.y0 -> value.y0, v => v.y1 -> value.y1, v => v.y2 -> value.y2, v => v.y3 -> value.y3, v => v.y4 -> value.y4, v => v.y5 -> value.y5, v => v.y6 -> value.y6, v => v.y7 -> value.y7, v => v.y8 -> value.y8, v => v.y9 -> value.y9)"
      }
    }
    "custom" - {
      case class Nested(i: Int, l: Long) extends Embedded
      case class Entity(a: String, b: Nested, c: Option[Nested])

      "exclude column" in {
        val meta = updateMeta[Entity](_.a)
        meta.expand.toString mustEqual
          "(q, value) => q.update(v => v.b.i -> value.b.i, v => v.b.l -> value.b.l, v => v.c.map((v) => v.i) -> value.c.map((v) => v.i), v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
      "exclude embedded" in {
        val meta = updateMeta[Entity](_.b)
        meta.expand.toString mustEqual
          "(q, value) => q.update(v => v.a -> value.a, v => v.c.map((v) => v.i) -> value.c.map((v) => v.i), v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
      "exclude nested column" in {
        val meta = updateMeta[Entity](_.b.i)
        meta.expand.toString mustEqual
          "(q, value) => q.update(v => v.a -> value.a, v => v.b.l -> value.b.l, v => v.c.map((v) => v.i) -> value.c.map((v) => v.i), v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
      "exclude option embedded" in {
        val meta = updateMeta[Entity](_.c)
        meta.expand.toString mustEqual
          "(q, value) => q.update(v => v.a -> value.a, v => v.b.i -> value.b.i, v => v.b.l -> value.b.l)"
      }
      "exclude option nested column" in {
        val meta = updateMeta[Entity](_.c.map(_.i))
        meta.expand.toString mustEqual
          "(q, value) => q.update(v => v.a -> value.a, v => v.b.i -> value.b.i, v => v.b.l -> value.b.l, v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
    }
  }

  "insert meta" - {
    "materialized" - {
      "simple" in {
        case class Entity(a: String, b: Int)
        val meta = materializeInsertMeta[Entity]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v.a -> value.a, v => v.b -> value.b)"
      }
      "with embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        case class Entity(a: String, b: Nested)
        val meta = materializeInsertMeta[Entity]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v.a -> value.a, v => v.b.i -> value.b.i, v => v.b.l -> value.b.l)"
      }
      "tuple" in {
        val meta = materializeInsertMeta[(String, Int)]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v._1 -> value._1, v => v._2 -> value._2)"
      }
      "tuple + embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        val meta = materializeInsertMeta[(String, Nested)]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v._1 -> value._1, v => v._2.i -> value._2.i, v => v._2.l -> value._2.l)"
      }
      "tuple + nested embedded" in {
        case class Nested(i: Int, l: Long) extends Embedded
        case class Entity(a: String, b: Nested)
        val meta = materializeInsertMeta[(String, Entity)]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v._1 -> value._1, v => v._2.a -> value._2.a, v => v._2.b.i -> value._2.b.i, v => v._2.b.l -> value._2.b.l)"
      }
      "optional nested" in {
        case class Entity(a: String, b: Int)
        val meta = materializeInsertMeta[(String, Option[Entity])]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v._1 -> value._1, v => v._2.map((v) => v.a) -> value._2.map((v) => v.a), v => v._2.map((v) => v.b) -> value._2.map((v) => v.b))"
      }
      "> 22 fields" in {
        val meta = materializeInsertMeta[MoreThan22]
        meta.expand.toString mustEqual "(q, value) => q.insert(v => v.v0 -> value.v0, v => v.v1 -> value.v1, v => v.v2 -> value.v2, v => v.v3 -> value.v3, v => v.v4 -> value.v4, v => v.v5 -> value.v5, v => v.v6 -> value.v6, v => v.v7 -> value.v7, v => v.v8 -> value.v8, v => v.v9 -> value.v9, v => v.x0 -> value.x0, v => v.x1 -> value.x1, v => v.x2 -> value.x2, v => v.x3 -> value.x3, v => v.x4 -> value.x4, v => v.x5 -> value.x5, v => v.x6 -> value.x6, v => v.x7 -> value.x7, v => v.x8 -> value.x8, v => v.x9 -> value.x9, v => v.y0 -> value.y0, v => v.y1 -> value.y1, v => v.y2 -> value.y2, v => v.y3 -> value.y3, v => v.y4 -> value.y4, v => v.y5 -> value.y5, v => v.y6 -> value.y6, v => v.y7 -> value.y7, v => v.y8 -> value.y8, v => v.y9 -> value.y9)"
      }
    }
    "custom" - {
      case class Nested(i: Int, l: Long) extends Embedded
      case class Entity(a: String, b: Nested, c: Option[Nested])

      "exclude column" in {
        val meta = insertMeta[Entity](_.a)
        meta.expand.toString mustEqual
          "(q, value) => q.insert(v => v.b.i -> value.b.i, v => v.b.l -> value.b.l, v => v.c.map((v) => v.i) -> value.c.map((v) => v.i), v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
      "exclude embedded" in {
        val meta = insertMeta[Entity](_.b)
        meta.expand.toString mustEqual
          "(q, value) => q.insert(v => v.a -> value.a, v => v.c.map((v) => v.i) -> value.c.map((v) => v.i), v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
      "exclude nested column" in {
        val meta = insertMeta[Entity](_.b.i)
        meta.expand.toString mustEqual
          "(q, value) => q.insert(v => v.a -> value.a, v => v.b.l -> value.b.l, v => v.c.map((v) => v.i) -> value.c.map((v) => v.i), v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
      "exclude option embedded" in {
        val meta = insertMeta[Entity](_.c)
        meta.expand.toString mustEqual
          "(q, value) => q.insert(v => v.a -> value.a, v => v.b.i -> value.b.i, v => v.b.l -> value.b.l)"
      }
      "exclude option nested column" in {
        val meta = insertMeta[Entity](_.c.map(_.i))
        meta.expand.toString mustEqual
          "(q, value) => q.insert(v => v.a -> value.a, v => v.b.i -> value.b.i, v => v.b.l -> value.b.l, v => v.c.map((v) => v.l) -> value.c.map((v) => v.l))"
      }
    }
  }
}