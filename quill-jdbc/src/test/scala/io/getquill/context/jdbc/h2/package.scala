package io.getquill.context.jdbc

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

package object h2 {

  object testContext extends H2JdbcContext(Literal, "testH2DB") with TestEntities with TestEncoders with TestDecoders

}
