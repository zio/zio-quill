package io.getquill

import language.implicitConversions
import language.experimental.macros
import scala.reflect.macros.whitebox.Context
import io.getquill.util.Messages._
import scala.runtime._
import io.getquill.sources.Source

trait ImplicitQuery {
  this: Source[_, _] =>

  implicit def toQuery[P <: Product](f: AbstractFunction1[_, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction2[_, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction3[_, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction4[_, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction5[_, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction6[_, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction7[_, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction8[_, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction9[_, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction10[_, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction11[_, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction12[_, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction13[_, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction14[_, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction15[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction16[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction17[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction18[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction19[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction20[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction21[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
  implicit def toQuery[P <: Product](f: AbstractFunction22[_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, P]): Query[P] = macro ImplicitQueryMacro.toQuery[P]
}

private[getquill] class ImplicitQueryMacro(val c: Context) {
  import c.universe._

  def toQuery[P <: Product](f: Expr[Any])(implicit p: WeakTypeTag[P]): Tree = {
    if (!p.tpe.typeSymbol.asClass.isCaseClass || !f.actualType.typeSymbol.isModuleClass)
      c.fail("Can't query a non-case class")

    q"${c.prefix}.query[$p]"
  }
}
