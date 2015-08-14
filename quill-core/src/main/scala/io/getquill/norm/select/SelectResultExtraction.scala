package io.getquill.norm

import scala.reflect.macros.whitebox.Context

import io.getquill.norm.select.SelectValues

trait SelectResultExtraction extends SelectValues {

  val c: Context
  import c.universe._

  def selectResultExtractor[T, R](values: List[SelectValue])(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) = {
    var index = -1
    def nextIndex = {
      index += 1
      index
    }
    val decodedValues =
      values.map {
        case SimpleSelectValue(_, decoder) =>
          q"$decoder($nextIndex, row)"
        case CaseClassSelectValue(tpe, params) =>
          val decodedParams =
            params.map(_.map {
              case SimpleSelectValue(_, decoder) =>
                q"$decoder($nextIndex, row)"
            })
          q"new $tpe(...$decodedParams)"
      }
    q"""
    (row: $r) => (..$decodedValues)
    """
  }
}
