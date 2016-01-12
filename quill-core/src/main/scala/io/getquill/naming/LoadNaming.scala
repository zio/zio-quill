package io.getquill.naming

import scala.reflect.macros.whitebox.Context
import io.getquill.util.LoadObject

object LoadNaming {

  def dynamic(c: Context)(tpe: c.Type) = {
    import c.universe._
    q"""
    new io.getquill.naming.NamingStrategy {
      override def default(s: String) = {
        ${naming(c)(tpe).foldLeft[Tree](q"s")((s, n) => q"${n.typeSymbol.companion}.default($s)")}
      }

      override def table(s: String) = {
        ${naming(c)(tpe).foldLeft[Tree](q"s")((s, n) => q"${n.typeSymbol.companion}.table($s)")}
      }

      override def column(s: String) = {
        ${naming(c)(tpe).foldLeft[Tree](q"s")((s, n) => q"${n.typeSymbol.companion}.column($s)")}
      }
    }
    """
  }

  def static(c: Context)(tpe: c.Type) =
    new NamingStrategy {
      override def default(s: String) =
        naming(c)(tpe).map(LoadObject[NamingStrategy](c))
          .foldLeft(s)((s, n) => n.default(s))

      override def table(s: String) =
        naming(c)(tpe).map(LoadObject[NamingStrategy](c))
          .foldLeft(s)((s, n) => n.table(s))

      override def column(s: String) =
        naming(c)(tpe).map(LoadObject[NamingStrategy](c))
          .foldLeft(s)((s, n) => n.column(s))
    }

  private def naming(c: Context)(tpe: c.Type) = {
    import c.universe._

    val types =
      tpe match {
        case RefinedType(types, _) => types
        case other                 => List(other)
      }
    types
      .filterNot(_ =:= c.weakTypeOf[NamingStrategy])
      .filterNot(_ =:= c.weakTypeOf[scala.Nothing])
  }
}
