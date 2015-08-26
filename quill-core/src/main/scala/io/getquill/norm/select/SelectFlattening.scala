package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ast
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.Tuple
import io.getquill.util.Messages.RichContext

trait SelectFlattening extends SelectValues {
  val c: Context

  import c.universe._

  protected def flattenSelect[T](q: Query, inferDecoder: Type => Option[Tree])(implicit t: WeakTypeTag[T]) = {
    val (query, mapAst) = ExtractSelect(q)
    val selectValues =
      selectElements(mapAst).map {
        case (ast, typ) =>
          inferDecoder(typ) match {
            case Some(decoder) =>
              SimpleSelectValue(ast, decoder)
            case None if (typ.typeSymbol.asClass.isCaseClass) =>
              caseClassSelectValue(typ, ast, inferDecoder)
            case _ =>
              c.fail(s"Source doesn't know how to decode '$ast: $typ'")
          }
      }
    (ReplaceSelect(query, selectAsts(selectValues).flatten), selectValues)
  }

  private def selectAsts(values: List[SelectValue]) =
    values map {
      case SimpleSelectValue(ast, _)       => List(ast)
      case CaseClassSelectValue(_, params) => params.flatten.map(_.ast)
    }

  private def caseClassSelectValue(typ: Type, ast: Ast, inferDecoder: Type => Option[Tree]) =
    CaseClassSelectValue(typ, selectValuesForCaseClass(typ, ast, inferDecoder))

  private def selectValuesForCaseClass(typ: Type, ast: Ast, inferDecoder: Type => Option[Tree]) =
    selectValuesForConstructor(caseClassConstructor(typ), ast, inferDecoder)

  private def selectValuesForConstructor(constructor: MethodSymbol, ast: Ast, inferDecoder: Type => Option[Tree]) =
    constructor.paramLists.map(_.map {
      param =>
        val paramType = param.typeSignature.typeSymbol.asType.toType
        val decoder =
          inferDecoder(paramType)
            .getOrElse(c.fail(s"Source doesn't know how to decode '${param.name}: $paramType'"))
        SimpleSelectValue(Property(ast, param.name.decodedName.toString), decoder)
    })

  private def selectElements[T](mapAst: Ast)(implicit t: WeakTypeTag[T]) =
    mapAst match {
      case Tuple(values) => values.zip(t.tpe.typeArgs)
      case ast           => List(ast -> t.tpe)
    }

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if (m.isPrimaryConstructor) => m
    }.headOption.get // a case class always have a primary constructor
}
