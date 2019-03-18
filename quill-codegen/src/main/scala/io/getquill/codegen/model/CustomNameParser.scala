package io.getquill.codegen.model

import io.getquill.codegen.util.StringUtil._

sealed trait NameParser {
  def generateQuerySchemas: Boolean
  def parseColumn(cm: JdbcColumnMeta): String
  def parseTable(tm: JdbcTableMeta): String
}

trait LiteralNames extends NameParser {
  def generateQuerySchemas = false
  def parseColumn(cm: JdbcColumnMeta): String = cm.columnName
  def parseTable(tm: JdbcTableMeta): String = tm.tableName.capitalize
}
trait SnakeCaseNames extends NameParser {
  def generateQuerySchemas = false
  def parseColumn(cm: JdbcColumnMeta): String = cm.columnName.snakeToLowerCamel
  def parseTable(tm: JdbcTableMeta): String = tm.tableName.snakeToUpperCamel
}

object LiteralNames extends LiteralNames
object SnakeCaseNames extends SnakeCaseNames

case class CustomNames(
  columnParser: JdbcColumnMeta => String = cm => cm.columnName.snakeToLowerCamel,
  tableParser:  JdbcTableMeta => String  = tm => tm.tableName.snakeToUpperCamel
) extends NameParser {
  def generateQuerySchemas = true
  def parseColumn(cm: JdbcColumnMeta): String = columnParser(cm)
  def parseTable(tm: JdbcTableMeta): String = tableParser(tm)
}

case class SnakeCaseCustomTable(
  tableParser: JdbcTableMeta => String
) extends NameParser {
  def generateQuerySchemas = true
  def parseColumn(cm: JdbcColumnMeta): String = cm.columnName.snakeToLowerCamel
  def parseTable(tm: JdbcTableMeta): String = tableParser(tm)
}
