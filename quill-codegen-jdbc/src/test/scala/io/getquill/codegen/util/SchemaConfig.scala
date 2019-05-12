package io.getquill.codegen.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.io.Source

sealed trait SchemaConfig {

  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def fileName: String = {
    import scala.reflect.runtime.{ universe => u }
    val m = u.runtimeMirror(this.getClass.getClassLoader)
    val sym = m.reflect(this).symbol
    sym.name.decodedName.toString + ".sql"
  }
  lazy val content: String = {
    val content = Source.fromURL(this.getClass.getClassLoader.getResource(fileName)).mkString
    logger.info("Loaded content: " + content)
    content
  }
}

object SchemaConfig {
  case object `schema_casesensitive` extends SchemaConfig
  case object `schema_simple` extends SchemaConfig
  case object `schema_snakecase` extends SchemaConfig
  case object `schema_snakecase_twoschema_differentcolumns_differenttypes` extends SchemaConfig
  case object `schema_snakecase_twotable` extends SchemaConfig
  case object `schema_snakecase_twotable_differentcolumns` extends SchemaConfig
  case object `schema_snakecase_twotable_differentcolumns_differenttypes` extends SchemaConfig
  case object `schema_twotable` extends SchemaConfig
}
