package io.getquill.codegen.gen

import io.getquill.codegen.model.Stereotyper.Namespacer
import io.getquill.codegen.model.UnrecognizedTypeStrategy
import io.getquill.codegen.model._

import scala.reflect.ClassTag

trait HasBasicMeta {
  type TableMeta <: BasicTableMeta
  type ColumnMeta <: BasicColumnMeta
}

trait CodeGeneratorComponents extends HasBasicMeta with QuerySchemaNaming {

  type ConnectionMaker
  type TypeInfo
  type Typer = TypeInfo => Option[ClassTag[_]]

  type SchemaReader      = (ConnectionMaker) => Seq[RawSchema[TableMeta, ColumnMeta]]
  type QuerySchemaNaming = TableMeta => String
  type ColumnGetter      = ColumnMeta => String

  def defaultExcludedSchemas = Set[String]()
  def querySchemaImports     = ""
  def nameParser: NameParser
  def unrecognizedTypeStrategy: UnrecognizedTypeStrategy
  def typer: Typer
  def schemaReader: SchemaReader
  def packagingStrategy: PackagingStrategy

  /**
   * When defining your query schema object, this will name the method which
   * produces the query schema. It will be named <code>query</code> by default
   * so if you are doing Table Stereotyping, be sure it's something reasonable
   * like <code>(ts) => ts.tableName.snakeToLowerCamel</code>
   *
   * <pre>{@code case class Person(firstName:String, lastName:String, age:Int)
   *
   * object Person { // The method will be 'query' by default which is good if
   * you are not stereotyping. def query = querySchema[Person](...) } }</pre>
   *
   * Now let's take an example where you have a database that has two schemas
   * <code>ALPHA</code> and <code>BRAVO</code>, each with a table called Person
   * and you want to stereotype the two schemas into one table case class. In
   * this case you have to be sure that memberNamer is something like <code>(ts)
   * \=> ts.tableName.snakeToLowerCamel</code> so you'll get a different method
   * for every querySchema.
   *
   * <pre>{@code case class Person(firstName:String, lastName:String, age:Int)
   *
   * object Person { // Taking ts.tableName.snakeToLowerCamel will ensure each
   * one has a different name. Otherwise // all of them will be 'query' which
   * will result in a compile error. def alphaPerson =
   * querySchema[Person]("ALPHA.PERSON", ...) def bravoPerson =
   * querySchema[Person]("BRAVO.PERSON", ...) } }</pre>
   */
  def querySchemaNaming: QuerySchemaNaming = `"query"`

  def filter(tc: RawSchema[TableMeta, ColumnMeta]): Boolean

  def defaultNamespace: String = "schema"

  /**
   * Take the schema of a table and convert it to something a object/package
   * etc... can actually be named as
   * i.e. a namespace.
   */
  def namespacer: Namespacer[TableMeta]
}
