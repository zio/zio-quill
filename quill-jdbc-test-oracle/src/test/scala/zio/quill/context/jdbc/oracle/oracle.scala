package zio.quill.context.jdbc

import zio.quill._
import zio.quill.context.sql.{TestDecoders, TestEncoders}

package object oracle {

  object testContext
      extends OracleJdbcContext(Literal, "testOracleDB")
      with TestEntities
      with TestEncoders
      with TestDecoders

}
