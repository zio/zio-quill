package io.getquill.norm

import io.getquill.ReturnAction.{ ReturnColumns, ReturnRecord }
import io.getquill._
import io.getquill.ast._
import io.getquill.context.Expand

class ExpandReturningSpec extends Spec {

  case class Person(name: String, age: Int)
  case class Foo(bar: String, baz: Int)

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
        case List((Property(ExternalIdent("p"), "name"), _), (Property(ExternalIdent("p"), "age"), _)) =>
      }
    }

    "should replace case class clauses with ExternalIdent" in {
      val q = quote {
        query[Person].insert(lift(Person("Joe", 123))).returning(p => Foo(p.name, p.age))
      }
      val list =
        ExpandReturning.apply(q.ast.asInstanceOf[Returning])(MirrorIdiom, Literal)
      list must matchPattern {
        case List((Property(ExternalIdent("p"), "name"), _), (Property(ExternalIdent("p"), "age"), _)) =>
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
    val insert = Insert(
      Map(
        Entity("Person", List()),
        Ident("p"),
        Tuple(List(Property(Ident("p"), "name"), Property(Ident("p"), "age")))
      ),
      List(Assignment(Ident("pp"), Property(Ident("pp"), "name"), Constant("Joe")))
    )
    val retMulti =
      Returning(insert, Ident("r"), Tuple(List(Property(Ident("r"), "name"), Property(Ident("r"), "age"))))
    val retSingle =
      Returning(insert, Ident("r"), Tuple(List(Property(Ident("r"), "name"))))

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
