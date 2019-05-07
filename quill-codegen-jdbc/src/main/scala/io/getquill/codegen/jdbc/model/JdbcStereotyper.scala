package io.getquill.codegen.jdbc.model

import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcTyper
import io.getquill.codegen.model._

trait JdbcStereotyper extends Stereotyper {
  type TableMeta = JdbcTableMeta
  type ColumnMeta = JdbcColumnMeta
  def typer: JdbcTyper

  import Stereotyper._

  def fuser: Fuser[JdbcTableMeta, JdbcColumnMeta] =
    new DefaultFuser[JdbcTableMeta, JdbcColumnMeta]
  def expresser: Expresser[JdbcTableMeta, JdbcColumnMeta] =
    new JdbcExpresser(nameParser, namespacer, typer)

  type JdbcStereotypingFunction = (Seq[RawSchema[JdbcTableMeta, JdbcColumnMeta]]) => Seq[TableStereotype[JdbcTableMeta, JdbcColumnMeta]]

  class JdbcStereotypingHelper extends JdbcStereotypingFunction {
    override def apply(schemaTables: Seq[RawSchema[JdbcTableMeta, JdbcColumnMeta]]): Seq[TableStereotype[JdbcTableMeta, JdbcColumnMeta]] = {

      // convert description objects into expression objects
      val expressionTables = schemaTables.map(expresser(_))

      // group by the namespaces
      val groupedExpressions =
        expressionTables.groupBy(tc => (tc.table.namespace, tc.table.name))

      // unify expression objects
      val unified = groupedExpressions.map({ case (_, seq) => fuser(seq) })

      unified.toSeq
    }
  }

  def stereotype(schemas: Seq[RawSchema[JdbcTableMeta, JdbcColumnMeta]]): Seq[TableStereotype[JdbcTableMeta, JdbcColumnMeta]] =
    new JdbcStereotypingHelper().apply(schemas)
}
