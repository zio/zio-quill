package io.getquill.quotation

import io.getquill._
import io.getquill.dsl.DynamicQueryDsl

class CompatibleDynamicQuerySpec extends Spec {

  object testContext extends MirrorContext(MirrorIdiom, Literal) with TestEntities with DynamicQueryDsl
  import testContext._

  "implicit classes" - {
    "query" in {
      val q: Quoted[Query[TestEntity]] = qr1
      val d = {
        val d = q.dynamic
        (d: DynamicQuery[TestEntity])
      }
      d.q mustEqual q
    }
    "entity query" in {
      val q: Quoted[EntityQuery[TestEntity]] = qr1
      val d = {
        val d = q.dynamic
        (d: DynamicEntityQuery[TestEntity])
      }
      d.q mustEqual q
    }
    "action" in {
      val q: Quoted[Action[TestEntity]] = qr1.insert(_.i -> 1)
      val d = {
        val d = q.dynamic
        (d: DynamicAction[Action[TestEntity]])
      }
      d.q mustEqual q
    }
    "insert" in {
      val q: Quoted[Insert[TestEntity]] = qr1.insert(_.i -> 1)
      val d = {
        val d = q.dynamic
        (d: DynamicInsert[TestEntity])
      }
      d.q mustEqual q
    }
    "update" in {
      val q: Quoted[Update[TestEntity]] = qr1.update(_.i -> 1)
      val d = {
        val d = q.dynamic
        (d: DynamicUpdate[TestEntity])
      }
      d.q mustEqual q
    }
    "action returning" in {
      val q: Quoted[ActionReturning[TestEntity, Int]] = quote {
        qr1.insert(_.i -> 1).returningGenerated(_.i)
      }
      val d = {
        val d = q.dynamic
        (d: DynamicActionReturning[TestEntity, Int])
      }
      d.q mustEqual q
    }
  }

  // Need to put here so an summon TypeTag for these
  case class S(v: String) extends Embedded
  case class E(s: S)

  "query" - {

    def test[T: QueryMeta](d: Quoted[Query[T]], s: Quoted[Query[T]]) =
      testContext.run(d).string mustEqual testContext.run(s).string

    "dynamicQuery" in {
      test(
        quote(query[TestEntity]).dynamic,
        query[TestEntity]
      )
    }

    "map" - {
      "simple" in {
        test(
          quote(query[TestEntity]).dynamic.map(v0 => quote(v0.i)),
          query[TestEntity].map(v0 => v0.i)
        )
      }
      "dynamic" in {
        var cond = true
        test(
          quote(query[TestEntity]).dynamic.map(v0 => if (cond) quote(v0.i) else quote(1)),
          query[TestEntity].map(v0 => v0.i)
        )

        cond = false
        test(
          quote(query[TestEntity]).dynamic.map(v0 => if (cond) quote(v0.i) else quote(1)),
          query[TestEntity].map(v0 => 1)
        )
      }
    }

    "flatMap" - {
      "simple" in {
        test(
          quote(query[TestEntity]).dynamic.flatMap(v0 => quote(query[TestEntity]).dynamic),
          query[TestEntity].flatMap(v0 => query[TestEntity])
        )
      }
      "mixed with static" in {
        test(
          quote(query[TestEntity]).dynamic.flatMap(v0 => query[TestEntity]),
          query[TestEntity].flatMap(v0 => query[TestEntity])
        )

        test(
          query[TestEntity].flatMap(v0 => quote(query[TestEntity]).dynamic),
          query[TestEntity].flatMap(v0 => query[TestEntity])
        )
      }
      "with map" in {
        test(
          quote(query[TestEntity]).dynamic.flatMap(v0 => quote(query[TestEntity]).dynamic.map(v1 => quote((unquote(v0), unquote(v1))))),
          query[TestEntity].flatMap(v0 => query[TestEntity].map(v1 => (v0, v1)))
        )
      }
      "for comprehension" in {
        test(
          for {
            v0 <- quote(query[TestEntity]).dynamic
            v1 <- quote(query[TestEntity]).dynamic
          } yield (unquote(v0), unquote(v1)),
          for {
            v0 <- query[TestEntity]
            v1 <- query[TestEntity]
          } yield (v0, v1)
        )
      }
    }

    "filter" in {
      test(
        quote(query[TestEntity]).dynamic.filter(v0 => quote(v0.i == 1)),
        query[TestEntity].filter(v0 => v0.i == 1)
      )
    }

    "withFilter" in {
      test(
        quote(query[TestEntity]).dynamic.withFilter(v0 => quote(v0.i == 1)),
        query[TestEntity].withFilter(v0 => v0.i == 1)
      )
    }

    "filterOpt" - {
      "defined" in {
        val o = Some(1)
        test(
          quote(query[TestEntity]).dynamic.filterOpt(o)((v0, i) => quote(v0.i == i)),
          query[TestEntity].filter(v0 => v0.i == lift(1))
        )
      }
      "empty" in {
        val o: Option[Int] = None
        test(
          quote(query[TestEntity]).dynamic.filterOpt(o)((v0, i) => quote(v0.i == i)),
          query[TestEntity]
        )
      }
    }

    "filterIf" - {
      "true" in {
        val ids = Seq(1)
        test(
          quote(query[TestEntity]).dynamic.filterIf(ids.nonEmpty)(v0 => quote(liftQuery(ids).contains(v0.i))),
          query[TestEntity].filter(v0 => quote(liftQuery(ids).contains(v0.i)))
        )
      }
      "false" in {
        val ids = Seq.empty[Int]
        test(
          quote(query[TestEntity]).dynamic.filterIf(ids.nonEmpty)(v0 => quote(liftQuery(ids).contains(v0.i))),
          query[TestEntity]
        )
      }
    }

    "concatMap" in {
      test(
        quote(query[TestEntity]).dynamic.concatMap[String, Array[String]](v0 => quote(v0.s.split(" "))),
        query[TestEntity].concatMap[String, Array[String]](v0 => v0.s.split(" "))
      )
    }

    "sortBy" in {
      val o = Ord.desc[Int]
      test(
        quote(query[TestEntity]).dynamic.sortBy(v0 => quote(v0.i))(o),
        query[TestEntity].sortBy(v0 => v0.i)(Ord.desc)
      )
    }

    "take" - {
      "quoted" in {
        test(
          quote(query[TestEntity]).dynamic.take(quote(1)),
          query[TestEntity].take(1)
        )
      }

      "int" in {
        test(
          quote(query[TestEntity]).dynamic.take(1),
          query[TestEntity].take(lift(1))
        )
      }

      "opt" - {
        "defined" in {
          test(
            quote(query[TestEntity]).dynamic.takeOpt(Some(1)),
            query[TestEntity].take(lift(1))
          )
        }
        "empty" in {
          test(
            quote(query[TestEntity]).dynamic.takeOpt(None),
            query[TestEntity]
          )
        }
      }
    }

    "drop" - {
      "quoted" in {
        test(
          quote(query[TestEntity]).dynamic.drop(quote(1)),
          query[TestEntity].drop(1)
        )
      }

      "int" in {
        test(
          quote(query[TestEntity]).dynamic.drop(1),
          query[TestEntity].drop(lift(1))
        )
      }

      "opt" - {
        "defined" in {
          test(
            quote(query[TestEntity]).dynamic.dropOpt(Some(1)),
            query[TestEntity].drop(lift(1))
          )
        }
        "empty" in {
          test(
            quote(query[TestEntity]).dynamic.dropOpt(None),
            query[TestEntity]
          )
        }
      }
    }

    "++" in {
      test(
        quote(query[TestEntity]).dynamic ++ quote(query[TestEntity]).dynamic.filter(v0 => v0.i == 1),
        query[TestEntity] ++ query[TestEntity].filter(v0 => v0.i == 1)
      )
    }

    "unionAll" in {
      test(
        quote(query[TestEntity]).dynamic.unionAll(quote(query[TestEntity]).dynamic.filter(v0 => v0.i == 1)),
        query[TestEntity].unionAll(query[TestEntity].filter(v0 => v0.i == 1))
      )
    }

    "union" in {
      test(
        quote(query[TestEntity]).dynamic.union(quote(query[TestEntity]).dynamic.filter(v0 => v0.i == 1)),
        query[TestEntity].union(query[TestEntity].filter(v0 => v0.i == 1))
      )
    }

    "groupBy" in {
      test(
        quote(query[TestEntity]).dynamic.groupBy(v0 => v0.i).map(v1 => v1._1),
        query[TestEntity].groupBy(v0 => v0.i).map(v1 => v1._1)
      )
    }

    "min" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.map(v1 => v1.i).min.contains(v0.i)),
        query[TestEntity].map(v0 => query[TestEntity].map(v1 => v1.i).min.contains(v0.i))
      )
    }

    "max" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.map(v1 => v1.i).max.contains(v0.i)),
        query[TestEntity].map(v0 => query[TestEntity].map(v1 => v1.i).max.contains(v0.i))
      )
    }

    "avg" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.map(v1 => v1.i).avg.contains(v0.i)),
        query[TestEntity].map(v0 => query[TestEntity].map(v1 => v1.i).avg.contains(v0.i))
      )
    }

    "sum" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.map(v1 => v1.i).sum.contains(v0.i)),
        query[TestEntity].map(v0 => query[TestEntity].map(v1 => v1.i).sum.contains(v0.i))
      )
    }

    "size" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.size),
        query[TestEntity].map(v0 => query[TestEntity].size)
      )
    }

    "regular joins" - {

      "join" in {
        test(
          quote(query[TestEntity]).dynamic.join(quote(query[TestEntity]).dynamic).on((v0, v1) => v0.i == v1.i),
          query[TestEntity].join(query[TestEntity]).on((v0, v1) => v0.i == v1.i)
        )
      }

      "leftJoin" in {
        test(
          quote(query[TestEntity]).dynamic.leftJoin(quote(query[TestEntity2]).dynamic).on((v0, v1) => v0.i == v1.i),
          query[TestEntity].leftJoin(query[TestEntity2]).on((v0, v1) => v0.i == v1.i)
        )
      }

      "rightJoin" in {
        test(
          quote(query[TestEntity]).dynamic.rightJoin(quote(query[TestEntity2]).dynamic).on((v0, v1) => v0.i == v1.i),
          query[TestEntity].rightJoin(query[TestEntity2]).on((v0, v1) => v0.i == v1.i)
        )
      }

      "fullJoin" in {
        test(
          quote(query[TestEntity]).dynamic.fullJoin(quote(query[TestEntity2]).dynamic).on((v0, v1) => v0.i == v1.i),
          query[TestEntity].fullJoin(query[TestEntity2]).on((v0, v1) => v0.i == v1.i)
        )
      }

    }

    "flat joins" - {
      "join" in {
        test(
          for {
            v0 <- quote(query[TestEntity]).dynamic
            v1 <- quote(query[TestEntity2]).dynamic.join(v1 => v0.i == v1.i)
          } yield (unquote(v0), unquote(v1)),
          for {
            v0 <- query[TestEntity]
            v1 <- query[TestEntity2].join(v1 => v0.i == v1.i)
          } yield (unquote(v0), unquote(v1))
        )
      }

      "leftJoin" in {
        test(
          for {
            v0 <- quote(query[TestEntity]).dynamic
            v1 <- quote(query[TestEntity2]).dynamic.leftJoin(v1 => v0.i == v1.i)
          } yield (unquote(v0), unquote(v1)),
          for {
            v0 <- query[TestEntity]
            v1 <- query[TestEntity2].leftJoin(v1 => v0.i == v1.i)
          } yield (unquote(v0), unquote(v1))
        )
      }

      "rightJoin" in {
        test(
          for {
            v0 <- quote(query[TestEntity]).dynamic
            v1 <- quote(query[TestEntity2]).dynamic.rightJoin(v1 => v0.i == v1.i)
          } yield (unquote(v0), unquote(v1)),
          for {
            v0 <- query[TestEntity]
            v1 <- query[TestEntity2].rightJoin(v1 => v0.i == v1.i)
          } yield (unquote(v0), unquote(v1))
        )
      }
    }

    "nonEmpty" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.nonEmpty),
        query[TestEntity].map(v0 => query[TestEntity].nonEmpty)
      )
    }

    "isEmpty" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.isEmpty),
        query[TestEntity].map(v0 => query[TestEntity].isEmpty)
      )
    }

    "contains" - {
      "quoted" in {
        test(
          quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.map(v1 => v1.i).contains(quote(v0.i))),
          query[TestEntity].map(v0 => query[TestEntity].map(v1 => v1.i).contains(v0.i))
        )
      }
      "value" in {
        test(
          quote(query[TestEntity]).dynamic.map(v0 => quote(query[TestEntity]).dynamic.map(v1 => v1.i).contains(1)),
          query[TestEntity].map(v0 => query[TestEntity].map(v1 => v1.i).contains(lift(1)))
        )
      }
    }

    "distinct" in {
      test(
        quote(query[TestEntity]).dynamic.distinct,
        query[TestEntity].distinct
      )
    }

    "nested" in {
      test(
        quote(query[TestEntity]).dynamic.nested.map(v0 => v0.i),
        query[TestEntity].nested.map(v0 => v0.i)
      )
    }
  }

  "entityQuery" - {
    def test[T: QueryMeta](d: Quoted[EntityQuery[T]], s: Quoted[EntityQuery[T]]) =
      testContext.run(d).string mustEqual testContext.run(s).string

    "filter" in {
      test(
        quote(query[TestEntity]).dynamic.filter(v0 => v0.i == 1),
        query[TestEntity].filter(v0 => v0.i == 1)
      )
    }
    "withFilter" in {
      test(
        quote(query[TestEntity]).dynamic.withFilter(v0 => v0.i == 1),
        query[TestEntity].withFilter(v0 => v0.i == 1)
      )
    }
    "filterOpt" - {
      "defined" in {
        val o = Some(1)
        test(
          quote(query[TestEntity]).dynamic.filterOpt(o)((v0, i) => v0.i == i),
          query[TestEntity].filter(v0 => v0.i == lift(1))
        )
      }
      "empty" in {
        val o: Option[Int] = None
        test(
          quote(query[TestEntity]).dynamic.filterOpt(o)((v0, i) => v0.i == i),
          query[TestEntity]
        )
      }
    }
    "map" in {
      test(
        quote(query[TestEntity]).dynamic.map(v0 => v0.i),
        query[TestEntity].map(v0 => v0.i)
      )
    }
  }

  "actions" - {
    def test[T](d: Quoted[Action[T]], s: Quoted[Action[T]]) =
      testContext.run(d).string mustEqual testContext.run(s).string

    val t = TestEntity("s", 1, 2L, Some(3), true)
    "insertValue" in {
      test(
        quote(query[TestEntity]).dynamic.insertValue(t),
        query[TestEntity].insert(lift(t))
      )
    }

    "updateValue" in {
      test(
        quote(query[TestEntity]).dynamic.updateValue(t),
        query[TestEntity].update(lift(t))
      )
    }

    "insert" - {
      "one column" in {
        test(
          quote(query[TestEntity]).dynamic.insert(set(_.i, 1)),
          query[TestEntity].insert(v => v.i -> 1)
        )
      }
      "multiple columns" in {
        test(
          quote(query[TestEntity]).dynamic.insert(set(_.i, 1), set(_.l, 2L)),
          query[TestEntity].insert(v => v.i -> 1, v => v.l -> 2L)
        )
      }
      "setOpt" in {
        test(
          quote(query[TestEntity]).dynamic.insert(setOpt(_.i, None), setOpt(_.l, Some(2L))),
          query[TestEntity].insert(v => v.l -> lift(2L))
        )
      }
      "string column name" in {
        test(
          quote(query[TestEntity]).dynamic.insert(set("i", 1), set("l", 2L)),
          query[TestEntity].insert(v => v.i -> 1, v => v.l -> 2L)
        )
      }
      "returning" in {
        test(
          quote(query[TestEntity]).dynamic.insert(set(_.i, 1)).returningGenerated(v0 => v0.l),
          quote {
            query[TestEntity].insert(v => v.i -> 1).returningGenerated(v0 => v0.l)
          }
        )
      }
      "returning non quoted" in {
        test(
          quote(query[TestEntity]).dynamic.insert(set(_.i, 1)).returningGenerated(v0 => v0.l),
          query[TestEntity].insert(v => v.i -> 1).returningGenerated((v0: TestEntity) => v0.l)
        )
      }
      "onConflictIgnore" - {
        "simple" in {
          test(
            quote(query[TestEntity]).dynamic.insert(set(_.i, 1)).onConflictIgnore,
            query[TestEntity].insert(v => v.i -> 1).onConflictIgnore
          )
        }
        "with targets" in {
          test(
            quote(query[TestEntity]).dynamic.insert(set(_.i, 1)).onConflictIgnore(_.i),
            query[TestEntity].insert(v => v.i -> 1).onConflictIgnore(_.i)
          )
        }
      }
    }

    "update" - {
      "one column" in {
        test(
          quote(query[TestEntity]).dynamic.update(set(_.i, 1)),
          query[TestEntity].update(v => v.i -> 1)
        )
      }
      "multiple columns" in {
        test(
          quote(query[TestEntity]).dynamic.update(set(_.i, 1), set(_.l, 2L)),
          query[TestEntity].update(v => v.i -> 1, v => v.l -> 2L)
        )
      }
      "string column name" in {
        test(
          quote(query[TestEntity]).dynamic.update(set("i", 1), set("l", 2L)),
          query[TestEntity].update(v => v.i -> 1, v => v.l -> 2L)
        )
      }
    }

    "delete" in {
      test(
        quote(query[TestEntity]).dynamic.delete,
        query[TestEntity].delete
      )
    }

  }

}