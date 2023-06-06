package io.getquill.codegen.model

import scala.reflect.ClassTag

/**
 * Represents a top-level entity to be processed by the code generator. A table
 * is considered to be properly 'stereotyped' if it is either the only table
 * with a given name or, if it has been combined with all other identically
 * named tables (in the same schema) that we wish to combine it with.
 */
case class TableStereotype[TableMeta, ColumnMeta](
  table: TableFusion[TableMeta],
  columns: Seq[ColumnFusion[ColumnMeta]]
)

case class TableFusion[TableMeta](
  namespace: String,
  name: String,
  meta: Seq[TableMeta]
)

case class ColumnFusion[ColumnMeta](
  name: String,
  dataType: ClassTag[_],
  nullable: Boolean,
  meta: Seq[ColumnMeta]
)
