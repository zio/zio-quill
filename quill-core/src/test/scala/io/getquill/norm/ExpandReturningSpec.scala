package io.getquill.norm

import io.getquill.ReturnAction.{ ReturnColumns, ReturnRecord }
import io.getquill._
import io.getquill.ast.Renameable.ByStrategy
import io.getquill.ast.Visibility.Visible
import io.getquill.ast._
import io.getquill.context.Expand
import io.getquill.quat._

class ExpandReturningSpec extends Spec {

  case class Person(name: String, age: Int)
  case class Foo(bar: String, baz: Int)
  val quat = Quat.Product("name" -> QV, "age" -> QV)

  "inner apply" - {
    val mi = MirrorIdiom
    val ctx = new MirrorContext(mi, Literal)
    import ctx._

    "should replace tuple clauses with ExternalIdent" in {
      val q = quote {
        query[Person].insert(lift(Person("Joe", 123))).returning(p => (p.name, p.age))
      }
      val list =
        ExpandReturning.apply(q.ast.asInstanceOf[Returning])(MirrorIdiom, Literal)
      list must matchPattern {
        case List((Property(ExternalIdent("p", `quat`), "name"), _), (Property(ExternalIdent("p", `quat`), "age"), _)) =>
      }
    }

    "should replace case class clauses with ExternalIdent" in {
      val q = quote {
        query[Person].insert(lift(Person("Joe", 123))).returning(p => Foo(p.name, p.age))
      }
      val list =
        ExpandReturning.apply(q.ast.asInstanceOf[Returning])(MirrorIdiom, Literal)
      list must matchPattern {
        case List((Property(ExternalIdent("p", `quat`), "name"), _), (Property(ExternalIdent("p", `quat`), "age"), _)) =>
      }
    }
  }

  "renaming alias" - {
    val ctx = new MirrorContext(MirrorIdiom, SnakeCase)
    import ctx._

    "replaces tuple clauses with ExternalIdent(newAlias)" in {
      val q = quote {
        query[Person].insert(lift(Person("Joe", 123))).returning(p => (p.name, p.age))
      }
      val list =
        ExpandReturning.apply(q.ast.asInstanceOf[Returning], Some("OTHER"))(MirrorIdiom, SnakeCase)
      list must matchPattern {
        case List((Property(ExternalIdent("OTHER", `quat`), "name"), _), (Property(ExternalIdent("OTHER", `quat`), "age"), _)) =>
      }
    }

    "replaces case class clauses with ExternalIdent(newAlias)" in {
      val q = quote {
        query[Person].insert(lift(Person("Joe", 123))).returning(p => Foo(p.name, p.age))
      }
      val list =
        ExpandReturning.apply(q.ast.asInstanceOf[Returning], Some("OTHER"))(MirrorIdiom, SnakeCase)
      list must matchPattern {
        case List((Property(ExternalIdent("OTHER", `quat`), "name"), _), (Property(ExternalIdent("OTHER", `quat`), "age"), _)) =>
      }
    }
  }

  "returning clause" - {
    val mi = MirrorIdiom
    val ctx = new MirrorContext(mi, Literal)
    import ctx._
    val q = quote { query[Person].insert(lift(Person("Joe", 123))) }

    "should expand tuples with plain record" in {
      val qi = quote { q.returning(p => (p.name, p.age)) }
      val ret =
        ExpandReturning.applyMap(qi.ast.asInstanceOf[Returning]) {
          case (ast, stmt) => fail("Should not use this method for the returning clause")
        }(mi, Literal)

      ret mustBe ReturnRecord
    }
    "should expand case classes with plain record" in {
      val qi = quote { q.returning(p => Foo(p.name, p.age)) }
      val ret =
        ExpandReturning.applyMap(qi.ast.asInstanceOf[Returning]) {
          case (ast, stmt) => fail("Should not use this method for the returning clause")
        }(mi, Literal)

      ret mustBe ReturnRecord
    }
    "should expand whole record with plain record (converted to tuple in parser)" in {
      val qi = quote { q.returning(p => p) }
      val ret =
        ExpandReturning.applyMap(qi.ast.asInstanceOf[Returning]) {
          case (ast, stmt) => fail("Should not use this method for the returning clause")
        }(mi, Literal)

      ret mustBe ReturnRecord
    }
  }

  "returning multi" - {
    val mi = MirrorIdiomReturningMulti
    val ctx = new MirrorContext(mi, Literal)
    import ctx._
    val q = quote { query[Person].insert(lift(Person("Joe", 123))) }

    "should expand tuples" in {
      val qi = quote { q.returning(p => (p.name, p.age)) }
      val ret =
        ExpandReturning.applyMap(qi.ast.asInstanceOf[Returning]) {
          case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
        }(mi, Literal)
      ret mustBe ReturnColumns(List("name", "age"))
    }
    "should expand case classes" in {
      val qi = quote { q.returning(p => Foo(p.name, p.age)) }
      val ret =
        ExpandReturning.applyMap(qi.ast.asInstanceOf[Returning]) {
          case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
        }(mi, Literal)
      ret mustBe ReturnColumns(List("name", "age"))
    }
    "should expand case classes (converted to tuple in parser)" in {
      val qi = quote { q.returning(p => p) }
      val ret =
        ExpandReturning.applyMap(qi.ast.asInstanceOf[Returning]) {
          case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
        }(mi, Literal)
      ret mustBe ReturnColumns(List("name", "age"))
    }
  }

  "returning single and unsupported" - {

    val renameable = ByStrategy

    def insert = Insert(
      Map(
        Entity.Opinionated("Person", List(), QEP, renameable),
        Ident("p"),
        Tuple(List(Property.Opinionated(Ident("p"), "name", renameable, Visible), Property.Opinionated(Ident("p"), "age", renameable, Visible)))
      ),
      List(Assignment(Ident("pp"), Property.Opinionated(Ident("pp"), "name", renameable, Visible), Constant.auto("Joe")))
    )
    def retMulti =
      Returning(insert, Ident("r"), Tuple(List(Property.Opinionated(Ident("r"), "name", renameable, Visible), Property.Opinionated(Ident("r"), "age", renameable, Visible))))
    def retSingle =
      Returning(insert, Ident("r"), Tuple(List(Property.Opinionated(Ident("r"), "name", renameable, Visible))))

    "returning single" - {
      val mi = MirrorIdiomReturningSingle
      val ctx = new MirrorContext(mi, Literal)

      "should fail if multiple fields encountered" in {
        assertThrows[IllegalArgumentException] {
          ExpandReturning.applyMap(retMulti) {
            case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
          }(mi, Literal)
        }
      }
      "should succeed if single field encountered" in {
        val ret =
          ExpandReturning.applyMap(retSingle) {
            case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
          }(mi, Literal)
        ret mustBe ReturnColumns(List("name"))
      }
    }
    "returning unsupported" - {
      val mi = MirrorIdiomReturningUnsupported
      val ctx = new MirrorContext(mi, Literal)

      "should fail if multiple fields encountered" in {
        assertThrows[IllegalArgumentException] {
          ExpandReturning.applyMap(retMulti) {
            case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
          }(mi, Literal)
        }
      }
      "should fail if single field encountered" in {
        assertThrows[IllegalArgumentException] {
          ExpandReturning.applyMap(retSingle) {
            case (ast, stmt) => Expand(ctx, ast, stmt, mi, Literal).string
          }(mi, Literal)
        }
      }
    }
  }
}
