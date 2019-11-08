package io.getquill.codegen.jdbc.model

import java.sql.Types._

import io.getquill.codegen.model._
import io.getquill.util.ContextLogger

import scala.reflect.{ ClassTag, classTag }

class DefaultJdbcTyper(
  strategy:          UnrecognizedTypeStrategy,
  numericPreference: NumericPreference
) extends (JdbcTypeInfo => Option[ClassTag[_]]) {

  private val logger = ContextLogger(classOf[DefaultJdbcTyper])
  private[getquill] val MaxIntDigits = 9
  private[getquill] val MaxLongDigits = 18

  def unresolvedType(jdbcType: Int, tag: ClassTag[_]): Option[ClassTag[_]] =
    unresolvedType(jdbcType, Some(tag))

  def unresolvedType(jdbcType: Int, tag: Option[ClassTag[_]]): Option[ClassTag[_]] = {
    strategy match {
      case AssumeString => Some(classTag[String])
      case SkipColumn   => None
      case ThrowTypingError =>
        throw new TypingError(s"Could not resolve jdbc type: ${jdbcType}" + tag.map(t => s" class: `${t}`.").getOrElse(""))
    }
  }

  def apply(jdbcTypeInfo: JdbcTypeInfo): Option[ClassTag[_]] = {

    val jdbcType = jdbcTypeInfo.jdbcType

    jdbcType match {
      case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR => Some(classTag[String])
      case NUMERIC => numericPreference match {
        case PreferPrimitivesWhenPossible if (jdbcTypeInfo.size <= MaxIntDigits) => Some(classTag[Int])
        case PreferPrimitivesWhenPossible if (jdbcTypeInfo.size <= MaxLongDigits) => Some(classTag[Long])
        case _ => Some(classTag[BigDecimal])
      }
      case DECIMAL                                   => Some(classTag[BigDecimal])
      case BIT | BOOLEAN                             => Some(classTag[Boolean])
      case TINYINT                                   => Some(classTag[Byte])
      case SMALLINT                                  => Some(classTag[Short])
      case INTEGER                                   => Some(classTag[Int])
      case BIGINT                                    => Some(classTag[Long])
      case REAL                                      => Some(classTag[Float])
      case FLOAT | DOUBLE                            => Some(classTag[Double])
      case DATE                                      => Some(classTag[java.time.LocalDate])
      case TIME                                      => Some(classTag[java.time.LocalDateTime])
      case TIMESTAMP                                 => Some(classTag[java.time.LocalDateTime])
      case ARRAY                                     => Some(classTag[java.sql.Array])

      case BINARY | VARBINARY | LONGVARBINARY | BLOB => unresolvedType(jdbcType, classTag[java.sql.Blob])
      case STRUCT                                    => unresolvedType(jdbcType, classTag[java.sql.Struct])
      case REF                                       => unresolvedType(jdbcType, classTag[java.sql.Ref])
      case DATALINK                                  => unresolvedType(jdbcType, classTag[java.net.URL])
      case ROWID                                     => unresolvedType(jdbcType, classTag[java.sql.RowId])
      case NCLOB                                     => unresolvedType(jdbcType, classTag[java.sql.NClob])
      case SQLXML                                    => unresolvedType(jdbcType, classTag[java.sql.SQLXML])
      case NULL                                      => unresolvedType(jdbcType, classTag[Null])

      case CLOB =>
        unresolvedType(jdbcType, classTag[java.sql.Clob])

      case other =>
        unresolvedType(other, None)
    }
  }
}