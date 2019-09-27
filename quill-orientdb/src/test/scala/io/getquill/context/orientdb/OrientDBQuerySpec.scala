package io.getquill.context.orientdb

import io.getquill.ast.{ Action => AstAction, Query => AstQuery, _ }
import io.getquill.context.sql._
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.StringToken
import io.getquill.{ Literal, Spec }

class OrientDBQuerySpec extends Spec {

  val mirrorContext = orientdb.mirrorContext
  import mirrorContext._

  "map" - {
    "property" in {
      val q = quote {
        qr1.map(t => t.i)
      }
      mirrorContext.run(q).string mustEqual
        "SELECT i FROM TestEntity"
    }
    "tuple" in {
      val q = quote {
        qr1.map(t => (t.i, t.s))
      }
      mirrorContext.run(q).string mustEqual
        "SELECT i, s FROM TestEntity"
    }
    "other" in {
      val q = quote {
        qr1.map(t => "s")
      }
      mirrorContext.run(q).string mustEqual
        "SELECT 's' FROM TestEntity"
    }
  }

  "take" in {
    val q = quote {
      qr1.take(1)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s, i, l, o FROM TestEntity LIMIT 1"
  }

  "sortBy" - {
    "property" in {
      val q = quote {
        qr1.sortBy(t => t.i)
      }
      mirrorContext.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC"
    }
    "tuple" in {
      val q = quote {
        qr1.sortBy(t => (t.i, t.s))
      }
      mirrorContext.run(q).string mustEqual
        "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC, s ASC"
    }
    "custom ordering" - {
      "property" in {
        val q = quote {
          qr1.sortBy(t => t.i)(Ord.desc)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC"
      }
      "tuple" in {
        val q = quote {
          qr1.sortBy(t => (t.i, t.s))(Ord(Ord.asc, Ord.desc))
        }
        mirrorContext.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity ORDER BY i ASC, s DESC"
      }
      "tuple single ordering" in {
        val q = quote {
          qr1.sortBy(t => (t.i, t.s))(Ord.desc)
        }
        mirrorContext.run(q).string mustEqual
          "SELECT s, i, l, o FROM TestEntity ORDER BY i DESC, s DESC"
      }
    }
  }

  "filter" in {
    val q = quote {
      qr1.filter(t => t.i == 1)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s, i, l, o FROM TestEntity WHERE i = 1"
  }

  "entity" in {
    mirrorContext.run(qr1).string mustEqual
      "SELECT s, i, l, o FROM TestEntity"
  }

  "aggregation" - {
    "min" in {
      val q = quote {
        qr1.map(t => t.i).min
      }

      mirrorContext.run(q).string mustEqual
        "SELECT MIN(i) FROM TestEntity"
    }
    "max" in {
      val q = quote {
        qr1.map(t => t.i).max
      }

      mirrorContext.run(q).string mustEqual
        "SELECT MAX(i) FROM TestEntity"
    }
    "sum" in {
      val q = quote {
        qr1.map(t => t.i).sum
      }

      mirrorContext.run(q).string mustEqual
        "SELECT SUM(i) FROM TestEntity"
    }
    "avg" in {
      val q = quote {
        qr1.map(t => t.i).avg
      }
      mirrorContext.run(q).string mustEqual
        "SELECT AVG(i) FROM TestEntity"
    }
    "count" in {
      val q = quote {
        qr1.filter(t => t.i == 1).size
      }
      mirrorContext.run(q).string mustEqual
        "SELECT COUNT(*) FROM TestEntity WHERE i = 1"
    }
  }

  "distinct query" in {
    val q = quote {
      qr1.map(t => t.i).distinct
    }
    mirrorContext.run(q).string mustEqual
      "SELECT DISTINCT(i) FROM TestEntity"
  }

  "all terms" in {
    val q = quote {
      qr1.filter(t => t.i == 1).sortBy(t => t.s).take(1).map(t => t.s)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s FROM TestEntity WHERE i = 1 ORDER BY s ASC LIMIT 1"
  }

  "groupBy supported" in {
    val q = quote {
      qr1.groupBy(t => t.s).map(t => t._1)
    }
    mirrorContext.run(q).string mustEqual
      "SELECT s FROM TestEntity GROUP BY s"
  }

  "union supported" in {
    val q = quote {
      qr1.filter(_.i == 0).union(qr1.filter(_.i == 1))
    }
    mirrorContext.run(q).string mustEqual
      f"SELECT s, i, l, o FROM (SELECT $$c LET $$a = (SELECT s, i, l, o FROM TestEntity WHERE i = 0), $$b = (SELECT s, i, l, o FROM TestEntity WHERE i = 1), $$c = UNIONALL($$a, $$b))"
  }

  "unionall supported" in {
    val q = quote {
      qr1.filter(_.i == 0).unionAll(qr1.filter(_.i == 1))
    }
    mirrorContext.run(q).string mustEqual
      f"SELECT s, i, l, o FROM (SELECT $$c LET $$a = (SELECT s, i, l, o FROM TestEntity WHERE i = 0), $$b = (SELECT s, i, l, o FROM TestEntity WHERE i = 1), $$c = UNIONALL($$a, $$b))"
  }

  import OrientDBIdiom._
  implicit val n = Literal
  val i = Ident("i")

  "tokenizers" - {
    "if" in {
      val t = implicitly[Tokenizer[If]]

      t.token(If(Ident("x"), Ident("a"), Ident("b"))) mustBe stmt"if(x, a, b)"
      //this seems as not working
      t.token(If(Ident("x"), Ident("b"), If(Ident("y"), Ident("b"), Ident("c")))) mustBe stmt"if(x, b, c)"

    }
    "query" in {
      val t = implicitly[Tokenizer[AstQuery]]
      t.token(Entity("name", Nil)) mustBe SqlQuery(Entity("name", Nil)).token
    }
    "sql query" in {
      val t = implicitly[Tokenizer[SqlQuery]]
      val e = FlattenSqlQuery(select = Nil)

      t.token(e) mustBe stmt"SELECT *"

      intercept[IllegalStateException](t.token(e.copy(distinct = true)))
        .getMessage mustBe "OrientDB DISTINCT with multiple columns is not supported"

      val x = SelectValue(Ident("x"))
      intercept[IllegalStateException](t.token(e.copy(select = List(x, x), distinct = true)))
        .getMessage mustBe "OrientDB DISTINCT with multiple columns is not supported"

      val tb = TableContext(Entity("tb", Nil), "x1")
      t.token(e.copy(from = List(tb, tb))) mustBe stmt"SELECT * FROM tb"

      val jn = FlatJoinContext(InnerJoin, tb.copy(alias = "x2"), Ident("x"))
      intercept[IllegalStateException](t.token(e.copy(from = List(tb, jn))))

      t.token(e.copy(limit = Some(Ident("1")), offset = Some(Ident("2")))) mustBe stmt"SELECT * SKIP 2 LIMIT 1"
      t.token(e.copy(limit = Some(Ident("1")))) mustBe stmt"SELECT * LIMIT 1"
      t.token(e.copy(offset = Some(Ident("2")))) mustBe stmt"SELECT * SKIP 2"

      intercept[IllegalStateException](t.token(UnaryOperationSqlQuery(BooleanOperator.`!`, e)))
        .getMessage mustBe "Other operators are not supported yet. Please raise a ticket to support more operations"
    }
    "operation" in {
      val t = implicitly[Tokenizer[Operation]]
      t.token(UnaryOperation(StringOperator.`toUpperCase`, i)) mustBe stmt"toUpperCase() (i)"
      t.token(UnaryOperation(StringOperator.`toLowerCase`, i)) mustBe stmt"toLowerCase() (i)"
      intercept[IllegalStateException](t.token(UnaryOperation(BooleanOperator.`!`, i)))

      t.token(BinaryOperation(NullValue, EqualityOperator.`==`, i)) mustBe stmt"i IS NULL"
      t.token(BinaryOperation(i, EqualityOperator.`!=`, NullValue)) mustBe stmt"i IS NOT NULL"
      t.token(BinaryOperation(NullValue, EqualityOperator.`!=`, i)) mustBe stmt"i IS NOT NULL"
      t.token(BinaryOperation(i, NumericOperator.`+`, i)) mustBe stmt"i + i"
      intercept[IllegalStateException](t.token(BinaryOperation(i, EqualityOperator.`!=`, i)))
      intercept[IllegalStateException](t.token(FunctionApply(i, Nil)))
    }
    "set operation" in {
      val t = implicitly[Tokenizer[SetOperation]]
      t.token(UnionOperation) mustBe stmt"UNION"
      t.token(UnionAllOperation) mustBe stmt"UNION ALL"
    }
    "select value" in {
      val t = implicitly[Tokenizer[SelectValue]]
      t.token(SelectValue(Ident("?"))) mustBe "?".token
      t.token(SelectValue(Aggregation(AggregationOperator.`max`, Entity("t", Nil)), Some("x"))) mustBe stmt"(SELECT MAX(*) FROM t) x"
    }
    "prop" in {
      val t = implicitly[Tokenizer[Property]]
      t.token(Property(i, "isEmpty")) mustBe stmt"i IS NULL"
      t.token(Property(i, "nonEmpty")) mustBe stmt"i IS NOT NULL"
      t.token(Property(i, "isDefined")) mustBe stmt"i IS NOT NULL"
    }
    "value" in {
      val t = implicitly[Tokenizer[Value]]
      t.token(NullValue) mustBe stmt"null"
      t.token(Tuple(List(NullValue))) mustBe stmt"null"
    }
    "action" in {
      val t = implicitly[Tokenizer[AstAction]]
      intercept[IllegalStateException](t.token(null: AstAction))
      def ins(a: String) =
        Insert(Entity("tb", Nil), List(Assignment(i, Property(Property(i, "x"), a), i)))
      t.token(ins("isEmpty")) mustBe stmt"INSERT INTO tb (x IS NULL) VALUES(i)"
      t.token(ins("isDefined")) mustBe stmt"INSERT INTO tb (x IS NOT NULL) VALUES(i)"
      t.token(ins("nonEmpty")) mustBe stmt"INSERT INTO tb (x IS NOT NULL) VALUES(i)"
      t.token(Insert(Entity("tb", Nil), List(Assignment(i, Property(i, "i"), i)))) mustBe stmt"INSERT INTO tb (i) VALUES(i)"
    }
    // not actually used anywhere but doing a sanity check here
    "external ident sanity check" in {
      val t = implicitly[Tokenizer[ExternalIdent]]
      t.token(ExternalIdent("TestIdent")) mustBe StringToken("TestIdent")
    }
  }
}