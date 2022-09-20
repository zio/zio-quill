package io.getquill.codegen.gen

import java.nio.charset.StandardCharsets
import java.nio.file._

import com.typesafe.scalalogging.Logger
import io.getquill.codegen.model._
import io.getquill.codegen.util.StringSeqUtil._
import io.getquill.codegen.util.StringUtil.{indent, _}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Generator {
  generator: CodeGeneratorComponents with Stereotyper =>
  type SingleGeneratorFactory[Emitter <: Generator#CodeEmitter] = ((EmitterSettings[TableMeta, ColumnMeta]) => Emitter)

  private val logger = Logger(LoggerFactory.getLogger(classOf[Generator]))

  def defaultNamespace: String
  def filter(tc: RawSchema[TableMeta, ColumnMeta]): Boolean = true

  /**
   * Should we prefix object/package produced by this generator? Set this as the
   * value of that. Otherwise set this to be the empty string.
   */
  def packagePrefix: String
  def connectionMakers: Seq[ConnectionMaker]

  /**
   * Instantiate the generator for a particular schema
   */
  def generatorMaker = new SingleGeneratorFactory[CodeEmitter] {
    override def apply(emitterSettings: EmitterSettings[TableMeta, ColumnMeta]): CodeEmitter =
      new CodeEmitter(emitterSettings)
  }

  class MultiGeneratorFactory[Emitter <: Generator#CodeEmitter](someGenMaker: SingleGeneratorFactory[Emitter]) {
    def apply: Seq[Emitter] = {

      val schemas =
        connectionMakers.flatMap(conn => schemaReader(conn).filter(tbl => filter(tbl)))

      // combine the generated elements as dictated by the packaging strategy and write the generator
      val genProcess = new StereotypePackager[Emitter, TableMeta, ColumnMeta]
      val emitters =
        genProcess.packageIntoEmitters(someGenMaker, packagingStrategy, stereotype(schemas))

      emitters
    }
  }
  def makeGenerators = new MultiGeneratorFactory(generatorMaker).apply

  def writeAllFiles(location: String): Future[Seq[Path]] =
    Future.sequence(writeFiles(location))

  def writeFiles(location: String): Seq[Future[Path]] = {
    // can't put Seq[Gen] into here because doing Seq[Gen] <: SingleUnitCodegen makes it covariant
    // and args here needs to be contravariant
    def makeGenWithCorrespondingFile(gens: Seq[CodeEmitter]) =
      gens.map { gen =>
        def DEFAULT_NAME = gen.defaultNamespace

        def tableName =
          gen.caseClassTables.headOption
            .orElse(gen.querySchemaTables.headOption)
            .map(_.table.name)
            .getOrElse(DEFAULT_NAME)

        val fileName: Path =
          (packagingStrategy.fileNamingStrategy, gen.codeWrapper) match {
            case (ByPackageObjectStandardName, _) =>
              Paths.get("package")

            // When the user wants to group tables by package, and use a standard package heading,
            // create a new package with the same name. For example say you have a
            // public.Person table (in schema.table notation) if a namespacer that
            // returns 'public' is used. The resulting file will be public/PublicExtensions.scala
            // which will have a Quill Context named 'Public'
            case (ByPackageName, PackageHeader(packageName)) =>
              Paths.get(packageName, packageName)

            case (ByPackageName, _) =>
              Paths.get(gen.packageName.getOrElse(DEFAULT_NAME))

            case (ByTable, PackageHeader(packageName)) =>
              Paths.get(packageName, tableName)

            // First case classes table name or first Query Schemas table name, or default if both empty
            case (ByTable, _) =>
              Paths.get(gen.packageName.getOrElse(DEFAULT_NAME), tableName)

            case (ByDefaultName, _) =>
              Paths.get(DEFAULT_NAME)

            // Looks like 'Method' needs to be explicitly here since it doesn't understand Gen type annotation is actually SingleUnitCodegen
            case (strategy: BySomeTableData[_], _)
                if (strategy.tt.tpe <:< scala.reflect.runtime.universe.typeTag[Generator#CodeEmitter].tpe) =>
              strategy.asInstanceOf[BySomeTableData[CodeEmitter]].namer(gen)
          }

        val fileWithExtension = fileName.resolveSibling(fileName.getFileName.toString + ".scala")
        val loc               = Paths.get(location)

        (gen, Paths.get(location, fileWithExtension.toString))
      }

    val generatorsAndFiles = makeGenWithCorrespondingFile(makeGenerators)

    generatorsAndFiles.map {
      case (gen, filePath) => {
        Files.createDirectories(filePath.getParent)
        val content = gen.apply
        logger.debug("Writing content:\n" + content)
        Future {
          Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))
        }
      }
    }
  }

  val renderMembers = nameParser match {
    case CustomNames(_, _) => true
    case _                 => false
  }

  /**
   * Run the Generator and return objects as strings
   *
   * @return
   */
  def writeStrings = makeGenerators.map(_.apply)

  class CodeEmitter(emitterSettings: EmitterSettings[TableMeta, ColumnMeta])
      extends AbstractCodeEmitter
      with PackageGen {
    import io.getquill.codegen.util.ScalaLangUtil._

    val caseClassTables: Seq[TableStereotype[TableMeta, ColumnMeta]] = emitterSettings.caseClassTables
    val querySchemaTables: Seq[TableStereotype[TableMeta, ColumnMeta]] =
      if (nameParser.generateQuerySchemas) emitterSettings.querySchemaTables else Seq()
    override def codeWrapper: CodeWrapper = emitterSettings.codeWrapper

    /**
     * Use this when the term for a particular schema is undefined but you need
     * to have one e.g. if you are writing to a file.
     */
    def defaultNamespace: String = generator.defaultNamespace

    override def packagePrefix: String = Generator.this.packagePrefix

    override def code = surroundByPackage(body)
    def body: String  = caseClassesCode + "\n\n" + tableSchemasCode

    def caseClassesCode: String  = caseClassTables.map(CaseClass(_).code).mkString("\n\n")
    def tableSchemasCode: String = querySchemaTables.map(CombinedTableSchemas(_, querySchemaNaming).code).mkString("\n")

    protected def ifMembers(str: String) = if (renderMembers) str else ""

    def CaseClass = new CaseClassGen(_)
    class CaseClassGen(val tableColumns: TableStereotype[TableMeta, ColumnMeta])
        extends super.AbstractCaseClassGen
        with CaseClassNaming[TableMeta, ColumnMeta] {
      def code =
        s"case class ${actualCaseClassName}(" + tableColumns.columns.map(Member(_).code).mkString(", ") + ")"

      def Member = new MemberGen(_)
      class MemberGen(val column: ColumnFusion[ColumnMeta])
          extends super.AbstractMemberGen
          with FieldNaming[ColumnMeta] {
        override def rawType: String = column.dataType.toString()
        override def actualType: String = {
          val tpe = escape(rawType).replaceFirst("java\\.lang\\.", "")
          if (column.nullable) s"Option[${tpe}]" else tpe
        }
      }
    }

    def CombinedTableSchemas = new CombinedTableSchemasGen(_, _)
    class CombinedTableSchemasGen(
      tableColumns: TableStereotype[TableMeta, ColumnMeta],
      querySchemaNaming: QuerySchemaNaming
    ) extends AbstractCombinedTableSchemasGen
        with ObjectGen {

      override def code: String               = surroundByObject(body)
      override def objectName: Option[String] = Some(escape(tableColumns.table.name))

      // TODO Have this come directly from the Generator's context (but make sure to override it in the structural tests so it doesn't disturb them)
      def imports = querySchemaImports

      // generate variables for every schema e.g.
      // foo = querySchema(...)
      // bar = querySchema(...)
      def body: String = {
        val schemas = tableColumns.table.meta
          .map(schema => s"def ${querySchemaNaming(schema)} = " + indent(QuerySchema(tableColumns, schema).code))
          .mkString("\n\n")

        Seq(imports, schemas).pruneEmpty.mkString("\n\n")
      }

      def QuerySchema = new QuerySchemaGen(_, _)
      class QuerySchemaGen(val tableColumns: TableStereotype[TableMeta, ColumnMeta], schema: TableMeta)
          extends AbstractQuerySchemaGen
          with CaseClassNaming[TableMeta, ColumnMeta] {

        def members =
          ifMembers(
            (tableColumns.columns.map(QuerySchemaMapping(_).code).mkString(",\n"))
          )

        override def code: String = s"""
                                       |quote {
                                       |  ${indent(querySchema)}
                                       |}
          """.stripMargin.trimFront

        def querySchema: String = s"""
                                     |querySchema[${actualCaseClassName}](
                                     |  ${indent("\"" + fullTableName + "\"")}${ifMembers(",")}
                                     |  ${indent(members)}
                                     |)
          """.stripMargin.trimFront

        override def tableName: String          = schema.tableName
        override def schemaName: Option[String] = schema.tableSchema

        def QuerySchemaMapping = new QuerySchemaMappingGen(_)
        class QuerySchemaMappingGen(val column: ColumnFusion[ColumnMeta])
            extends AbstractQuerySchemaMappingGen
            with FieldNaming[ColumnMeta] {
          override def code: String           = s"""_.${fieldName} -> "${databaseColumn}""""
          override def databaseColumn: String = column.meta.head.columnName
        }
      }
    }
  }
}
