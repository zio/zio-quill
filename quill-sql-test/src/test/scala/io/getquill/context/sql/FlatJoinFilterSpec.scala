package io.getquill.context.sql

import io.getquill.context.sql.testContext._
import io.getquill.base.Spec

/**
 * Reproduction tests for flat join + filter column reference bugs.
 *
 * #396: Flat join in for-comprehension with outer .filter() on tuple generates
 * wrong column reference in WHERE clause.
 *
 * #239: Flat join in for-comprehension with filter on tuple references the
 * wrong table alias.
 *
 * #335: Self-referencing subquery with tuple mapping uses bare column names
 * instead of aliased _2-prefixed names.
 */
class FlatJoinFilterSpec extends Spec {

  case class File(fileKey: Long, fileCategoryKey: Int)
  case class FileCategory(fileCategoryKey: Int)

  case class Foo(id: Int, someField: String)
  case class Bar(fooId: Int, otherField: String)

  case class Item(id: Int, score: Int, category: Int)

  "#396 - flat join with outer filter should reference correct columns" in {
    val q = quote {
      (for {
        f  <- query[File]
        fc <- query[FileCategory].join(fc => fc.fileCategoryKey == f.fileCategoryKey)
      } yield (f.fileKey, fc.fileCategoryKey))
        .filter(_._1 == 1L)
    }

    val sql = testContext.run(q).string
    // The WHERE clause should reference the file table's fileKey, not a synthetic _1 on the wrong table
    sql must not include ("._1")
    // Sanity: should still be a join query
    sql must include("JOIN")
  }

  "#239 - flat join filter should reference correct table in equality" in {
    val q = quote {
      (for {
        foo <- query[Foo]
        bar <- query[Bar].join(bar => bar.fooId == foo.id)
      } yield (foo, bar))
        .filter { case (foo, bar) =>
          foo.someField == "baz" && bar.otherField == "boo"
        }
    }

    val sql = testContext.run(q).string
    // foo.someField should be on the foo table, not the bar table
    sql must not include ("bar.someField")
    sql must not include ("bar.some_field")
  }

  "#335 - subquery with tuple mapping should use correct column aliases" in {
    val q = quote {
      query[Item]
        .map(i => (sql"${i.score} + 1".as[Int], i))
        .filter(_._1 > 0)
        .map(_._2)
        .filter(x =>
          query[Item]
            .map(i => (sql"${i.score} + 1".as[Int], i))
            .filter(_._1 > 0)
            .map(_._2)
            .filter(y => y.category == x.category && y.id != x.id)
            .map(_.score)
            .max
            .exists(_ < x.score)
        )
    }

    val sql = testContext.run(q).string
    // The subquery should NOT reference bare column names that don't exist.
    // e.g. MAX(i1.score) should be MAX(i1._2score)
    sql must not include ("MAX(i1.score)")
  }
}
