package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Expr
import io.getquill.ast.Property
import io.getquill.util.Show._
import io.getquill.util.Messages._
import io.getquill.ast.Tuple
import io.getquill.ast.ExprShow
import io.getquill.ast.Query

trait SelectFlattening extends SelectValues {
  val c: Context

  import c.universe.{ Expr => _, _ }

  protected def flattenSelect[T](q: Query, inferDecoder: Type => Option[Tree])(implicit t: WeakTypeTag[T]) = {
    val (query, mapExpr) = ExtractSelect(q)
    val selectValues =
      selectElements(mapExpr).map {
        case (expr, typ) =>
          inferDecoder(typ) match {
            case Some(decoder) =>
              SimpleSelectValue(expr, decoder)
            case None if (typ.typeSymbol.asClass.isCaseClass) =>
              caseClassSelectValue(typ, expr, inferDecoder)
            case _ =>
              import ExprShow._
              c.fail(s"Source doesn't know how to decode '${t.tpe.typeSymbol.name}.${expr.show}: $typ'")
          }
      }
    (ReplaceSelect(query, selectExprs(selectValues).flatten), selectValues)
  }

  private def selectExprs(values: List[SelectValue]) =
    values map {
      case SimpleSelectValue(expr, _)      => List(expr)
      case CaseClassSelectValue(_, params) => params.flatten.map(_.expr)
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
            .getOrElse(c.fail(s"Source doesn't know how to decode '${param.name}: $paramType'"))
        SimpleSelectValue(Property(expr, param.name.decodedName.toString), decoder)
    })

  private def selectElements[T](mapExpr: Expr)(implicit t: WeakTypeTag[T]) =
    mapExpr match {
      case Tuple(values) =>
        if (values.size != t.tpe.typeArgs.size)
          c.fail(s"Query shape doesn't match the return type $t, please submit a bug report.")
        values.zip(t.tpe.typeArgs)
      case expr =>
        List(expr -> t.tpe)
    }

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if (m.isPrimaryConstructor) => m
    }.headOption.getOrElse {
      c.fail(s"Can't find the primary constructor for '${t.typeSymbol.name}, please submit a bug report.'")
    }

}