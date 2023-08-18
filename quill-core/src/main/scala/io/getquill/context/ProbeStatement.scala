package io.getquill.context

import scala.concurrent.duration.DurationInt
import scala.reflect.api.Types
import scala.reflect.macros.whitebox.{Context => MacroContext}
import scala.util.Failure
import scala.util.Success

import io.getquill._
import io.getquill.util.Cache
import io.getquill.util.MacroContextExt._
import io.getquill.util.LoadObject
import io.getquill.idiom.Idiom

object ProbeStatement {

  private val cache = new Cache[Types#Type, Context[Idiom, NamingStrategy]]

  def apply(statement: String, c: MacroContext) = {
    import c.universe.{Try => _, _}

    def resolveContext(tpe: Type) =
      tpe match {
        case tpe if tpe <:< c.weakTypeOf[QueryProbing] =>
          LoadObject[Context[Idiom, NamingStrategy]](c)(tpe) match {
            case Success(context) =>
              Some(context)
            case Failure(ex) =>
              c.error(
                s"Can't load the context of type '$tpe' for a compile-time query probing. " +
                  s"Make sure that context creation happens in a separate compilation unit. " +
                  s"For more information please refer to the documentation https://getquill.io/#quotation-query-probing. " +
                  s"Reason: '$ex'"
              )
              None
          }
        case _ =>
          None
      }

    val tpe = c.prefix.tree.tpe

    cache
      .getOrElseUpdate(tpe, resolveContext(tpe), 30.seconds)
      .map(_.probe(statement))
      .foreach {
        case Success(_) =>
        case Failure(ex) =>
          c.error(s"Query probing failed. Reason: '$ex'")
      }
  }
}
