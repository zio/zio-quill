package io.getquill.codegen.integration

import com.typesafe.scalalogging.Logger
import io.getquill.codegen.util.ConfigPrefix
import io.getquill.codegen.util.SchemaConfig._
import io.getquill.codegen.util.TryOps._
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

object SchemaNames {
  val simpleSnake = `schema_snakecase`
  val simpleLiteral = `schema_casesensitive`
  val twoSchema = `schema_snakecase_twoschema_differentcolumns_differenttypes`
}

object CodegenTestCaseRunner {

  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def main(args: Array[String]): Unit = {
    val path = args(0)
    val prefixes =
      if (args.drop(1).contains("all")) ConfigPrefix.all
      else args.drop(1).map(ConfigPrefix.fromValue(_).orThrow).toList

    // Need to generate files for each database test-case by test-case (since auto-commit is used)
    // but can do cases from multiple databases at the same time
    val results =
      prefixes.map(prefix =>
        Future {
          val generatedFiles = apply(prefix, path)
          generatedFiles.foreach(f => logger.info(s"${prefix} | ${f}"))
        })

    Await.result(Future.sequence(results), Duration.Inf)
    ()
  }

  def apply(dbPrefix: ConfigPrefix, path: String) = {
    CodegenTestCases(dbPrefix).map(gen => {
      logger.info(s"Generating files for: ${dbPrefix.value} (${dbPrefix.packagePath}) with ${gen}")
      // Since auto-commit in enabled, need to wait for each test-case individually. Otherwise tests
      // will step on each-other's toes.
      Await.result(gen.generateWithSchema(dbPrefix, path), Duration.Inf).toSeq
    }).flatten
  }
}

