package io.getquill.codegen

import io.getquill.codegen.integration.CodegenTestCases._
import io.getquill.codegen.util.ConfigPrefix.TestSqliteDB
import io.getquill.codegen.util._
import org.scalatest.matchers.should.Matchers._

class SqliteCodegenTestCases extends CodegenSpec {
  import io.getquill.codegen.generated.postgres._

  type Prefix = TestSqliteDB
  val prefix = TestSqliteDB

  "trivial generator tests" - {
    "use trivial snake case schema" in WithContext[Prefix, `1-simple-snake`].run { ctx =>
      import `1-simple-snake-lib`.public._
      import ctx._

      val results = ctx.run(query[Person].filter(_.age > 11)).toSeq
      results should contain theSameElementsAs
        (List(Person(1, "Joe", "Bloggs", 22), Person(2, "Jack", "Ripper", 33)))
    }
    "use trivial literal schema" in WithContext[Prefix, `2-simple-literal`].run { ctx =>
      import `2-simple-literal-lib`.public._
      import ctx._

      val results = ctx.run(query[Person].filter(_.age > 11)).toSeq
      results should contain theSameElementsAs
        (List(Person(1, "Joe", "Bloggs", 22), Person(2, "Jack", "Ripper", 33)))
    }
  }

  "composable generator" - {

    "1-comp-sanity" in WithContext[Prefix, `1-comp-sanity`].run { ctx =>
      import `1-comp-sanity-lib`.public._
      import ctx._
      ctx.run(query[Person].filter(_.age > 11)) should contain theSameElementsAs List(
        Person(1, "Joe", "Bloggs", 22),
        Person(2, "Jack", "Ripper", 33)
      )
    }

    "2-comp-stereo-single" in WithContext[Prefix, `2-comp-stereo-single`].run { ctx =>
      import `2-comp-stereo-single-lib`.public._
      import ctx._
      (ctx.run(PersonDao.query.filter(_.age > 11))) should contain theSameElementsAs
        (List(
          Person(1, "Joe", "Bloggs", 22),
          Person(2, "Jack", "Ripper", 33)
        ))
    }
  }
}
