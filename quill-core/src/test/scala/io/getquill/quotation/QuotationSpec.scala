package io.getquill.quotation

import io.getquill.ast.Implicits._
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast.{Query => _, _}
import io.getquill.context.ValueClass
import io.getquill.norm.NormalizeStringConcat
import io.getquill.MirrorContexts.testContext._
import io.getquill.util.Messages
import io.getquill.Ord
import io.getquill.Query
import io.getquill.quat._
import io.getquill.Quoted
import io.getquill.base.Spec

import scala.math.BigDecimal.{double2bigDecimal, int2bigDecimal, javaBigDecimal2bigDecimal, long2bigDecimal}

case class CustomAnyValue(i: Int) extends AnyVal
case class EmbeddedValue(s: String, i: Int)

class QuotationSpec extends Spec {

  // remove the === matcher from scalatest so that we can test === in Context.extra
  override def convertToEqualizer[T](left: T): Equalizer[T] = new Equalizer(left)

  // Needs to be defined outside of method otherwise Scala Bug "No TypeTag available for TestEnt" manifests.
  // See https://stackoverflow.com/a/16990806/1000455
  case class TestEnt(ev: EmbeddedValue)
  case class TestEnt2(ev: Option[EmbeddedValue])
  case class ActionTestEntity(id: Int)

  "quotes and unquotes asts" - {

    "query" - {
      "schema" - {
        import io.getquill.quat.QuatOps.Implicits._

        "without aliases" in {
          quote(unquote(qr1)).ast mustEqual Entity("TestEntity", Nil, TestEntityQuat)
        }
        "with alias" in {
          val q = quote {
            querySchema[TestEntity]("SomeAlias")
          }
          quote(unquote(q)).ast mustEqual Entity.Opinionated("SomeAlias", Nil, TestEntityQuat, Fixed)
        }
        "with property alias" in {
          import io.getquill.quat.QuatOps.Implicits._

          val q = quote {
            querySchema[TestEntity]("SomeAlias", _.s -> "theS", _.i -> "theI")
          }
          quote(unquote(q)).ast mustEqual Entity.Opinionated(
            "SomeAlias",
            List(PropertyAlias(List("s"), "theS"), PropertyAlias(List("i"), "theI")),
            quatOf[TestEntity].productOrFail().renameAtPath(Nil, List("s" -> "theS", "i" -> "theI")),
            Fixed
          )
        }
        "with embedded property alias" in {

          val q = quote {
            querySchema[TestEnt]("SomeAlias", _.ev.s -> "theS", _.ev.i -> "theI")
          }
          val renamedQuat =
            quatOf[TestEnt]
              .productOrFail()
              .renameAtPath(List("ev"), List("s" -> "theS", "i" -> "theI"))
          quote(unquote(q)).ast mustEqual Entity.Opinionated(
            "SomeAlias",
            List(PropertyAlias(List("ev", "s"), "theS"), PropertyAlias(List("ev", "i"), "theI")),
            renamedQuat,
            Fixed
          )
        }
        "with embedded option property alias" in {
          val q = quote {
            querySchema[TestEnt2]("SomeAlias", _.ev.map(_.s) -> "theS", _.ev.map(_.i) -> "theI")
          }
          val renamedQuat =
            quatOf[TestEnt2]
              .productOrFail()
              .renameAtPath(List("ev"), List("s" -> "theS", "i" -> "theI"))
          quote(unquote(q)).ast mustEqual Entity.Opinionated(
            "SomeAlias",
            List(PropertyAlias(List("ev", "s"), "theS"), PropertyAlias(List("ev", "i"), "theI")),
            renamedQuat,
            Fixed
          )
        }
        "explicit `Predef.ArrowAssoc`" in {
          val q = quote {
            querySchema[TestEntity]("TestEntity", e => Predef.ArrowAssoc(e.s).->[String]("theS"))
          }
          val renamedQuat = TestEntityQuat.renameAtPath(Nil, List("s" -> "theS"))
          quote(unquote(q)).ast mustEqual Entity.Opinionated(
            "TestEntity",
            List(PropertyAlias(List("s"), "theS")),
            renamedQuat,
            Fixed
          )
        }
        "with property alias and unicode arrow" in {
          val q = quote {
            querySchema[TestEntity]("SomeAlias", _.s -> "theS", _.i -> "theI")
          }
          val renamedQuat = TestEntityQuat.renameAtPath(Nil, List("s" -> "theS", "i" -> "theI"))
          quote(unquote(q)).ast mustEqual Entity.Opinionated(
            "SomeAlias",
            List(PropertyAlias(List("s"), "theS"), PropertyAlias(List("i"), "theI")),
            renamedQuat,
            Fixed
          )
        }
        "with only some properties renamed" in {
          val q = quote {
            querySchema[TestEntity]("SomeAlias", _.s -> "theS").filter(t => t.s == "s" && t.i == 1)
          }
          val renamedQuat = TestEntityQuat.renameAtPath(Nil, List("s" -> "theS"))
          quote(unquote(q)).ast mustEqual (
            Filter(
              Entity.Opinionated("SomeAlias", List(PropertyAlias(List("s"), "theS")), renamedQuat, Fixed),
              Ident("t", renamedQuat),
              (Property(Ident("t", renamedQuat), "s") +==+ Constant
                .auto("s")) +&&+ (Property(Ident("t", renamedQuat), "i") +==+ Constant.auto(1))
            )
          )
        }

        case class TableData(id: Int)

        "with implicit property and generic" in {
          implicit class LimitQuery[T](q: Query[T]) {
            def limitQuery = quote(sql"$q LIMIT 1".as[Query[T]])
          }
          val q = quote(query[TableData].limitQuery)
          q.ast mustEqual Infix(
            List("", " LIMIT 1"),
            List(Entity("TableData", List(), Quat.LeafProduct("id"))),
            false,
            false,
            Quat.Generic
          )
          quote(unquote(q)).ast mustEqual Infix(
            List("", " LIMIT 1"),
            List(Entity("TableData", List(), Quat.LeafProduct("id"))),
            false,
            false,
            Quat.LeafProduct("id")
          )
        }
        "with method and generic" in {
          def limitQuery[T] = quote((q: Query[T]) => sql"$q LIMIT 1".as[Query[T]])
          val q             = quote(limitQuery(query[TableData]))
          q.ast mustEqual Infix(
            List("", " LIMIT 1"),
            List(Entity("TableData", List(), Quat.LeafProduct("id"))),
            false,
            false,
            Quat.Generic
          )
          quote(unquote(q)).ast mustEqual Infix(
            List("", " LIMIT 1"),
            List(Entity("TableData", List(), Quat.LeafProduct("id"))),
            false,
            false,
            Quat.LeafProduct("id")
          )
        }
        "with method and generic - typed" in {
          def limitQuery[T] = quote((q: Query[T]) => sql"$q LIMIT 1".as[Query[T]])
          val q             = quote(limitQuery[TableData](query[TableData]))
          q.ast mustEqual Infix(
            List("", " LIMIT 1"),
            List(Entity("TableData", List(), Quat.LeafProduct("id"))),
            false,
            false,
            Quat.Generic
          )
          quote(unquote(q)).ast mustEqual Infix(
            List("", " LIMIT 1"),
            List(Entity("TableData", List(), Quat.LeafProduct("id"))),
            false,
            false,
            Quat.LeafProduct("id")
          )
        }
      }
      "filter" in {
        val q = quote {
          qr1.filter(t => t.s == "s")
        }
        quote(unquote(q)).ast mustEqual Filter(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t", TestEntityQuat),
          BinaryOperation(Property(Ident("t", TestEntityQuat), "s"), EqualityOperator.`_==`, Constant.auto("s"))
        )
      }
      "withFilter" in {
        val q = quote {
          qr1.withFilter(t => t.s == "s")
        }
        quote(unquote(q)).ast mustEqual Filter(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          BinaryOperation(Property(Ident("t"), "s"), EqualityOperator.`_==`, Constant.auto("s"))
        )
      }
      "map" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        quote(unquote(q)).ast mustEqual Map(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          Property(Ident("t"), "s")
        )
      }
      "flatMap" in {
        val q = quote {
          qr1.flatMap(t => qr2)
        }
        quote(unquote(q)).ast mustEqual FlatMap(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t", TestEntityQuat),
          Entity("TestEntity2", Nil, TestEntity2Quat)
        )
      }
      "concatMap" in {
        val q = quote {
          qr1.concatMap(t => t.s.split(" "))
        }
        quote(unquote(q)).ast mustEqual ConcatMap(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          BinaryOperation(Property(Ident("t", TestEntityQuat), "s"), StringOperator.`split`, Constant.auto(" "))
        )
      }
      "sortBy" - {
        "default ordering" in {
          val q = quote {
            qr1.sortBy(t => t.s)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t", TestEntityQuat),
            Property(Ident("t"), "s"),
            AscNullsFirst
          )
        }
        "asc" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.asc)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t"),
            Property(Ident("t"), "s"),
            Asc
          )
        }
        "desc" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.desc)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t"),
            Property(Ident("t"), "s"),
            Desc
          )
        }
        "ascNullsFirst" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.ascNullsFirst)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t"),
            Property(Ident("t"), "s"),
            AscNullsFirst
          )
        }
        "descNullsFirst" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.descNullsFirst)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t"),
            Property(Ident("t"), "s"),
            DescNullsFirst
          )
        }
        "ascNullsLast" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.ascNullsLast)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t"),
            Property(Ident("t"), "s"),
            AscNullsLast
          )
        }
        "descNullsLast" in {
          val q = quote {
            qr1.sortBy(t => t.s)(Ord.descNullsLast)
          }
          quote(unquote(q)).ast mustEqual SortBy(
            Entity("TestEntity", Nil, TestEntityQuat),
            Ident("t"),
            Property(Ident("t"), "s"),
            DescNullsLast
          )
        }
        "tuple" - {
          "simple" in {
            val q = quote {
              qr1.sortBy(t => (t.s, t.i))(Ord.desc)
            }
            quote(unquote(q)).ast mustEqual SortBy(
              Entity("TestEntity", Nil, TestEntityQuat),
              Ident("t"),
              Tuple(List(Property(Ident("t"), "s"), Property(Ident("t"), "i"))),
              Desc
            )
          }
          "by element" in {
            val q = quote {
              qr1.sortBy(t => (t.s, t.i))(Ord(Ord.desc, Ord.asc))
            }
            quote(unquote(q)).ast mustEqual SortBy(
              Entity("TestEntity", Nil, TestEntityQuat),
              Ident("t"),
              Tuple(List(Property(Ident("t"), "s"), Property(Ident("t"), "i"))),
              TupleOrdering(List(Desc, Asc))
            )
          }
        }
      }
      "groupBy" in {
        val q = quote {
          qr1.groupBy(t => t.s)
        }
        quote(unquote(q)).ast mustEqual GroupBy(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t", TestEntityQuat),
          Property(Ident("t", TestEntityQuat), "s")
        )
      }

      "aggregation" - {
        "min" in {
          val q = quote {
            qr1.map(t => t.i).min
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`min`,
            Map(
              Entity("TestEntity", Nil, TestEntityQuat),
              Ident("t", TestEntityQuat),
              Property(Ident("t", TestEntityQuat), "i")
            )
          )
        }
        "max" in {
          val q = quote {
            qr1.map(t => t.i).max
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`max`,
            Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Property(Ident("t", TestEntityQuat), "i"))
          )
        }
        "avg" in {
          val q = quote {
            qr1.map(t => t.i).avg
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`avg`,
            Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Property(Ident("t", TestEntityQuat), "i"))
          )
        }
        "sum" in {
          val q = quote {
            qr1.map(t => t.i).sum
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`sum`,
            Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Property(Ident("t", TestEntityQuat), "i"))
          )
        }
        "size" in {
          val q = quote {
            qr1.map(t => t.i).size
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`size`,
            Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Property(Ident("t", TestEntityQuat), "i"))
          )
        }
      }

      "aggregation implicits" - {
        "min" in {
          val q = quote {
            qr1.map(t => t.s).min
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`min`,
            Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Property(Ident("t", TestEntityQuat), "s"))
          )
        }
        "max" in {
          val q = quote {
            qr1.map(t => t.s).max
          }
          quote(unquote(q)).ast mustEqual Aggregation(
            AggregationOperator.`max`,
            Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Property(Ident("t", TestEntityQuat), "s"))
          )
        }
      }
      "distinct" in {
        val q = quote {
          qr1.distinct
        }
        quote(unquote(q)).ast mustEqual Distinct(Entity("TestEntity", Nil, TestEntityQuat))
      }
      "nested" in {
        val q = quote {
          qr1.nested
        }
        quote(unquote(q)).ast mustEqual Nested(Entity("TestEntity", Nil, TestEntityQuat))
      }
      "take" in {
        val q = quote {
          qr1.take(10)
        }
        quote(unquote(q)).ast mustEqual Take(Entity("TestEntity", Nil, TestEntityQuat), Constant.auto(10))
      }
      "drop" in {
        val q = quote {
          qr1.drop(10)
        }
        quote(unquote(q)).ast mustEqual Drop(Entity("TestEntity", Nil, TestEntityQuat), Constant.auto(10))
      }
      "union" in {
        val q = quote {
          qr1.union(qr2)
        }
        quote(unquote(q)).ast mustEqual Union(
          Entity("TestEntity", Nil, TestEntityQuat),
          Entity("TestEntity2", Nil, TestEntity2Quat)
        )
      }
      "unionAll" - {
        "unionAll" in {
          val q = quote {
            qr1.union(qr2)
          }
          quote(unquote(q)).ast mustEqual Union(
            Entity("TestEntity", Nil, TestEntityQuat),
            Entity("TestEntity2", Nil, TestEntity2Quat)
          )
        }
        "++" in {
          val q = quote {
            qr1 ++ qr2
          }
          quote(unquote(q)).ast mustEqual UnionAll(
            Entity("TestEntity", Nil, TestEntityQuat),
            Entity("TestEntity2", Nil, TestEntity2Quat)
          )
        }
      }
      "join" - {

        def tree(t: JoinType) =
          Join(
            t,
            Entity("TestEntity", Nil, TestEntityQuat),
            Entity("TestEntity2", Nil, TestEntity2Quat),
            Ident("a"),
            Ident("b"),
            BinaryOperation(Property(Ident("a"), "s"), EqualityOperator.`_==`, Property(Ident("b"), "s"))
          )

        "inner join" in {
          val q = quote {
            qr1.join(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(InnerJoin)
        }
        "left join" in {
          val q = quote {
            qr1.leftJoin(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(LeftJoin)
        }
        "right join" in {
          val q = quote {
            qr1.rightJoin(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(RightJoin)
        }
        "full join" in {
          val q = quote {
            qr1.fullJoin(qr2).on((a, b) => a.s == b.s)
          }
          quote(unquote(q)).ast mustEqual tree(FullJoin)
        }

        "fails if not followed by 'on'" in {
          """
            quote {
              qr1.fullJoin(qr2)
            }
          """ mustNot compile
        }
      }
    }
    "action" - {
      "update" - {
        "field" in {
          val q = quote {
            qr1.update(t => t.s -> "s")
          }
          quote(unquote(q)).ast mustEqual Update(
            Entity("TestEntity", Nil, TestEntityQuat),
            List(Assignment(Ident("t"), Property(Ident("t"), "s"), Constant.auto("s")))
          )
        }
        "set field using another field" in {
          val q = quote {
            qr1.update(t => t.i -> (t.i + 1))
          }
          quote(unquote(q)).ast mustEqual Update(
            Entity("TestEntity", Nil, TestEntityQuat),
            List(
              Assignment(
                Ident("t"),
                Property(Ident("t"), "i"),
                BinaryOperation(Property(Ident("t"), "i"), NumericOperator.`+`, Constant.auto(1))
              )
            )
          )
        }
        "case class" in {
          val q = quote { (t: TestEntity) =>
            qr1.updateValue(t)
          }
          val n = quote { (t: TestEntity) =>
            qr1.update(
              v => v.s -> t.s,
              v => v.i -> t.i,
              v => v.l -> t.l,
              v => v.o -> t.o,
              v => v.b -> t.b
            )
          }
          quote(unquote(q)).ast mustEqual n.ast
        }
        "explicit `Predef.ArrowAssoc`" in {
          val q = quote {
            qr1.update(t => Predef.ArrowAssoc(t.s).->[String]("s"))
          }
          quote(unquote(q)).ast mustEqual Update(
            Entity("TestEntity", Nil, TestEntityQuat),
            List(Assignment(Ident("t"), Property(Ident("t"), "s"), Constant.auto("s")))
          )
        }
        "unicode arrow must compile" in {
          """|quote {
             |  qr1.filter(t => t.i == 1).update(_.s → "new", _.i → 0)
             |}
          """.stripMargin must compile
        }
      }
      "insert" - {
        "field" in {
          val q = quote {
            qr1.insert(t => t.s -> "s")
          }
          quote(unquote(q)).ast mustEqual Insert(
            Entity("TestEntity", Nil, TestEntityQuat),
            List(Assignment(Ident("t"), Property(Ident("t"), "s"), Constant.auto("s")))
          )
        }
        "case class" in {
          val q = quote { (t: TestEntity) =>
            qr1.insertValue(t)
          }
          val n = quote { (t: TestEntity) =>
            qr1.insert(
              v => v.s -> t.s,
              v => v.i -> t.i,
              v => v.l -> t.l,
              v => v.o -> t.o,
              v => v.b -> t.b
            )
          }
          quote(unquote(q)).ast mustEqual n.ast
        }
        "batch" in {
          val list   = List(1, 2)
          val delete = quote((i: Int) => qr1.filter(_.i == i).delete)
          val q = quote {
            liftQuery(list).foreach(i => delete(i))
          }
          quote(unquote(q)).ast mustEqual
            Foreach(ScalarQueryLift("q.list", list, intEncoder, QV), Ident("i"), delete.ast.body)
        }
        "batch with Quoted[Action[T]]" in {
          val list = List(
            ActionTestEntity(1),
            ActionTestEntity(2)
          )
          val insert = quote((row: ActionTestEntity) => query[ActionTestEntity].insertValue(row))
          val q      = quote(liftQuery(list).foreach(row => quote(insert(row))))
          quote(unquote(q)).ast mustEqual
            Foreach(CaseClassQueryLift("q.list", list, quatOf[ActionTestEntity]), Ident("row"), insert.ast.body)
        }
        "unicode arrow must compile" in {
          """|quote {
             |  qr1.insert(_.s → "new", _.i → 0)
             |}
          """.stripMargin must compile
        }
      }
      "delete" in {
        val q = quote {
          qr1.delete
        }
        quote(unquote(q)).ast mustEqual Delete(Entity("TestEntity", Nil, TestEntityQuat))
      }
      "fails if the assignment types don't match" in {
        """
          quote {
            qr1.update(t => t.i -> "s")
          }
        """ mustNot compile
      }
    }
    "value" - {
      "null" in {
        val q = quote("s" != null)
        quote(unquote(q)).ast.b mustEqual NullValue
      }
      "constant" in {
        val q = quote(11L)
        quote(unquote(q)).ast mustEqual Constant.auto(11L)
      }
      "tuple" - {
        "literal" in {
          val q = quote((1, "a"))
          quote(unquote(q)).ast mustEqual Tuple(List(Constant.auto(1), Constant.auto("a")))
        }
        "arrow assoc" - {
          "unicode arrow" in {
            val q = quote(1 -> "a")
            quote(unquote(q)).ast mustEqual Tuple(List(Constant.auto(1), Constant.auto("a")))
          }
          "normal arrow" in {
            val q = quote(1 -> "a" -> "b")
            quote(unquote(q)).ast mustEqual Tuple(
              List(Tuple(List(Constant.auto(1), Constant.auto("a"))), Constant.auto("b"))
            )
          }
          "explicit `Predef.ArrowAssoc`" in {
            val q = quote(Predef.ArrowAssoc("a").->[String]("b"))
            quote(unquote(q)).ast mustEqual Tuple(List(Constant.auto("a"), Constant.auto("b")))
          }
        }
      }
    }
    "ident" in {
      val q = quote { (s: String) =>
        s
      }
      quote(unquote(q)).ast.body mustEqual Ident("s")
    }
    "property" - {
      "class field" in {
        val q = quote {
          qr1.map(t => t.s)
        }
        quote(unquote(q)).ast.body mustEqual Property(Ident("t"), "s")
      }
      "option.get fails" in {
        """
          quote {
            (o: Option[Int]) => o.get
          }
        """ mustNot compile
      }
      "fails if not case class property" - {
        "val" in {
          case class T(s: String) {
            val boom = 1
          }
          """
          quote {
            (o: T) => o.boom
          }
          """ mustNot compile
        }
        "def" in {
          case class T(s: String) {
            def boom = 1
          }
          """
          quote {
            (o: T) => o.boom
          }
          """ mustNot compile
        }
      }
    }
    "property anonymous" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      quote(unquote(q)).ast.body mustEqual Property(Ident("t"), "s")
    }
    "function" - {
      "anonymous function" in {
        val q = quote { (s: String) =>
          s
        }
        quote(unquote(q)).ast mustEqual Function(List(Ident("s")), Ident("s"))
      }
      "with type parameter" in {
        def q[T] = quote { (q: Query[T]) =>
          q
        }
        IsDynamic(q.ast) mustEqual false
        quote(unquote(q)).ast mustEqual Function(List(Ident("q")), Ident("q"))
      }
    }
    "function apply" - {
      "local function" in {
        val f = quote { (s: String) =>
          s
        }
        val q = quote {
          f("s")
        }
        quote(unquote(q)).ast mustEqual Constant.auto("s")
      }
      "function reference" in {
        val q = quote { (f: String => String) =>
          f("a")
        }
        quote(unquote(q)).ast.body mustEqual FunctionApply(Ident("f"), List(Constant.auto("a")))
      }
    }
    "binary operation" - {
      "==" - {
        "normal" in {
          val q = quote { (a: Int, b: Int) =>
            a == b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "succeeds when different numerics are used Int/Long" in {
          val q = quote { (a: Int, b: Long) =>
            a == b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "succeeds when different numerics are used Long/Int" in {
          val q = quote { (a: Long, b: Int) =>
            a == b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "fails if the types don't match" in {
          """
            quote {
              (a: Int, b: String) => a == b
            }
          """ mustNot compile
        }
        "comparing compatible nested types" - {
          val ia = Ident("a")
          val ib = Ident("b")

          "succeeds when Option/Option" in {
            val q = quote { (a: Option[Int], b: Option[Int]) =>
              a == b
            }

            quote(unquote(q)).ast.body mustEqual (OptionIsEmpty(ia) +&&+ OptionIsEmpty(ib)) +||+ (OptionIsDefined(
              ia
            ) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib))
          }
          "succeeds when Option/T" in {
            """
            val q = quote {
              (a: Option[Int], b: Int) => a == b
            }
            """ mustNot compile
          }
          "succeeds when T/Option" in {
            """
            val q = quote {
              (a: Int, b: Option[Int]) => a == b
            }
            """ mustNot compile
          }
          "fails with multiple nesting T/Option[Option]" in {
            """
            val q = quote {
              (a: Int, b: Option[Option[Int]]) => a == b
            }
            """ mustNot compile
          }
          "succeeds with multiple nesting Option[Option]/T" in {
            """
            val q = quote {
              (a: Option[Option[Int]], b: Int) => a == b
            }
            """ mustNot compile
          }
          "succeeds when Option/None" in {
            """
            val q = quote {
              (a: Int) => a == None
            }
            """ mustNot compile
          }
          "fails when None/Option (left hand bias)" in {
            """
            val q = quote {
              (a: Int) => None == a
            }
            """ mustNot compile
          }
          "comparing types with suclassing" - {
            case class Foo(id: Int)
            trait Foot
            case class Bar(id: Int)
            trait Bart

            "succeeds when Option[T]/Option[T]" in {
              val q = quote { (a: Option[Foo], b: Option[Foo]) =>
                a == b
              }
              quote(unquote(q)).ast.body mustEqual (OptionIsEmpty(ia) +&&+ OptionIsEmpty(ib)) +||+ (OptionIsDefined(
                ia
              ) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib))
            }
            "succeeds when Option[T]/Option[subclass T]" in {
              val q = quote { (a: Option[Foo], b: Option[Foo with Foot]) =>
                a == b
              }
              quote(unquote(q)).ast.body mustEqual (OptionIsEmpty(ia) +&&+ OptionIsEmpty(ib)) +||+ (OptionIsDefined(
                ia
              ) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib))
            }
            "succeeds when Option[subclass T]/Option[T]" in {
              val q = quote { (a: Option[Foo with Foot], b: Option[Foo]) =>
                a == b
              }
              quote(unquote(q)).ast.body mustEqual (OptionIsEmpty(ia) +&&+ OptionIsEmpty(ib)) +||+ (OptionIsDefined(
                ia
              ) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib))
            }
            "fails when Option[T]/Option[A]" in {
              """
                quote {
                  (a: Option[Foo], b: Option[Bar]) => a == b
                }
              """ mustNot compile
            }
            "fails when Option[subclass1 T]/Option[subclass 2T]" in {
              """
                quote {
                  (a: Option[Foo with Foot], b: Option[Foo with Bart]) => a == b
                }
              """ mustNot compile
            }
          }
        }
        "extras" - {
          import extras._
          val ia = Ident("a")
          val ib = Ident("b")

          "normal" in {
            val q = quote { (a: Int, b: Int) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
          }
          "normal - string" in {
            val q = quote { (a: String, b: String) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
          }
          "succeeds when different numerics are used Int/Long" in {
            val q = quote { (a: Int, b: Long) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
          }
          "succeeds when Option/Option" in {
            val q = quote { (a: Option[Int], b: Option[Int]) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib)
          }
          "succeeds when Option/T" in {
            val q = quote { (a: Option[Int], b: Int) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ (ia +==+ ib)
          }
          "succeeds when T/Option" in {
            val q = quote { (a: Int, b: Option[Int]) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ib) +&&+ (ia +==+ ib)
          }
          "succeeds when Option/Option - Different Numerics" in {
            val q = quote { (a: Option[Int], b: Option[Long]) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib)
          }
          "succeeds when Option/T - Different Numerics" in {
            val q = quote { (a: Option[Int], b: Long) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ (ia +==+ ib)
          }
          "succeeds when T/Option - Different Numerics" in {
            val q = quote { (a: Int, b: Option[Long]) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ib) +&&+ (ia +==+ ib)
          }
          "succeeds when Option/Option - String" in {
            val q = quote { (a: Option[String], b: Option[String]) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ OptionIsDefined(ib) +&&+ (ia +==+ ib)
          }
          "succeeds when Option/T - String" in {
            val q = quote { (a: Option[String], b: String) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ (ia +==+ ib)
          }
          "succeeds when T/Option - String" in {
            val q = quote { (a: String, b: Option[String]) =>
              a === b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ib) +&&+ (ia +==+ ib)
          }
        }
      }
      "equals" - {
        "equals method" in {
          val q = quote { (a: Int, b: Int) =>
            a.equals(b)
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "==" in {
          val q = quote { (a: Int, b: Int) =>
            a == b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }

        case class Foo(id: Int)
        trait Foot
        case class Bar(id: Int)
        trait Bart

        "should succeed if right is subclass" in {
          val q = quote { (a: Foo, b: Foo with Foot) =>
            a.equals(b)
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "should succeed if left is subclass" in {
          val q = quote { (a: Foo with Foot, b: Foo) =>
            a.equals(b)
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "should succeed with refinements" in {
          val q = quote { (a: Foo with Foot, b: Foo with Foot) =>
            a.equals(b)
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_==`, Ident("b"))
        }
        "should fail if both are subclasses" in {
          "quote{ (a: Foo with Foot, b: Foo with Bart) => a.equals(b) }.ast.body" mustNot compile
        }
        "should fail if classes unrelated" in {
          "quote{ (a: Foo, b: Bar) => a.equals(b) }.ast.body" mustNot compile
        }
      }
      "!=" - {
        "normal" in {
          val q = quote { (a: Int, b: Int) =>
            a != b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_!=`, Ident("b"))
        }
        "succeeds when different numerics are used Int/Long" in {
          val q = quote { (a: Int, b: Long) =>
            a != b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_!=`, Ident("b"))
        }
        "succeeds when different numerics are used Long/Int" in {
          val q = quote { (a: Long, b: Int) =>
            a != b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_!=`, Ident("b"))
        }
        "fails if the types don't match" in {
          """
            quote {
              (a: Int, b: String) => a != b
            }
          """ mustNot compile
        }
        "comparing compatible nested types" - {
          val ia = Ident("a")
          val ib = Ident("b")

          "succeeds when Option/Option" in {
            val q = quote { (a: Option[Int], b: Option[Int]) =>
              a != b
            }
            quote(unquote(q)).ast.body mustEqual (OptionIsDefined(ia) +&&+ OptionIsEmpty(ib)) +||+ (OptionIsEmpty(
              ia
            ) +&&+ OptionIsDefined(ib)) +||+ (ia +!=+ ib)
          }
          "fails when Option/T" in {
            """
            val q = quote {
              (a: Option[Int], b: Int) => a != b
            }
            """ mustNot compile
          }
          "fails when T/Option" in {
            """
            val q = quote {
              (a: Int, b: Option[Int]) => a != b
            }
            """ mustNot compile
          }
          "fails with multiple nesting T/Option[Option]" in {
            """
            val q = quote {
              (a: Int, b: Option[Option[Int]]) => a != b
            }
            """ mustNot compile
          }
          "fails with multiple nesting Option[Option]/T" in {
            """
            val q = quote {
              (a: Option[Option[Int]], b: Int) => a != b
            }
            """ mustNot compile
          }
          "succeeds when Option/None" in {
            """
            val q = quote {
              (a: Int) => a != None
            }
            """ mustNot compile
          }
          "fails when None/Option (left hand bias)" in {
            """
            val q = quote {
              (a: Int) => None != a
            }
            """ mustNot compile
          }
          "comparing types with suclassing" - {
            case class Foo(id: Int)
            trait Foot
            case class Bar(id: Int)
            trait Bart

            "succeeds when Option[T]/Option[subclass T]" in {
              val q = quote { (a: Option[Foo], b: Option[Foo with Foot]) =>
                a != b
              }
              quote(unquote(q)).ast.body mustEqual (OptionIsDefined(ia) +&&+ OptionIsEmpty(ib)) +||+ (OptionIsEmpty(
                ia
              ) +&&+ OptionIsDefined(ib)) +||+ (ia +!=+ ib)
            }
            "succeeds when Option[subclass T]/Option[T]" in {
              val q = quote { (a: Option[Foo with Foot], b: Option[Foo]) =>
                a != b
              }
              quote(unquote(q)).ast.body mustEqual (OptionIsDefined(ia) +&&+ OptionIsEmpty(ib) +||+ (OptionIsEmpty(
                ia
              ) +&&+ OptionIsDefined(ib)) +||+ (ia +!=+ ib))
            }
            "fails when Option[T]/Option[A]" in {
              """
                quote {
                  (a: Option[Foo], b: Option[Bar]) => a != b
                }
              """ mustNot compile
            }
            "fails when Option[subclass1 T]/Option[subclass 2T]" in {
              """
                quote {
                  (a: Option[Foo with Foot], b: Option[Foo with Bart]) => a != b
                }
              """ mustNot compile
            }
          }
        }
        "extras" - {
          import extras._
          val ia = Ident("a")
          val ib = Ident("b")

          "normal" in {
            val q = quote { (a: Int, b: Int) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_!=`, Ident("b"))
          }
          "succeeds when different numerics are used Int/Long" in {
            val q = quote { (a: Int, b: Long) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), EqualityOperator.`_!=`, Ident("b"))
          }
          "succeeds when Option/Option" in {
            val q = quote { (a: Option[Int], b: Option[Int]) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ OptionIsDefined(ib) +&&+ (ia +!=+ ib)
          }
          "succeeds when Option/T" in {
            val q = quote { (a: Option[Int], b: Int) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ (ia +!=+ ib)
          }
          "succeeds when T/Option" in {
            val q = quote { (a: Int, b: Option[Int]) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ib) +&&+ (ia +!=+ ib)
          }
          "succeeds when Option/Option - String" in {
            val q = quote { (a: Option[String], b: Option[String]) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ OptionIsDefined(ib) +&&+ (ia +!=+ ib)
          }
          "succeeds when Option/T - String" in {
            val q = quote { (a: Option[String], b: String) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ia) +&&+ (ia +!=+ ib)
          }
          "succeeds when T/Option - String" in {
            val q = quote { (a: String, b: Option[String]) =>
              a =!= b
            }
            quote(unquote(q)).ast.body mustEqual OptionIsDefined(ib) +&&+ (ia +!=+ ib)
          }
        }
      }
      "&&" in {
        val q = quote { (a: Boolean, b: Boolean) =>
          a && b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), BooleanOperator.`&&`, Ident("b"))
      }
      "||" in {
        val q = quote { (a: Boolean, b: Boolean) =>
          a || b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), BooleanOperator.`||`, Ident("b"))
      }
      "-" in {
        val q = quote { (a: Int, b: Int) =>
          a - b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`-`, Ident("b"))
      }
      "+" - {
        "numeric" in {
          val q = quote { (a: Int, b: Int) =>
            a + b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`+`, Ident("b"))
        }
        "string" in {
          val q = quote { (a: String, b: String) =>
            a + b
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), StringOperator.`+`, Ident("b"))
        }
        "string interpolation" - {

          def normStrConcat(ast: Ast): Ast = NormalizeStringConcat(ast)

          "one param" - {
            "end" in {
              val q = quote { (i: Int) =>
                s"v$i"
              }
              quote(unquote(q)).ast.body mustEqual BinaryOperation(Constant.auto("v"), StringOperator.`+`, Ident("i"))
            }
            "start" in {
              val q = quote { (i: Int) =>
                s"${i}v"
              }
              normStrConcat(quote(unquote(q)).ast.body) mustEqual BinaryOperation(
                Ident("i"),
                StringOperator.`+`,
                Constant.auto("v")
              )
            }
          }
          "multiple params" in {
            val q = quote { (i: Int, j: Int, h: Int) =>
              s"${i}a${j}b${h}"
            }
            normStrConcat(quote(unquote(q)).ast.body) mustEqual BinaryOperation(
              BinaryOperation(
                BinaryOperation(
                  BinaryOperation(Ident("i"), StringOperator.`+`, Constant.auto("a")),
                  StringOperator.`+`,
                  Ident("j")
                ),
                StringOperator.`+`,
                Constant.auto("b")
              ),
              StringOperator.`+`,
              Ident("h")
            )
          }
        }
      }
      "*" in {
        val q = quote { (a: Int, b: Int) =>
          a * b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`*`, Ident("b"))
      }
      ">" in {
        val q = quote { (a: Int, b: Int) =>
          a > b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`>`, Ident("b"))
      }
      ">=" in {
        val q = quote { (a: Int, b: Int) =>
          a >= b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`>=`, Ident("b"))
      }
      "<" in {
        val q = quote { (a: Int, b: Int) =>
          a < b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`<`, Ident("b"))
      }
      "<=" in {
        val q = quote { (a: Int, b: Int) =>
          a <= b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`<=`, Ident("b"))
      }
      "/" in {
        val q = quote { (a: Int, b: Int) =>
          a / b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`/`, Ident("b"))
      }
      "%" in {
        val q = quote { (a: Int, b: Int) =>
          a % b
        }
        quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), NumericOperator.`%`, Ident("b"))
      }
      "contains" - {
        "query" in {
          val q = quote { (a: Query[TestEntity], b: TestEntity) =>
            a.contains(b)
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("a"), SetOperator.`contains`, Ident("b"))
        }
        "within option operation" - {
          "forall" in {
            val q = quote { (a: Query[Int], b: Option[Int]) =>
              b.forall(a.contains)
            }
            quote(unquote(q)).ast.body mustBe an[OptionOperation]
          }
          "exists" in {
            val q = quote { (a: Query[Int], b: Option[Int]) =>
              b.exists(a.contains)
            }
            quote(unquote(q)).ast.body mustBe an[OptionOperation]
          }
          "map" in {
            val q = quote { (a: Query[Int], b: Option[Int]) =>
              b.map(a.contains)
            }
            quote(unquote(q)).ast.body mustBe an[OptionOperation]
          }
        }
        "split" in {
          val q = quote { (s: String) =>
            s.split(" ")
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(Ident("s"), StringOperator.`split`, Constant.auto(" "))
        }
        "startsWith" in {
          val q = quote { (s: String) =>
            s.startsWith(" ")
          }
          quote(unquote(q)).ast.body mustEqual BinaryOperation(
            Ident("s"),
            StringOperator.`startsWith`,
            Constant.auto(" ")
          )
        }
      }
    }
    "unary operation" - {
      "-" in {
        val q = quote { (a: Int) =>
          -a
        }
        quote(unquote(q)).ast.body mustEqual UnaryOperation(NumericOperator.`-`, Ident("a"))
      }
      "!" in {
        val q = quote { (a: Boolean) =>
          !a
        }
        quote(unquote(q)).ast.body mustEqual UnaryOperation(BooleanOperator.`!`, Ident("a"))
      }
      "nonEmpty" in {
        val q = quote {
          qr1.nonEmpty
        }
        quote(unquote(q)).ast mustEqual UnaryOperation(
          SetOperator.`nonEmpty`,
          Entity("TestEntity", Nil, TestEntityQuat)
        )
      }
      "isEmpty" in {
        val q = quote {
          qr1.isEmpty
        }
        quote(unquote(q)).ast mustEqual UnaryOperation(SetOperator.`isEmpty`, Entity("TestEntity", Nil, TestEntityQuat))
      }
      "toUpperCase" in {
        val q = quote {
          qr1.map(t => t.s.toUpperCase)
        }
        quote(unquote(q)).ast mustEqual Map(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          UnaryOperation(StringOperator.`toUpperCase`, Property(Ident("t"), "s"))
        )
      }
      "toLowerCase" in {
        val q = quote {
          qr1.map(t => t.s.toLowerCase)
        }
        quote(unquote(q)).ast mustEqual Map(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          UnaryOperation(StringOperator.`toLowerCase`, Property(Ident("t"), "s"))
        )
      }
      "toLong" in {
        val q = quote {
          qr1.map(t => t.s.toLong)
        }
        quote(unquote(q)).ast mustEqual Map(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          UnaryOperation(StringOperator.`toLong`, Property(Ident("t"), "s"))
        )
      }
      "toInt" in {
        val q = quote {
          qr1.map(t => t.s.toInt)
        }
        quote(unquote(q)).ast mustEqual Map(
          Entity("TestEntity", Nil, TestEntityQuat),
          Ident("t"),
          UnaryOperation(StringOperator.`toInt`, Property(Ident("t"), "s"))
        )
      }
    }
    "sql" - {
      "with `as`" in {
        val q = quote {
          sql"true".as[Boolean]
        }
        quote(unquote(q)).ast mustEqual Infix(List("true"), Nil, false, false, Quat.BooleanValue)
      }
      "with params" in {
        val q = quote { (a: String, b: String) =>
          sql"$a || $b".as[String]
        }
        quote(unquote(q)).ast.body mustEqual Infix(List("", " || ", ""), List(Ident("a"), Ident("b")), false, false, QV)
      }
      "with dynamic string" - {
        "at the end - pure" in {
          val b = "dyn"
          val q = quote { (a: String) =>
            sql"$a || #$b".pure.as[String]
          }
          quote(unquote(q)).ast must matchPattern {
            case Function(_, Infix(List("", " || dyn"), List(Ident("a", Quat.Value)), true, false, QV)) =>
          }
        }
        "at the end" in {
          val b = "dyn"
          val q = quote { (a: String) =>
            sql"$a || #$b".as[String]
          }
          quote(unquote(q)).ast must matchPattern {
            case Function(_, Infix(List("", " || dyn"), List(Ident("a", Quat.Value)), false, false, QV)) =>
          }
        }
        "at the beginning - pure" in {
          val a = "dyn"
          val q = quote { (b: String) =>
            sql"#$a || $b".pure.as[String]
          }
          quote(unquote(q)).ast must matchPattern {
            case Function(_, Infix(List("dyn || ", ""), List(Ident("b", Quat.Value)), true, false, QV)) =>
          }
        }
        "at the beginning" in {
          val a = "dyn"
          val q = quote { (b: String) =>
            sql"#$a || $b".as[String]
          }
          quote(unquote(q)).ast must matchPattern {
            case Function(_, Infix(List("dyn || ", ""), List(Ident("b", Quat.Value)), false, false, QV)) =>
          }
        }
        "only" in {
          val a = "dyn1"
          val q = quote {
            sql"#$a".as[String]
          }
          quote(unquote(q)).ast mustEqual Infix(List("dyn1"), List(), false, false, QV)
        }
        "sequential - pure" in {
          val a = "dyn1"
          val b = "dyn2"
          val q = quote {
            sql"#$a#$b".pure.as[String]
          }
          quote(unquote(q)).ast mustEqual Infix(List("dyn1dyn2"), List(), true, false, QV)
        }
        "sequential" in {
          val a = "dyn1"
          val b = "dyn2"
          val q = quote {
            sql"#$a#$b".as[String]
          }
          quote(unquote(q)).ast mustEqual Infix(List("dyn1dyn2"), List(), false, false, QV)
        }
        "non-string value" in {
          case class Value(a: String)
          val a = Value("dyn")
          val q = quote {
            sql"#$a".as[String]
          }
          quote(unquote(q)).ast mustEqual Infix(List("Value(dyn)"), List(), false, false, QV)
        }
      }
    }
    "option operation" - {
      import io.getquill.ast.Implicits._

      case class Row(id: Int, value: String)

      "map" - {
        "simple" in {
          val q = quote { (o: Option[Int]) =>
            o.map(v => v)
          }
          quote(unquote(q)).ast.body mustEqual OptionMap(Ident("o"), Ident("v"), Ident("v"))
        }
        "unchecked" in {
          val q = quote { (o: Option[Row]) =>
            o.map(v => v)
          }
          quote(unquote(q)).ast.body mustEqual OptionTableMap(Ident("o"), Ident("v"), Ident("v"))
        }
      }
      "flatMap" - {
        "simple" in {
          val q = quote { (o: Option[Int]) =>
            o.flatMap(v => Option(v))
          }
          quote(unquote(q)).ast.body mustEqual OptionFlatMap(Ident("o"), Ident("v"), OptionApply(Ident("v")))
        }
        "unchecked" in {
          val q = quote { (o: Option[Row]) =>
            o.flatMap(v => Option(v))
          }
          quote(unquote(q)).ast.body mustEqual OptionTableFlatMap(Ident("o"), Ident("v"), OptionApply(Ident("v")))
        }
      }
      "getOrElse" in {
        val q = quote { (o: Option[Int]) =>
          o.getOrElse(11)
        }
        quote(unquote(q)).ast.body mustEqual OptionGetOrElse(Ident("o"), Constant.auto(11))
      }
      "map + getOrElse" in {
        val q = quote { (o: Option[Int]) =>
          o.map(i => i < 10).getOrElse(true)
        }
        quote(unquote(q)).ast.body mustEqual
          OptionGetOrElse(
            OptionMap(Ident("o"), Ident("i"), BinaryOperation(Ident("i"), NumericOperator.`<`, Constant.auto(10))),
            Constant.auto(true)
          )
      }
      "flatten" in {
        val q = quote { (o: Option[Option[Int]]) =>
          o.flatten
        }
        quote(unquote(q)).ast.body mustEqual OptionFlatten(Ident("o"))
      }
      "Some" in {
        val q = quote { (i: Int) =>
          Some(i)
        }
        quote(unquote(q)).ast.body mustEqual OptionSome(Ident("i"))
      }
      "apply" in {
        val q = quote { (i: Int) =>
          Option(i)
        }
        quote(unquote(q)).ast.body mustEqual OptionApply(Ident("i"))
      }
      "orNull" in {
        val q = quote { (o: Option[String]) =>
          o.orNull
        }
        quote(unquote(q)).ast.body mustEqual OptionOrNull(Ident("o"))
      }
      "getOrNull" in {
        val q = quote { (o: Option[Int]) =>
          o.getOrNull
        }
        quote(unquote(q)).ast.body mustEqual OptionGetOrNull(Ident("o"))
      }
      "None" in {
        val q = quote(None)
        quote(unquote(q)).ast mustEqual OptionNone(Quat.Null)
      }
      "forall" - {
        "simple" in {
          val q = quote { (o: Option[Boolean]) =>
            o.forall(v => v)
          }
          quote(unquote(q)).ast.body mustEqual OptionForall(Ident("o"), Ident("v"), Ident("v"))
        }
        "embedded" in {
          case class EmbeddedEntity(id: Int)
          "quote((o: Option[EmbeddedEntity]) => o.forall(v => v.id == 1))" mustNot compile
        }
      }
      "filterIfDefined" - {
        "simple" in {
          val q = quote { (o: Option[Boolean]) =>
            o.filterIfDefined(v => v)
          }
          quote(unquote(q)).ast.body mustEqual FilterIfDefined(Ident("o"), Ident("v"), Ident("v"))
        }
        "embedded" in {
          case class EmbeddedEntity(id: Int)
          "quote((o: Option[EmbeddedEntity]) => o.filterIfDefined(v => v.id == 1))" mustNot compile
        }
      }
      "exists" - {
        "simple" in {
          val q = quote { (o: Option[Boolean]) =>
            o.exists(v => v)
          }
          quote(unquote(q)).ast.body mustEqual OptionExists(Ident("o"), Ident("v"), Ident("v"))
        }
        "unchecked" in {
          val q = quote { (o: Option[Row]) =>
            o.exists(v => v.id == 4)
          }
          quote(unquote(q)).ast.body mustEqual OptionTableExists(
            Ident("o"),
            Ident("v"),
            Property(Ident("v"), "id") +==+ Constant.auto(4)
          )
        }
        "embedded" in {
          case class EmbeddedEntity(id: Int)
          val q = quote { (o: Option[EmbeddedEntity]) =>
            o.exists(v => v.id == 1)
          }
          quote(unquote(q)).ast.body mustEqual OptionTableExists(
            Ident("o"),
            Ident("v"),
            Property(Ident("v"), "id") +==+ Constant.auto(1)
          )
        }
      }
      "contains" in {
        val q = quote { (o: Option[Boolean], v: Int) =>
          o.contains(v)
        }
        quote(unquote(q)).ast.body mustEqual OptionContains(Ident("o"), Ident("v"))
      }
      "isEmpty" in {
        val q = quote { (o: Option[Boolean]) =>
          o.isEmpty
        }
        quote(unquote(q)).ast.body mustEqual OptionIsEmpty(Ident("o"))
      }
      "nonEmpty" in {
        val q = quote { (o: Option[Boolean]) =>
          o.nonEmpty
        }
        quote(unquote(q)).ast.body mustEqual OptionNonEmpty(Ident("o"))
      }
      "isDefined" in {
        val q = quote { (o: Option[Boolean]) =>
          o.isDefined
        }
        quote(unquote(q)).ast.body mustEqual OptionIsDefined(Ident("o"))
      }
    }
    "traversable operations" - {
      "map.contains" in {
        val q = quote { (m: collection.Map[Int, String], k: Int) =>
          m.contains(k)
        }
        quote(unquote(q)).ast.body mustEqual MapContains(Ident("m"), Ident("k"))
      }
      "set.contains" in {
        val q = quote { (s: Set[Int], v: Int) =>
          s.contains(v)
        }
        quote(unquote(q)).ast.body mustEqual SetContains(Ident("s"), Ident("v"))
      }
      "list.contains" in {
        val q = quote { (l: List[Int], v: Int) =>
          l.contains(v)
        }
        quote(unquote(q)).ast.body mustEqual ListContains(Ident("l"), Ident("v"))
      }
    }
    "boxed numbers" - {
      "big decimal" in {
        quote { (a: Int, b: Long, c: Double, d: java.math.BigDecimal) =>
          (a: BigDecimal, b: BigDecimal, c: BigDecimal, d: BigDecimal)
        }
        ()
      }
      "predef" - {
        "scala to java" in {
          val q = quote { (a: Byte, b: Short, c: Char, d: Int, e: Long, f: Float, g: Double, h: Boolean) =>
            (
              a: java.lang.Byte,
              b: java.lang.Short,
              c: java.lang.Character,
              d: java.lang.Integer,
              e: java.lang.Long,
              f: java.lang.Float,
              g: java.lang.Double,
              h: java.lang.Boolean
            )
          }
          quote(unquote(q)).ast match {
            case Function(params, Tuple(values)) =>
              values mustEqual params
            case _ => Messages.fail("Should not happen")
          }
        }
        "java to scala" in {
          val q = quote {
            (
              a: java.lang.Byte,
              b: java.lang.Short,
              c: java.lang.Character,
              d: java.lang.Integer,
              e: java.lang.Long,
              f: java.lang.Float,
              g: java.lang.Double,
              h: java.lang.Boolean
            ) =>
              (a: Byte, b: Short, c: Char, d: Int, e: Long, f: Float, g: Double, h: Boolean)
          }
          quote(unquote(q)).ast match {
            case Function(params, Tuple(values)) =>
              values mustEqual params
            case _ => Messages.fail("Should not happen")
          }
        }
      }
    }
    "dynamic" - {
      "quotation" in {
        val filtered = quote {
          qr1.filter(t => t.i == 1)
        }
        def m(b: Boolean) =
          if (b)
            filtered
          else
            qr1
        val q1 = quote {
          unquote(m(true))
        }
        val q2 = quote {
          unquote(m(false))
        }
        quote(unquote(q1)).ast mustEqual filtered.ast
        quote(unquote(q2)).ast mustEqual qr1.ast
      }
      "quoted dynamic" in {
        val i: Quoted[Int] = quote(1)
        val q: Quoted[Int] = quote(i + 1)
        quote(unquote(q)).ast mustEqual BinaryOperation(Constant.auto(1), NumericOperator.`+`, Constant.auto(1))
      }
      "arbitrary tree" in {
        object test {
          def a = quote("a")
        }
        val q = quote {
          test.a
        }
        quote(unquote(q)).ast mustEqual Constant.auto("a")
      }
      "nested" in {
        case class Add(i: Quoted[Int]) {
          def apply() = quote(i + 1)
        }
        val q = quote {
          Add(1).apply()
        }
        quote(unquote(q)).ast mustEqual BinaryOperation(Constant.auto(1), NumericOperator.`+`, Constant.auto(1))
      }
      "type param" - {
        "simple" in {
          def test[T: SchemaMeta] = quote(query[T])

          test[TestEntity].ast mustEqual Entity("TestEntity", Nil, TestEntityQuat)
        }
        "nested" in {
          def test[T: SchemaMeta] = quote(query[T].map(t => 1))
          test[TestEntity].ast mustEqual Map(Entity("TestEntity", Nil, TestEntityQuat), Ident("t"), Constant.auto(1))
        }
      }
      "forced" in {
        val q = qr1.dynamic
        "q.ast: Query[_]" mustNot compile
      }
    }
    "if" - {
      "simple" in {
        val q = quote { (c: Boolean) =>
          if (c) 1 else 2
        }
        quote(unquote(q)).ast.body mustEqual If(Ident("c"), Constant.auto(1), Constant.auto(2))
      }
      "nested" in {
        val q = quote { (c1: Boolean, c2: Boolean) =>
          if (c1) 1 else if (c2) 2 else 3
        }
        quote(unquote(q)).ast.body mustEqual If(
          Ident("c1"),
          Constant.auto(1),
          If(Ident("c2"), Constant.auto(2), Constant.auto(3))
        )
      }
    }
    "ord" in {
      val o = quote {
        Ord.desc[Int]
      }
      val q = quote {
        qr1.sortBy(_.i)(o)
      }
      quote(unquote(q)).ast.ordering mustEqual o.ast
    }
  }

  "liftings" - {

    import language.reflectiveCalls

    "retains liftings" - {
      "constant" in {
        val q = quote(lift(1))

        val l = q.liftings.`1`
        l.value mustEqual 1
        l.encoder mustEqual intEncoder
      }
      "identifier" in {
        val i = 1
        val q = quote(lift(i))

        val l = q.liftings.i
        l.value mustEqual i
        l.encoder mustEqual intEncoder
      }
      "property" in {
        case class TestEntity(a: String)
        val t = TestEntity("a")
        val q = quote(lift(t.a))

        val l = q.liftings.`t.a`
        l.value mustEqual t.a
        l.encoder mustEqual stringEncoder
      }
      "arbitrary" in {
        class A { def x = 1 }
        val q = quote(lift(new A().x))
        q.liftings.`new A().x`.value mustEqual new A().x
      }
      "duplicate" in {
        val i = 1
        val q = quote(lift(i) + lift(i))
        val l = q.liftings.i
        l.value mustEqual i
        l.encoder mustEqual intEncoder
      }
    }

    "aggregates liftings of nested quotations" - {
      "one level" in {
        val i  = 1
        val q1 = quote(lift(i))
        val q2 = quote(q1 + 1)

        val l = q2.liftings.`q1.i`
        l.value mustEqual i
        l.encoder mustEqual intEncoder
      }
      "multiple levels" in {
        val (a, b, c) = (1, 2L, 3f)
        val q1        = quote(lift(a))
        val q2        = quote(q1 + lift(b))
        val q3        = quote(q1 + q2 + lift(c))

        val l1 = q3.liftings.`q2.q1.a`
        l1.value mustEqual a
        l1.encoder mustEqual intEncoder

        val l2 = q3.liftings.`q2.b`
        l2.value mustEqual b
        l2.encoder mustEqual longEncoder

        val l3 = q3.liftings.c
        l3.value mustEqual c
        l3.encoder mustEqual floatEncoder
      }
      "in-place" in {
        val q = quote {
          quote(lift(1))
        }
        val l = q.liftings.`1`
        l.value mustEqual 1
        l.encoder mustEqual intEncoder
      }
      "nested lifted constant" in {
        val q1 = quote(lift(1))
        val q2 = quote(q1 + 1)

        val l = q2.liftings.`q1.1`
        l.value mustEqual 1
        l.encoder mustEqual intEncoder
      }
      "with implicit class" - {
        "constant" in {
          trait Implicits {
            var random = 999
            implicit class PlusRadom(q: Int) {
              def plusRandom = quote(q + lift(random))
            }
          }
          object implicits extends Implicits
          import implicits._
          val q = quote(1.plusRandom)
          val l = q.liftings.`implicits.PlusRadom(null.asInstanceOf[Int(1)]).plusRandom.Implicits.this.random`
          l.value mustEqual 999
          l.encoder mustEqual intEncoder
        }
        "query" in {
          trait Implicits {
            var random = 999
            implicit class ToRadom(q: Query[TestEntity]) {
              def toRandom = quote(q.map(_ => lift(random)))
            }
          }
          object implicits extends Implicits
          import implicits._
          val q = quote(query[TestEntity].toRandom)
          val l =
            q.liftings.`implicits.ToRadom(null.asInstanceOf[io.getquill.EntityQuery[io.getquill.MirrorContexts.testContext.TestEntity]]).toRandom.Implicits.this.random`
          l.value mustEqual 999
          l.encoder mustEqual intEncoder
        }
      }
      "embedded" in {
        case class EmbeddedTestEntity(id: String)
        case class TestEntity(embedded: EmbeddedTestEntity)
        val t = TestEntity(EmbeddedTestEntity("test"))
        val q = quote {
          query[TestEntity].insertValue(lift(t))
        }
        q.liftings.`t.embedded.id`.value mustEqual t.embedded.id
        val q2 = quote(q)
        q2.liftings.`q.t.embedded.id`.value mustEqual t.embedded.id
      }
      "merges properties into the case class lifting" - {
        val t = TestEntity("s", 1, 2L, Some(3), true)
        "direct access" in {
          val q = quote {
            lift(t).s
          }
          val l = q.liftings.`t.s`
          l.value mustEqual t.s
          l.encoder mustEqual stringEncoder
        }
        "after beta reduction" in {
          val f = quote { (t: TestEntity) =>
            t.s
          }

          val q = quote {
            f(lift(t))
          }
          val l = q.liftings.`t.s`
          l.value mustEqual t.s
          l.encoder mustEqual stringEncoder
        }
        "action" in {
          val q = quote {
            query[TestEntity].insertValue(lift(t))
          }
          val l1 = q.liftings.`t.s`
          l1.value mustEqual t.s
          l1.encoder mustEqual stringEncoder

          val l2 = q.liftings.`t.i`
          l2.value mustEqual t.i
          l2.encoder mustEqual intEncoder

          val l3 = q.liftings.`t.l`
          l3.value mustEqual t.l
          l3.encoder mustEqual longEncoder

          q.liftings.`t.o`.value mustEqual t.o
        }
        "action + beta reduction" in {
          val n = quote { (t: TestEntity) =>
            query[TestEntity].updateValue(t)
          }
          val q = quote {
            n(lift(t))
          }
          val l1 = q.liftings.`t.s`
          l1.value mustEqual t.s
          l1.encoder mustEqual stringEncoder

          val l2 = q.liftings.`t.i`
          l2.value mustEqual t.i
          l2.encoder mustEqual intEncoder

          val l3 = q.liftings.`t.l`
          l3.value mustEqual t.l
          l3.encoder mustEqual longEncoder

          q.liftings.`t.o`.value mustEqual t.o
        }
      }
    }

    "supports value class" in {
      def q(v: ValueClass) = quote {
        lift(v)
      }
      q(ValueClass(1)).liftings.`v`.value mustEqual ValueClass(1)
    }

    "supports custom AnyVal" in {
      def q(v: CustomAnyValue) = quote {
        lift(v)
      }
      q(CustomAnyValue(1)).liftings.`v`.value mustEqual CustomAnyValue(1)
    }
  }

  "reduces tuple matching locally" - {
    "simple" in {
      val q = quote { (t: (Int, Int)) =>
        t match {
          case (a, b) => a + b
        }
      }
      quote(unquote(q)).ast.body mustEqual
        BinaryOperation(Property(Ident("t"), "_1"), NumericOperator.`+`, Property(Ident("t"), "_2"))
    }
    "nested" in {
      val q = quote { (t: ((Int, Int), Int)) =>
        t match {
          case ((a, b), c) => a + b + c
        }
      }
      quote(unquote(q)).ast.body mustEqual
        BinaryOperation(
          BinaryOperation(
            Property(Property(Ident("t"), "_1"), "_1"),
            NumericOperator.`+`,
            Property(Property(Ident("t"), "_1"), "_2")
          ),
          NumericOperator.`+`,
          Property(Ident("t"), "_2")
        )
    }
  }

  "unquotes referenced quotations" in {
    val q  = quote(1)
    val q2 = quote(q + 1)
    quote(unquote(q2)).ast mustEqual BinaryOperation(Constant.auto(1), NumericOperator.`+`, Constant.auto(1))
  }

  "ignores the ifrefutable call" in {
    val q = quote {
      qr1.map(t => (t.i, t.l))
    }
    """
      quote {
        for {
          (a, b) <- q
        } yield {
          a + b
        }
      }
    """ must compile
  }

  "supports implicit quotations" - {
    "implicit class" in {
      implicit class ForUpdate[T](q: Query[T]) {
        def forUpdate = quote(sql"$q FOR UPDATE")
      }

      val q = quote {
        query[TestEntity].forUpdate
      }
      val n = quote {
        sql"${query[TestEntity]} FOR UPDATE"
      }
      quote(unquote(q)).ast mustEqual n.ast
    }
  }

  "with additional param" in {
    implicit class GreaterThan[T](q: Query[Int]) {
      def greaterThan(j: Int) = quote(q.filter(i => i > j))
    }

    val j = 1
    val q = quote {
      query[TestEntity].map(t => t.i).greaterThan(j)
    }

    val n = quote {
      query[TestEntity].map(t => t.i).filter(i => i > j)
    }

    quote(unquote(q)).ast mustEqual n.ast
  }

  "doesn't double quote" in {
    val q               = quote(1)
    val dq: Quoted[Int] = quote(q)
  }

  "doesn't a allow quotation of null" in {
    "quote(null)" mustNot compile
  }

  "fails if the tree is not valid" in {
    """quote("s".getBytes)""" mustNot compile
  }

  "infers the correct dynamic tree" in {
    val i  = -1
    val q1 = quote(qr1.filter(_.s == "aa"))
    val q2 = quote(qr1.filter(_.s == "bb"))
    val q =
      if (i > 0) q1
      else q2

    quote(unquote(q)).ast mustEqual q2.ast
  }

  // nested quotation with lifting compiles as a type member
  def quote1(i: Int) = quote {
    qr1.filter(_.i == lift(i))
  }
  def quote2 = quote {
    quote1(1)
  }
}
