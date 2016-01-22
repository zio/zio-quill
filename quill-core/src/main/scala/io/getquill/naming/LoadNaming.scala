package io.getquill.naming

import scala.reflect.macros.whitebox.Context
import io.getquill.util.LoadObject

object LoadNaming {

  def dynamic(c: Context)(tpe: c.Type) = {
    import c.universe._
    val list = strategies(c)(tpe).map(s => q"${s.typeSymbol.companion}")
    q"io.getquill.naming.NamingStrategy($list)"
  }

  def static(c: Context)(tpe: c.Type) =
    NamingStrategy(strategies(c)(tpe).map(LoadObject[NamingStrategy](c)))

  private def strategies(c: Context)(tpe: c.Type) = {
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
