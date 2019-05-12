package io.getquill.codegen.jdbc.model

import java.sql.DatabaseMetaData

import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcTyper
import io.getquill.codegen.model.Stereotyper.{ Expresser, Namespacer }
import io.getquill.codegen.model._

class JdbcExpresser(
  nameParser: NameParser,
  namespacer: Namespacer[JdbcTableMeta],
  typer:      JdbcTyper
) extends Expresser[JdbcTableMeta, JdbcColumnMeta] {
  override def apply(schema: RawSchema[JdbcTableMeta, JdbcColumnMeta]): TableStereotype[JdbcTableMeta, JdbcColumnMeta] = {
    val tableModel = TableFusion(
      namespacer(schema.table),
      nameParser.parseTable(schema.table),
      Seq(schema.table)
    )
    val columnModels =
      schema.columns
        .map(desc => (desc, typer(JdbcTypeInfo(desc))))
        .filter { case (desc, tpe) => tpe.isDefined }
        .map {
          case (desc, tpe) =>
            ColumnFusion(
              nameParser.parseColumn(desc),
              tpe.get, // is safe to do this at this point since we filtered out nones
              desc.nullable != DatabaseMetaData.columnNoNulls,
              Seq(desc)
            )
        }

    TableStereotype(tableModel, columnModels)
  }
}
