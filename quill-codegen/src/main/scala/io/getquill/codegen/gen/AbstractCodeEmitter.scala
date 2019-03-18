package io.getquill.codegen.gen

import io.getquill.codegen.model._
import io.getquill.codegen.util.StringUtil.{ indent, _ }

case class EmitterSettings[TableMeta, ColumnMeta](
  caseClassTables:   Seq[TableStereotype[TableMeta, ColumnMeta]],
  querySchemaTables: Seq[TableStereotype[TableMeta, ColumnMeta]],
  codeWrapper:       CodeWrapper
)

abstract class AbstractCodeEmitter {
  def apply = code
  def code: String

  abstract class AbstractCaseClassGen {
    def code: String
    def rawCaseClassName: String
    def actualCaseClassName: String

    abstract class AbstractMemberGen {
      def code: String = s"${fieldName}: ${actualType}"
      def rawType: String
      def actualType: String
      def rawFieldName: String
      def fieldName: String
    }
  }

  abstract class AbstractCombinedTableSchemasGen {
    def code: String
    def imports: String

    abstract class AbstractQuerySchemaGen {
      def code: String
      def tableName: String
      def schemaName: Option[String]
      def fullTableName = schemaName.map(_ + ".").getOrElse("") + tableName
      def rawCaseClassName: String
      def actualCaseClassName: String

      abstract class AbstractQuerySchemaMappingGen {
        def code: String
        def rawFieldName: String
        def fieldName: String
        def databaseColumn: String
      }

    }

  }
}

trait ObjectGen {
  def objectName: Option[String]
  def surroundByObject(innerCode: String) =
    objectName match {
      case None => innerCode
      case Some(objectNameActual) =>
        s"""
object ${objectNameActual} {
  ${indent(innerCode)}
}
""".stripMargin.trimFront
    }
}

trait PackageGen {
  def packagePrefix: String

  def packageName: Option[String] = codeWrapper match {
    case NoWrapper                  => None
    case PackageHeader(packageName) => Some(packageName)
    case PackageObject(packageName) => Some(packageName)
    case SimpleObject(packageName)  => Some(packageName)
  }

  def codeWrapper: CodeWrapper
  def surroundByPackage(innerCode: String) =
    codeWrapper match {
      case NoWrapper => innerCode
      case PackageHeader(packageName) => {
        val out = if (packagePrefix.trim != "") s"package ${packagePrefix}.${packageName}\n\n"
        else ""
        out + innerCode
      }

      case PackageObject(packageName) =>
        s"""
package object ${packageName} {
  ${indent(innerCode)}
}
""".stripMargin.trimFront

      case SimpleObject(packageName) =>
        s"""
object ${packageName} {
  ${indent(innerCode)}
}
""".stripMargin.trimFront
    }
}
