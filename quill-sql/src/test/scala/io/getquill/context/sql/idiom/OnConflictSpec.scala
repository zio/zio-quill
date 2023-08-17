package io.getquill.context.sql.idiom

import io.getquill.{Insert, Quoted, TestEntities}
import io.getquill.base.Spec
import io.getquill.context.sql.SqlContext

trait OnConflictSpec extends Spec {
  val ctx: SqlContext[_, _] with TestEntities
  import ctx._

  lazy val e = TestEntity("s1", 1, 1, None, true)

  def `onConflict with query`(fun: Quoted[Insert[TestEntity]] => Unit) =
    "with query" - fun(quote(query[TestEntity].insertValue(lift(e))))
  def `onConflict with schemaMeta`(fun: Quoted[Insert[TestEntity]] => Unit) = "with schemaMeta" - fun(quote {
    implicit val teSchemaMeta = testEntitySchemaMeta
    query[TestEntity].insertValue(lift(e))
  })

  def `onConflict with querySchema`(fun: Quoted[Insert[TestEntity]] => Unit) =
    "with querySchema" - fun(quote(testEntityQuerySchema.insertValue(lift(e))))

  def `onConflict with all`(fun: Quoted[Insert[TestEntity]] => Unit) = {
    `onConflict with query`(fun)
    `onConflict with schemaMeta`(fun)
    `onConflict with querySchema`(fun)
  }

  def del = quote(query[TestEntity].delete)

  def `no target - ignore`(ins: Quoted[Insert[TestEntity]]) = quote {
    ins.onConflictIgnore
  }
  def `cols target - ignore`(ins: Quoted[Insert[TestEntity]]) = quote {
    ins.onConflictIgnore(_.i)
  }
  def `no target - update`(ins: Quoted[Insert[TestEntity]]) = quote {
    ins.onConflictUpdate((t, e) => t.l -> (t.l + e.l) / 2, _.s -> _.s)
  }
  def `cols target - update`(ins: Quoted[Insert[TestEntity]]) = quote {
    ins.onConflictUpdate(_.i, _.s)((t, e) => t.l -> (t.l + e.l) / 2, _.s -> _.s)
  }
  def insBatch = quote(liftQuery(List(e, TestEntity("s2", 1, 2L, Some(1), true))))

  def `no target - ignore batch` = quote {
    insBatch.foreach(query[TestEntity].insertValue(_).onConflictIgnore)
  }
}
