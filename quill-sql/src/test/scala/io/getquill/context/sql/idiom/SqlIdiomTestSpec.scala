package io.getquill.context.sql.idiom

import io.getquill.ReturnAction.ReturnColumns
import io.getquill.{Action, Literal, MirrorSqlDialectWithReturnMulti, Ord, PostgresDialect, Query, SqlMirrorContext}
import io.getquill.context.mirror.Row

object SqlIdiomTestSpec {

  case class TestEntity(s: String, i: Int, l: Long, o: Option[Int], b: Boolean)
  case class TestEntity2(s: String, i: Int, l: Long, o: Option[Int])

  val ctx = new SqlMirrorContext(PostgresDialect, Literal)
  import ctx._

  val qr1 = quote {
    query[TestEntity]
  }
  val qr2 = quote {
    query[TestEntity2]
  }

  def main(args: Array[String]): Unit = {
//    System.setProperty("quill.trace.enabled", "true")
//    System.setProperty("quill.trace.color", "true")
//    System.setProperty("quill.trace.quat", "full")
//    System.setProperty("quill.trace.types", "all")
//    io.getquill.util.Messages.resetCache() //

    // val q = quote {
    //  for {
    //    v1 <- qr1.map(x => x.i).distinct
    //    v2 <- qr2.filter(_.i == v1)
    //  } yield (v1, v2)
    // }.dynamic
    // println(ctx.run(q).string)
    // "SELECT i._1, x1.s, x1.i, x1.l, x1.o FROM (SELECT DISTINCT i.i AS _1 FROM TestEntity i) AS i, TestEntity2 x1 WHERE x1.i = i._1"

//    val q = quote {
//      qr1.map(x => x.i).nested
//    }.dynamic
//
//    println(run(q).string) //

    val q = quote {
      for {
        (a, b) <- qr1 join qr2 on ((a, b) => a.i == b.i)
        c      <- qr1 rightJoin (c => c.i == a.i)
      } yield (a, b, c.map(c => c.i))
    }.dynamic

    println(run(q).string)
  }

}
