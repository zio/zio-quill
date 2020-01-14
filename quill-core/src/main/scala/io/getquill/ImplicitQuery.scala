package io.getquill

import scala.language.experimental.macros
import scala.language.implicitConversions
import scala.reflect.macros.whitebox.{ Context => MacroContext }
import scala.runtime.AbstractFunction1
import scala.runtime.AbstractFunction10
import scala.runtime.AbstractFunction11
import scala.runtime.AbstractFunction12
import scala.runtime.AbstractFunction13
import scala.runtime.AbstractFunction14
import scala.runtime.AbstractFunction15
import scala.runtime.AbstractFunction16
import scala.runtime.AbstractFunction17
import scala.runtime.AbstractFunction18
import scala.runtime.AbstractFunction19
import scala.runtime.AbstractFunction2
import scala.runtime.AbstractFunction20
import scala.runtime.AbstractFunction21
import scala.runtime.AbstractFunction22
import scala.runtime.AbstractFunction3
import scala.runtime.AbstractFunction4
import scala.runtime.AbstractFunction5
import scala.runtime.AbstractFunction6
import scala.runtime.AbstractFunction7
import scala.runtime.AbstractFunction8
import scala.runtime.AbstractFunction9

import io.getquill.util.MacroContextExt._
import io.getquill.context.Context

trait ImplicitQuery {
  this: Context[_, _] =>

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

private[getquill] class ImplicitQueryMacro(val c: MacroContext) {
  import c.universe._

  def toQuery[P <: Product](f: Expr[Any])(implicit p: WeakTypeTag[P]): Tree = {
    if (!p.tpe.typeSymbol.asClass.isCaseClass || !f.actualType.typeSymbol.isModuleClass)
      c.fail("Can't query a non-case class")

    q"${c.prefix}.query[$p]"
  }
}
