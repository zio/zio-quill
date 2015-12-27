package io.getquill

import language.experimental.macros
import language.implicitConversions
import scala.reflect.macros.whitebox.Context
import io.getquill.util.Messages._

object ImplicitQuery {
  implicit def toQuery[P <: Product](f: Function1[_, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function2[_, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function3[_, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function4[_, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function5[_, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function6[_, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function7[_, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function8[_, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function9[_, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function10[_, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function11[_, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function12[_, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function13[_, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function14[_, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function15[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function16[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function17[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function18[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function19[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function20[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function21[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: Function22[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
}

private[getquill] class ImplicitQueryMacro(val c: Context) {
  import c.universe._

  def toQuery[P <: Product](f: Expr[Any])(implicit p: WeakTypeTag[P]): Tree = {
    if (!p.tpe.typeSymbol.asClass.isCaseClass || !f.actualType.typeSymbol.isModuleClass)
      c.fail("Can't query a non-case class")

    q"io.getquill.query[$p]"
  }
}
