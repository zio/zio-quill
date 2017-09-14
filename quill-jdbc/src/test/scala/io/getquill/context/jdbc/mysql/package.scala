package io.getquill.context.jdbc

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

package object mysql {

  object testContext extends MysqlJdbcContext(Literal, "testMysqlDB") with TestEntities with TestEncoders with TestDecoders

}
