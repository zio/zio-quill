package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.sources.EncodingMacro
import io.getquill.util.Messages._

trait SelectFlattening extends EncodingMacro {
  val c: Context

  import c.universe._

  protected def flattenSelect[T](q: Query, inferDecoder: Type => Option[Tree])(implicit t: WeakTypeTag[T]) = {
    val (query, mapAst) = ExtractSelect(q)
    val selectValues = encoding[T](mapAst, inferDecoder)
    (ReplaceSelect(query, selectAsts(selectValues)), selectValues)
  }

  private def selectAsts(value: Value, nested: Boolean = false): List[Ast] =
    value match {
      case SimpleValue(ast, _, _)                 => List(ast)
      case EmbeddedValue(_, params)               => params.flatten.map(selectAsts(_, nested = false)).flatten
      case OptionValue(value)                     => selectAsts(value)
      case CaseClassValue(_, params) if (!nested) => params.flatten.map(selectAsts(_, nested = true)).flatten
      case CaseClassValue(tpe, params)            => c.fail(s"Can't select non-embedded nested case classes. Found '$tpe'.")
    }
}
