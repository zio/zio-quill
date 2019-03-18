package io.getquill.codegen.jdbc.gen

import io.getquill.codegen.gen.CodeGeneratorComponents
import io.getquill.codegen.jdbc.DatabaseTypes.DatabaseType
import io.getquill.codegen.jdbc.model.JdbcTypes._
import io.getquill.codegen.jdbc.model.{ DefaultJdbcTyper, JdbcTypeInfo }
import io.getquill.codegen.model._

trait JdbcCodeGeneratorComponents extends CodeGeneratorComponents {

  type TableMeta = JdbcTableMeta
  type ColumnMeta = JdbcColumnMeta
  type ConnectionMaker = JdbcConnectionMaker
  type TypeInfo = JdbcTypeInfo

  override type Typer = JdbcTyper
  override type SchemaReader = JdbcSchemaReader
  override type QuerySchemaNaming = JdbcQuerySchemaNaming

  def packagePrefix: String

  def nameParser: NameParser = LiteralNames

  override def defaultExcludedSchemas = Set("information_schema", "performance_schema", "sys", "mysql")

  /**
   * When the Jdbc Typer tries to figure out which Scala/Java objects to use for
   * which JDBC type (e.g. use String for Varchar(...), Long for bigint etc...),
   * what do we do when we discover a JDBC type which we cannot translate (e.g. blob which is
   * currently not supported by quill). The simplest thing to do is to skip the column.
   */
  def unrecognizedTypeStrategy: UnrecognizedTypeStrategy = SkipColumn

  /**
   * When the Jdbc Typer sees a `NUMERIC` jdbc column, should it use int/long
   * instead of `BigInteger` if the scale allows?
   */
  def numericPreference: NumericPreference = UseDefaults

  /** Retrieve type of database we are using from JDBC. Needed here for metadata fetch **/
  def databaseType: DatabaseType

  def typer: Typer = new DefaultJdbcTyper(unrecognizedTypeStrategy, numericPreference)
  def schemaReader: SchemaReader = new DefaultJdbcSchemaReader(databaseType)
  def packagingStrategy: PackagingStrategy = PackagingStrategy.ByPackageHeader.TablePerFile(packagePrefix)
}
