package zio.quill.context.jdbc

import zio.quill._
import zio.quill.context.sql.{TestDecoders, TestEncoders}

package object h2 {

  object testContext extends H2JdbcContext(Literal, "testH2DB") with TestEntities with TestEncoders with TestDecoders

}
