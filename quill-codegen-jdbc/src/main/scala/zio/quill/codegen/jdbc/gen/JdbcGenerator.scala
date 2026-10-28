package zio.quill.codegen.jdbc.gen

import zio.quill.codegen.gen.Generator
import zio.quill.codegen.jdbc.DatabaseTypes.{DatabaseType, MySql, SqlServer}
import zio.quill.codegen.jdbc.model.JdbcStereotyper
import zio.quill.codegen.jdbc.model.JdbcTypes.JdbcConnectionMaker
import zio.quill.codegen.jdbc.util.DiscoverDatabaseType
import zio.quill.codegen.model.Stereotyper.Namespacer
import zio.quill.codegen.model.{JdbcColumnMeta, JdbcTableMeta, RawSchema}
import zio.quill.codegen.util.StringUtil._

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
