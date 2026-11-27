package zio.quill.codegen.jdbc.model

import java.sql.Connection

import zio.quill.codegen.model.{JdbcColumnMeta, JdbcTableMeta, RawSchema}

import scala.reflect.ClassTag

object JdbcTypes {
  type JdbcConnectionMaker   = () => Connection
  type JdbcSchemaReader      = (JdbcConnectionMaker) => Seq[RawSchema[JdbcTableMeta, JdbcColumnMeta]]
  type JdbcTyper             = JdbcTypeInfo => Option[ClassTag[_]]
  type JdbcQuerySchemaNaming = JdbcTableMeta => String
}
