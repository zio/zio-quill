package io.getquill.sources

import io.getquill.util.Messages._
import scala.concurrent.duration.DurationInt
import scala.language.existentials
import scala.reflect.api.Types
import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import io.getquill._
import io.getquill.util.Cache

trait QueryProbingMacro {
  val c: Context
  import c.universe.{ Try => _, _ }

  def probeQuery[T <: Source[_, _]](f: T => Try[_]) = {
    val tpe = c.prefix.tree.tpe
    QueryProbingMacro.cache
      .getOrElseUpdate(tpe, resolveSource(tpe), 30.seconds)
      .asInstanceOf[Option[T]]
      .map(f)
      .map {
        case Success(_) =>
        case Failure(ex) =>
          c.error(s"Query probing failed. Reason: '$ex'")
      }
  }

  private def resolveSource(tpe: Type) =
    tpe match {
      case tpe if (tpe <:< c.weakTypeOf[QueryProbing]) =>
        loadSource(tpe) match {
          case Success(source) =>
            Some(source)
          case Failure(ex) =>
            c.error(s"Can't load the source of type '$tpe' for compile-time query probing. Reason: '$ex'")
            None
        }
      case other =>
        None
    }

  private def loadSource(tpe: Type): Try[Source[_, _]] =
    Try {
      tpe match {
        case tpe if (tpe <:< c.weakTypeOf[Singleton]) =>
          val cls = Class.forName(tpe.typeSymbol.fullName + "$")
          val field = cls.getField("MODULE$")
          field.get(cls)
        case tpe =>
          Class.forName(tpe.typeSymbol.fullName).newInstance
      }
    }.asInstanceOf[Try[Source[_, _]]]
}

object QueryProbingMacro {
  private val cache = new Cache[Types#Type, Source[_, _]]
}
