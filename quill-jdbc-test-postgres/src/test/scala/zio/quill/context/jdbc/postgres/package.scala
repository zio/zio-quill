package zio.quill.context.jdbc

import zio.quill._
import zio.quill.context.sql.{TestDecoders, TestEncoders}

package object postgres {

  object testContext
      extends PostgresJdbcContext(Literal, "testPostgresDB")
      with TestEntities
      with TestEncoders
      with TestDecoders

}
