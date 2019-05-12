package io.getquill.codegen.model

import io.getquill.codegen.gen.HasBasicMeta
import io.getquill.codegen.model.Stereotyper.Namespacer

object Stereotyper {
  type Namespacer[TableMeta] = TableMeta => String
  type Expresser[TableMeta, ColumnMeta] = (RawSchema[TableMeta, ColumnMeta]) => TableStereotype[TableMeta, ColumnMeta]
  type Fuser[TableMeta, ColumnMeta] = (Seq[TableStereotype[TableMeta, ColumnMeta]]) => TableStereotype[TableMeta, ColumnMeta]
}

trait Stereotyper extends HasBasicMeta {
  def namespacer: Namespacer[JdbcTableMeta]
  def nameParser: NameParser
  def stereotype(schemas: Seq[RawSchema[TableMeta, ColumnMeta]]): Seq[TableStereotype[TableMeta, ColumnMeta]]
}
