package io.getquill.context

import io.getquill.util.Messages._
import scala.concurrent.duration.DurationInt
import scala.language.existentials
import scala.reflect.api.Types
import scala.reflect.macros.whitebox.{Context => MacroContext}
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import io.getquill._
import io.getquill.util.Cache

trait QueryProbingMacro {
  val c: MacroContext
  import c.universe.{ Try => _, _ }

  def probeQuery[T <: Context[_, _]](f: T => Try[_]) = {
    val tpe = c.prefix.tree.tpe
    QueryProbingMacro.cache
      .getOrElseUpdate(tpe, resolveContext(tpe), 30.seconds)
      .asInstanceOf[Option[T]]
      .map(f)
      .map {
        case Success(_) =>
        case Failure(ex) =>
          c.error(s"Query probing failed. Reason: '$ex'")
      }
  }

  private def resolveContext(tpe: Type) =
    tpe match {
      case tpe if (tpe <:< c.weakTypeOf[QueryProbing]) =>
        loadContext(tpe) match {
          case Success(context) =>
            Some(context)
          case Failure(ex) =>
            c.error(s"Can't load the context of type '$tpe' for compile-time query probing. Reason: '$ex'")
            None
        }
      case other =>
        None
    }

  private def loadContext(tpe: Type): Try[Context[_, _]] =
    Try {
      tpe match {
        case tpe if (tpe <:< c.weakTypeOf[Singleton]) =>
          val cls = Class.forName(tpe.typeSymbol.fullName + "$")
          val field = cls.getField("MODULE$")
          field.get(cls)
        case tpe =>
          Class.forName(tpe.typeSymbol.fullName).newInstance
      }
    }.asInstanceOf[Try[Context[_, _]]]
}

object QueryProbingMacro {
  private val cache = new Cache[Types#Type, Context[_, _]]
}
