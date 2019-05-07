package io.getquill.codegen.model

import io.getquill.codegen.util.ScalaLangUtil.escape

trait ByName {
  def prefix: String
  def namespaceMaker: PackageNamingStrategy.NamespaceMaker
  def byName(table: TableStereotype[_, _]) = namespaceMaker(table)
}

sealed trait CodeWrapper
case class PackageHeader(packageName: String) extends CodeWrapper
case class PackageObject(packageName: String) extends CodeWrapper
case class SimpleObject(packageName: String) extends CodeWrapper
case object NoWrapper extends CodeWrapper

sealed trait FileNamingStrategy

/**
 * Name each package by the name of the table being generated.
 * If multiple tables are going to the generator, need to choose which one to use,
 * most likely the 1st. Typically used in ByPackage strategies. This is
 * the most common use-case.
 */
case object ByTable extends FileNamingStrategy

/**
 * Use this when generating package object so the filename will always be 'package.scala'
 */
case object ByPackageObjectStandardName extends FileNamingStrategy

/**
 * Typically used when multiple Tables are grouped into the same schema. but a package object is not used.
 */
case object ByPackageName extends FileNamingStrategy
case object ByDefaultName extends FileNamingStrategy

case class BySomeTableData[Gen](val namer: Gen => java.nio.file.Path)(implicit val tt: scala.reflect.runtime.universe.TypeTag[Gen]) extends FileNamingStrategy

trait CaseClassNaming[TableMeta, ColumnMeta] {
  def tableColumns: TableStereotype[TableMeta, ColumnMeta]
  def rawCaseClassName: String = tableColumns.table.name
  def actualCaseClassName: String = escape(rawCaseClassName)
}

trait FieldNaming[ColumnMeta] {
  def column: ColumnFusion[ColumnMeta]
  def rawFieldName: String = column.name
  def fieldName = escape(rawFieldName)
}
