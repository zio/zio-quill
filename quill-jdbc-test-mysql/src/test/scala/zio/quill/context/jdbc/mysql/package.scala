package zio.quill.context.jdbc

import zio.quill._
import zio.quill.context.sql.{TestDecoders, TestEncoders}

package object mysql {

  object testContext
      extends MysqlJdbcContext(Literal, "testMysqlDB")
      with TestEntities
      with TestEncoders
      with TestDecoders

}
