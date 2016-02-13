package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.util.Messages.RichContext
import io.getquill.sources.EncodingMacro

trait SelectFlattening extends EncodingMacro {
  val c: Context

  import c.universe._

  protected def flattenSelect[T](q: Query, inferDecoder: Type => Option[Tree])(implicit t: WeakTypeTag[T]) = {
    val (query, mapAst) = ExtractSelect(q)
    val selectValues = encoding[T](mapAst, inferDecoder)
    (ReplaceSelect(query, selectAsts(selectValues)), selectValues)
  }

  private def selectAsts(value: Value): List[Ast] =
    value match {
      case SimpleValue(ast, _, _)    => List(ast)
      case CaseClassValue(_, params) => params.flatten.map(selectAsts).flatten
      case OptionValue(value)        => selectAsts(value)
    }
}
