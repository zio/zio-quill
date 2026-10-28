package zio.quill.context.jdbc

import zio.quill.context.sql.{TestDecoders, TestEncoders}
import zio.quill.{Literal, SqliteJdbcContext, TestEntities}

package object sqlite {

  object testContext
      extends SqliteJdbcContext(Literal, "testSqliteDB")
      with TestEntities
      with TestEncoders
      with TestDecoders

}
