package io.getquill.norm

import io.getquill.ast.Expr
import io.getquill.ast.Tuple
import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Property
import io.getquill.ast.ExprShow.exprShow
import io.getquill.util.ImplicitResolution
import io.getquill.util.Show._
import io.getquill.util.Messages

trait SelectNormalization extends Messages {
  this: NormalizationMacro =>

  val c: Context
  import c.universe.{ Expr => _, _ }

  protected def normalizeSelect[T](inferDecoder: Type => Option[Tree], mapExpr: Expr)(implicit t: WeakTypeTag[T]) = {
    selectElements(mapExpr).map {
      case (expr, typ) =>
        inferDecoder(typ) match {
          case Some(decoder) =>
            SimpleSelectValue(expr, decoder)
          case None if (typ.typeSymbol.asClass.isCaseClass) =>
            caseClassSelectValue(typ, expr, inferDecoder)
          case _ =>
            fail(s"Source doesn't know how to encode '${t.tpe.typeSymbol.name}.${expr.show}: $typ'")
        }
    }
  }

  private def caseClassSelectValue(typ: Type, expr: Expr, inferDecoder: Type => Option[Tree]) =
    CaseClassSelectValue(typ, selectValuesForCaseClass(typ, expr, inferDecoder))

  private def selectValuesForCaseClass(typ: Type, expr: Expr, inferDecoder: Type => Option[Tree]) =
    selectValuesForConstructor(caseClassConstructor(typ), expr, inferDecoder)

  private def selectValuesForConstructor(constructor: MethodSymbol, expr: Expr, inferDecoder: Type => Option[Tree]) =
    constructor.paramLists.map(_.map {
      param =>
        val paramType = param.typeSignature.typeSymbol.asType.toType
        val decoder =
          inferDecoder(paramType)
            .getOrElse(fail(s"Source doesn't know how to encode '${param.name}: $paramType'"))
        SimpleSelectValue(Property(expr, param.name.decodedName.toString), decoder)
    })

  private def selectElements[T](mapExpr: Expr)(implicit t: WeakTypeTag[T]) =
    mapExpr match {
      case Tuple(values) =>
        if (values.size != t.tpe.typeArgs.size)
          fail(s"Query shape doesn't match the return type $t, please submit a bug report.")
        values.zip(t.tpe.typeArgs)
      case expr =>
        List(expr -> t.tpe)
    }

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if (m.isPrimaryConstructor) => m
    }.headOption.getOrElse {
      fail(s"Can't find the primary constructor for '${t.typeSymbol.name}, please submit a bug report.'")
    }
}
