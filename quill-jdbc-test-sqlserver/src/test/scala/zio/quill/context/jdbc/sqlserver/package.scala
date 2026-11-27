package zio.quill.context.jdbc

import zio.quill._
import zio.quill.context.sql.{TestDecoders, TestEncoders}

package object sqlserver {

  object testContext
      extends SqlServerJdbcContext(Literal, "testSqlServerDB")
      with TestEntities
      with TestEncoders
      with TestDecoders
}
