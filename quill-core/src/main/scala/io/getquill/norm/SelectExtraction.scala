package io.getquill.norm

import scala.reflect.macros.whitebox.Context

trait SelectExtraction {
  this: NormalizationMacro =>

  val c: Context
  import c.universe._

  def selectExtractor[T, R](values: List[SelectValue])(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) = {
    var index = -1
      def nextIndex = {
        index += 1
        index
      }
    val decodedValues =
      values.map {
        case SimpleSelectValue(_, encoder) =>
          q"$encoder.decode($nextIndex, row)"
        case CaseClassSelectValue(tpe, params) =>
          val decodedParams =
            params.map(_.map {
              case SimpleSelectValue(_, encoder) =>
                q"$encoder.decode($nextIndex, row)"
            })
          q"new $tpe(...$decodedParams)"
      }
    q"""
    (row: $r) => (..$decodedValues)
    """
  }
}