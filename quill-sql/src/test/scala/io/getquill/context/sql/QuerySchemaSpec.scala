package io.getquill.context.sql

import io.getquill.{ Literal, PostgresDialect, Spec, SqlMirrorContext, TestEntities }

class QuerySchemaSpec extends Spec {

  val ctx = new SqlMirrorContext(PostgresDialect, Literal) with TestEntities
  import ctx._

  "OnConflict Should work correct with" - {
    case class Person(id: Long, name: String)
    val p = Person(1, "Joe")

    "querySchema" in {
      val q = quote {
        querySchema[Person]("thePerson", _.id -> "theId", _.name -> "theName")
          .insert(lift(p))
          .onConflictUpdate(_.id)(_.name -> _.name)
      }
      ctx.run(q).string mustEqual "INSERT INTO thePerson AS t (theId,theName) VALUES (?, ?) ON CONFLICT (theId) DO UPDATE SET theName = EXCLUDED.theName"
    }

    "schemaMeta" in {
      implicit val personSchemaMeta = schemaMeta[Person]("thePerson", _.id -> "theId", _.name -> "theName")
      val q = quote {
        query[Person]
          .insert(lift(p))
          .onConflictUpdate(_.id)(_.name -> _.name)
      }
      ctx.run(q).string mustEqual "INSERT INTO thePerson AS t (theId,theName) VALUES (?, ?) ON CONFLICT (theId) DO UPDATE SET theName = EXCLUDED.theName"
    }
  }
}
