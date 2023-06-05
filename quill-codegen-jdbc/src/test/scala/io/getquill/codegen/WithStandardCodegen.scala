package io.getquill.codegen

import java.sql.DriverManager

import io.getquill.codegen.jdbc.gen.JdbcGeneratorBase
import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcQuerySchemaNaming
import io.getquill.codegen.model.Stereotyper.Namespacer
import io.getquill.codegen.model._
import io.getquill.codegen.util.SchemaConfig
import io.getquill.codegen.util.StringUtil._

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
