package zio.quill.codegen

import java.sql.DriverManager

import zio.quill.codegen.jdbc.gen.JdbcGeneratorBase
import zio.quill.codegen.jdbc.model.JdbcTypes.JdbcQuerySchemaNaming
import zio.quill.codegen.model.Stereotyper.Namespacer
import zio.quill.codegen.model._
import zio.quill.codegen.util.SchemaConfig
import zio.quill.codegen.util.StringUtil._

trait WithStandardCodegen {

  def defaultNamespace: String

  def standardCodegen(
    schemaConfig: SchemaConfig,
    tableFilter: RawSchema[JdbcTableMeta, JdbcColumnMeta] => Boolean = _ => true,
    entityNamingStrategy: NameParser = LiteralNames,
    entityNamespacer: Namespacer[JdbcTableMeta] = ts => ts.tableSchema.getOrElse(defaultNamespace),
    entityMemberNamer: JdbcQuerySchemaNaming = ts => ts.tableName.snakeToLowerCamel
  ) =
    new JdbcGeneratorBase(() => {
      DriverManager.getConnection(
        s"jdbc:h2:mem:sample;INIT=RUNSCRIPT FROM 'classpath:h2_schema_precursor.sql'\\;RUNSCRIPT FROM 'classpath:${schemaConfig.fileName}'",
        "sa",
        "sa"
      )
    }) {
      override def filter(tc: RawSchema[TableMeta, ColumnMeta]): Boolean = super.filter(tc) && tableFilter(tc)
      override def nameParser: NameParser                                = entityNamingStrategy
      override val namespacer: Namespacer[TableMeta]                     = entityNamespacer
      override def querySchemaNaming: QuerySchemaNaming                  = entityMemberNamer
      override def packagingStrategy: PackagingStrategy                  = super.packagingStrategy
    }
}
