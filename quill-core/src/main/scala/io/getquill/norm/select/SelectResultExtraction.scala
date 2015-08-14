package io.getquill.norm

import scala.reflect.macros.whitebox.Context

import io.getquill.norm.select.SelectValues

trait SelectResultExtraction extends SelectValues {

  val c: Context
  import c.universe._

  def selectResultExtractor[T, R](values: List[SelectValue])(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
    q"""
      (row: $r) => (..${expand(values)})
    """

  private def expand(values: List[SelectValue], index: Int = 0): List[Tree] =
    values match {
      case head :: tail => expand(head, index) +: expand(tail, index + 1)
      case Nil          => List()
    }

  private def expand(value: SelectValue, index: Int) =
    value match {
      case SimpleSelectValue(_, decoder) =>
        q"$decoder($index, row)"
      case CaseClassSelectValue(tpe, params) =>
        val decodedParams =
          params.map(_.map {
            case SimpleSelectValue(_, decoder) =>
              q"$decoder($index, row)"
          })
        q"new $tpe(...$decodedParams)"
    }
}
