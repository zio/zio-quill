package io.getquill.codegen.util

import java.io.Closeable

import io.getquill._
import io.getquill.codegen.integration.DbHelper
import javax.sql.DataSource
import org.scalatest.freespec.AnyFreeSpec

import scala.language.implicitConversions

abstract class CodegenSpec extends AnyFreeSpec with SchemaMaker {
  type Prefix <: ConfigPrefix
  val prefix: Prefix

  implicit def regToOption[T](t: T) = Some(t)
}

object SchemaMaker extends SchemaMaker

case class SchemaMakerCoordinates(dbPrefix: ConfigPrefix, naming: NamingStrategy, schemaConfig: SchemaConfig)

trait SchemaMaker {

  private[getquill] def withDatasource[T](schemaConfig: SchemaConfig, dbPrefix: ConfigPrefix)(testCode: DataSource with Closeable => T): T = {
    val ds = dbPrefix.makeDatasource
    val helper = new DbHelper(schemaConfig, dbPrefix, ds)
    DbHelper.dropTables(ds)
    helper.setup()
    testCode(ds)
  }

  def withContext[T](coords: SchemaMakerCoordinates)(testCode: => T): T = {
    import coords._
    withDatasource(schemaConfig, dbPrefix)(ds => {
      testCode
    })
  }
}
