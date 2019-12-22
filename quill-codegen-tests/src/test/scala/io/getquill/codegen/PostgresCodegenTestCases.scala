package io.getquill.codegen

import io.getquill.codegen.integration.CodegenTestCases._
import io.getquill.codegen.util.ConfigPrefix.TestPostgresDB
import io.getquill.codegen.util._
import org.scalatest.matchers.should.Matchers._

class PostgresCodegenTestCases extends CodegenSpec {
  import io.getquill.codegen.generated.postgres._

  type Prefix = TestPostgresDB
  val prefix = TestPostgresDB

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

    "3-comp-stereo-oneschema" in WithContext[Prefix, `3-comp-stereo-oneschema`].run { ctx =>
      import `3-comp-stereo-oneschema-lib`.public._
      import ctx._

      (ctx.run(PublicSchema.PersonDao.alphaPerson.filter(_.age > 11))) should contain theSameElementsAs (
        List(
          Person(1, "Joe", "Bloggs", 22, 55L, "Wonkles"),
          Person(2, "Jack", "Ripper", 33, 66L, "Ginkles")
        )
      )
      (ctx.run(PublicSchema.AddressDao.publicAddress.filter(_.personFk == 1))) should contain theSameElementsAs (
        List(
          Address(1, "123 Someplace", 1001),
          Address(1, "678 Blah", 2002)
        )
      )
    }

    "4-comp-stereo-twoschema" in WithContext[Prefix, `4-comp-stereo-twoschema`].run { ctx =>
      import `4-comp-stereo-twoschema-lib`.common._
      import `4-comp-stereo-twoschema-lib`.public._
      import ctx._

      (ctx.run(PersonDao.alphaPerson.filter(_.age > 11))) should contain theSameElementsAs (
        List(
          Person(1, "Joe", "Bloggs", 22, 55L, "Wonkles"),
          Person(2, "Jack", "Ripper", 33, 66L, "Ginkles")
        )
      )
      (ctx.run(AddressDao.publicAddress.filter(_.personFk == 1))) should contain theSameElementsAs (
        List(
          Address(1, "123 Someplace", 1001),
          Address(1, "678 Blah", 2002)
        )
      )
    }

    "5 - non-stereotyped multiple schemas" in WithContext[Prefix, `5-comp-non-stereo-allschema`].run { ctx =>
      import `5-comp-non-stereo-allschema-lib`.alpha._
      import `5-comp-non-stereo-allschema-lib`.public._
      import ctx._

      (ctx.run(ctx.AlphaSchema.PersonDao.query.filter(_.age > 11))) should contain theSameElementsAs (
        List(
          Person(1, "Joe", "Bloggs", 22, "blah", 55, "Wonkles"),
          Person(2, "Jack", "Ripper", 33, "blah", 66, "Ginkles")
        )
      )
      (ctx.run(ctx.PublicSchema.AddressDao.query.filter(_.personFk == 1))) should contain theSameElementsAs (
        List(
          Address(1, "123 Someplace", 1001),
          Address(1, "678 Blah", 2002)
        )
      )
    }
  }
}
