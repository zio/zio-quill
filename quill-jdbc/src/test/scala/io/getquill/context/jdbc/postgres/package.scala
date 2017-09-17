package io.getquill.context.jdbc

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

package object postgres {

  object testContext extends PostgresJdbcContext(Literal, "testPostgresDB") with TestEntities with TestEncoders with TestDecoders

}
