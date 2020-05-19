package io.getquill

import io.getquill.context.sql.PeopleSpec
import io.getquill.context.zio.ZioJdbcContext

trait PeopleZioSpec extends PeopleSpec with ZioSpec {

  val context: ZioJdbcContext[_, _]
  import context._

  val `Ex 11 query` = quote(query[Person])
  val `Ex 11 expected` = peopleEntries
}
