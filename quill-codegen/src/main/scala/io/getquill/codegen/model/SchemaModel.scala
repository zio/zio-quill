package io.getquill.codegen.model

import java.sql.ResultSet

final case class RawSchema[T, C](table: T, columns: Seq[C])

trait BasicTableMeta {
  def tableSchema: Option[String]
  def tableName: String
}

trait BasicColumnMeta {
  def columnName: String
}

final case class JdbcTableMeta(
  tableCat: Option[String],
  tableSchema: Option[String],
  tableName: String,
  tableType: Option[String]
) extends BasicTableMeta

object JdbcTableMeta {
  def fromResultSet(rs: ResultSet): JdbcTableMeta = JdbcTableMeta(
    tableCat = Option(rs.getString("TABLE_CAT")),
    tableSchema = Option(rs.getString("TABLE_SCHEM")),
    tableName = rs.getString("TABLE_NAME"),
    tableType = Option(rs.getString("TABLE_TYPE"))
  )
}

final case class JdbcColumnMeta(
  tableCat: Option[String],
  tableSchema: Option[String],
  tableName: String,
  columnName: String,
  dataType: Int,
  typeName: String,
  nullable: Int,
  size: Int
) extends BasicColumnMeta

object JdbcColumnMeta {
  def fromResultSet(rs: ResultSet): JdbcColumnMeta =
    JdbcColumnMeta(
      tableCat = Option(rs.getString("TABLE_CAT")),
      tableSchema = Option(rs.getString("TABLE_SCHEM")),
      tableName = rs.getString("TABLE_NAME"),
      columnName = rs.getString("COLUMN_NAME"),
      dataType = rs.getInt("DATA_TYPE"),
      typeName = rs.getString("TYPE_NAME"),
      nullable = rs.getInt("NULLABLE"),
      size = rs.getInt("COLUMN_SIZE")
    )
}

object SchemaModel {}
