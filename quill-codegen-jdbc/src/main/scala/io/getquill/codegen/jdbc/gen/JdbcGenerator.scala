package io.getquill.codegen.jdbc.gen

import io.getquill.codegen.gen.Generator
import io.getquill.codegen.jdbc.DatabaseTypes.{DatabaseType, MySql, SqlServer}
import io.getquill.codegen.jdbc.model.JdbcStereotyper
import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcConnectionMaker
import io.getquill.codegen.jdbc.util.DiscoverDatabaseType
import io.getquill.codegen.model.Stereotyper.Namespacer
import io.getquill.codegen.model.{JdbcColumnMeta, JdbcTableMeta, RawSchema}
import io.getquill.codegen.util.StringUtil._

class JdbcGeneratorBase(val connectionMakers: Seq[JdbcConnectionMaker], val packagePrefix: String)
    extends JdbcGenerator
    with JdbcCodeGeneratorComponents
    with JdbcStereotyper {

  override type TableMeta  = JdbcTableMeta
  override type ColumnMeta = JdbcColumnMeta

  def this(connectionMaker: JdbcConnectionMaker) = this(Seq(connectionMaker), "")
}

trait JdbcGenerator extends Generator { this: JdbcCodeGeneratorComponents with JdbcStereotyper =>
  val connectionMakers: Seq[JdbcConnectionMaker]
  val databaseType: DatabaseType = DiscoverDatabaseType.apply(connectionMakers.head)
  val columnGetter               = (cm: ColumnMeta) => cm.columnName

  override def filter(tc: RawSchema[JdbcTableMeta, JdbcColumnMeta]): Boolean =
    databaseType match {
      case MySql => !tc.table.tableCat.existsInSetNocase(defaultExcludedSchemas.toList: _*)
      case _     => !tc.table.tableSchema.existsInSetNocase(defaultExcludedSchemas.toList: _*)
    }

  override def namespacer: Namespacer[TableMeta] = databaseType match {
    case MySql | SqlServer => tm => tm.tableCat.map(_.snakeToLowerCamel).getOrElse(defaultNamespace)
    case _                 => tm => tm.tableSchema.orElse(tm.tableCat).map(_.snakeToLowerCamel).getOrElse(defaultNamespace)
  }
}
