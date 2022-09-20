package io.getquill.codegen.jdbc

import java.nio.file.Paths

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import io.getquill.codegen.gen.EmitterSettings
import io.getquill.codegen.jdbc.gen.JdbcGeneratorBase
import io.getquill.codegen.jdbc.model.JdbcTypes.JdbcConnectionMaker
import io.getquill.codegen.model.{BySomeTableData, JdbcColumnMeta, JdbcTableMeta, PackagingStrategy}
import io.getquill.codegen.util.StringUtil._
import io.getquill.util.LoadConfig
import javax.sql.DataSource

/**
 * This generator generates a query schema trait which can be composed with a
 * custom context that you create in your client code (called MySchemaExtensions
 * below because it extends and existing quill context with your query schemas).
 * Here is what that looks like:
 *
 * <pre>{@code case class Person(firstName:String, lastName:String, age:Int)
 * case class Address(...)
 *
 * trait PublicExtensions[+Idiom <: io.getquill.idiom.Idiom, Naming <:
 * io.getquill.NamingStrategy] { this:io.getquill.context.Context[Idiom, Naming]
 * \=>
 *
 * object PersonDao { def query = querySchema[Person](...) } object AddressDao {
 * def query = querySchema[Address](...) } }
 *
 * // Then when declaring your context: import io.getquill._ object
 * MyCustomContext extends SqlMirrorContext[H2Dialect, Literal](H2Dialect,
 * Literal) with PublicExtensions[H2Dialect, Literal]
 *
 * }</pre>
 *
 * <h2>A Note on Stereotyping</h2> Stereotyping using the ComposeableTraitsGen
 * is done in the following manner. Firstly, extend a ComposeableTraitsGen and
 * add the Namespacer of your choice. Let's take the <code>alpha</code> and
 * <code>bravo</code> namespaces and combine them into a <code>common</code>
 * namespace. Also be sure to set the memberNamer correctly so that the
 * different querySchemas generated won't all be called '.query' in the
 * <code>Common</code> object.
 *
 * <pre> class MyStereotypingGen(...) extends ComposeableTraitsGen(...) {
 * override def namespacer: Namespacer = ts=> if(ts.tableSchem == "alpha" ||
 * ts.tableSchem == "bravo") "common" else ts.tableSchem
 *
 * override def memberNamer: MemberNamer = ts => ts.tableName.snakeToLowerCamel
 * } </pre>
 *
 * The following schema should result: <pre> trait CommonExtensions[+Idiom <:
 * io.getquill.idiom.Idiom, Naming <: io.getquill.NamingStrategy] {
 * this:io.getquill.context.Context[Idiom, Naming] =>
 *
 * object PersonDao { // you don't want each of these to be called 'query' so
 * choose an appropriate memberNamer def alphaPerson = querySchema[Person](...)
 * def bravoPerson = querySchema[Person](...) } }
 *
 * trait PublicExtensions[+Idiom <: io.getquill.idiom.Idiom, Naming <:
 * io.getquill.NamingStrategy] { this:io.getquill.context.Context[Idiom, Naming]
 * \=>
 *
 * object AddressDao { def publicAddress = querySchema[Address](...) } } </pre>
 *
 * <h2>When DAO Objects Collide</h2> Now when you are trying to generate schemas
 * which are not being stereotyped but have equivalent table names for example:
 * <pre> trait AlphaExtensions[+Idiom <: io.getquill.idiom.Idiom, Naming <:
 * io.getquill.NamingStrategy] { this:io.getquill.context.Context[Idiom, Naming]
 * \=>
 *
 * object PersonDao { def query = querySchema[Person](...) } }
 *
 * trait BravoExtensions[+Idiom <: io.getquill.idiom.Idiom, Naming <:
 * io.getquill.NamingStrategy] { this:io.getquill.context.Context[Idiom, Naming]
 * \=>
 *
 * object PersonDao { def query = querySchema[Person](...) } }
 *
 * // Invalid because MyCustomContext has a PersonDao from AlphaExtensions and
 * and a PersonDao from BravoExtensions which collide. object MyCustomContext
 * extends SqlMirrorContext[H2Dialect, Literal](H2Dialect, Literal) with
 * AlphaExtensions[H2Dialect, Literal] with BravoExtensions[H2Dialect, Literal]
 * </pre>
 *
 * You will not be able to append these two traits to the same quill context
 * because the PersonDao inside alpha and bravo will collide inside of
 * MyCustomContext. Use the parameter <code>nestedTrait=true</code> in order to
 * get around this.
 *
 * <pre> trait AlphaExtensions[+Idiom <: io.getquill.idiom.Idiom, Naming <:
 * io.getquill.NamingStrategy] { this:io.getquill.context.Context[Idiom, Naming]
 * \=>
 *
 * trait AlphaSchema { object PersonDao { def query = querySchema[Person](...) }
 * } }
 *
 * trait BravoExtensions[+Idiom <: io.getquill.idiom.Idiom, Naming <:
 * io.getquill.NamingStrategy] { this:io.getquill.context.Context[Idiom, Naming]
 * \=>
 *
 * trait BravoSchema { object PersonDao { def query = querySchema[Person](...) }
 * } }
 *
 * // Since PersonDao is inside MyCustomContext.alpha and MyCustomContext.bravo
 * as opposed to MyCustomContext // there will be no collision. object
 * MyCustomContext extends SqlMirrorContext[H2Dialect, Literal](H2Dialect,
 * Literal) with AlphaExtensions[H2Dialect, Literal] with
 * BravoExtensions[H2Dialect, Literal] </pre>
 */

class ComposeableTraitsJdbcCodegen(
  override val connectionMakers: Seq[JdbcConnectionMaker],
  override val packagePrefix: String = "",
  val nestedTrait: Boolean = false
) extends JdbcGeneratorBase(connectionMakers, packagePrefix) {
  import io.getquill.codegen.util.StringUtil.indent

  def this(
    connectionMaker: JdbcConnectionMaker,
    packagePrefix: String,
    nestedTrait: Boolean
  ) = this(Seq(connectionMaker), packagePrefix, nestedTrait)

  /**
   * Simple convenience constructor in the case there is a single datasource.
   * Useful in order to not have to construct the datasource over and over
   * again.
   */
  def this(dataSource: DataSource, packagePrefix: String, nestedTrait: Boolean) =
    this(Seq(() => dataSource.getConnection), packagePrefix, nestedTrait)
  def this(config: JdbcContextConfig, packagePrefix: String, nestedTrait: Boolean) =
    this(config.dataSource, packagePrefix, nestedTrait)
  def this(config: Config, packagePrefix: String, nestedTrait: Boolean) =
    this(JdbcContextConfig(config), packagePrefix, nestedTrait)
  def this(configPrefix: String, packagePrefix: String, nestedTrait: Boolean) =
    this(LoadConfig(configPrefix), packagePrefix, nestedTrait)

  def this(
    connectionMaker: JdbcConnectionMaker,
    packagePrefix: String
  ) = this(Seq(connectionMaker), packagePrefix, false)

  override def makeGenerators: Seq[ContextifiedUnitGenerator] = new MultiGeneratorFactory(generatorMaker).apply
  override def generatorMaker = new SingleGeneratorFactory[ContextifiedUnitGenerator] {
    override def apply(emitterSettings: EmitterSettings[JdbcTableMeta, JdbcColumnMeta]): ContextifiedUnitGenerator =
      new ContextifiedUnitGenerator(emitterSettings)
  }

  override def packagingStrategy: PackagingStrategy =
    PackagingStrategy.ByPackageHeader
      .TablePerSchema(packagePrefix)
      .copy(fileNamingStrategy =
        new BySomeTableData[ContextifiedUnitGenerator](gen =>
          Paths.get(gen.packageName.getOrElse(gen.defaultNamespace), gen.traitName)
        )
      )

  class ContextifiedUnitGenerator(emitterSettings: EmitterSettings[JdbcTableMeta, JdbcColumnMeta])
      extends CodeEmitter(emitterSettings) {

    def possibleTraitNesting(innerCode: String) = if (nestedTrait) {
      s"""
         |object ${packageName.getOrElse(defaultNamespace).capitalize}Schema {
         |  ${indent(innerCode)}
         |}
         |""".stripMargin.trimFront
    } else innerCode

    def traitName: String = packageName.getOrElse(defaultNamespace).capitalize + "Extensions"

    override def tableSchemasCode: String = super.tableSchemasCode.notEmpty
      .map(tsCode => s"""
                        |trait ${traitName}[+Idiom <: io.getquill.idiom.Idiom, +Naming <: io.getquill.NamingStrategy] {
                        |  this:io.getquill.context.Context[Idiom, Naming] =>
                        |
                        |  ${indent(possibleTraitNesting(indent(tsCode)))}
                        |}
                        |""".stripMargin.trimFront)
      .getOrElse("")

    override def CombinedTableSchemas = new CombinedTableSchemasGen(_, _) {
      override def objectName: Option[String] = super.objectName.map(_ + "Dao")
    }
  }
}
