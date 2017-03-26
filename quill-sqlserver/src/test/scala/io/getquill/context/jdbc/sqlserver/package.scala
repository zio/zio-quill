package io.getquill.context.jdbc

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.SqlServerJdbcContext

package object sqlserver {

  object testContext extends SqlServerJdbcContext[Literal]("testSqlServerDB") with TestEntities with TestEncoders with TestDecoders
}