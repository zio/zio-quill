package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context

trait SelectResultExtraction extends SelectValues {

  val c: Context
  import c.universe._

  def selectResultExtractor[R](values: List[SelectValue])(implicit r: WeakTypeTag[R]) = {
    val decodings =
      extractors(values) match {
        case (trees, _) => trees
      }
    q"""
    (row: $r) => (..$decodings)
    """
  }

  private def extractors(values: List[SelectValue], index: Int = 0): (List[Tree], Int) =
    values match {
      case Nil => (List(), index)
      case SimpleSelectValue(_, decoder) :: tail =>
        val (tailTrees, tailIndex) = extractors(tail, index + 1)
        (q"$decoder($index, row)" +: tailTrees, tailIndex)
      case CaseClassSelectValue(tpe, params) :: tail =>
        val (decodedParams, paramsIndex) =
          params.foldLeft((List[List[Tree]](), index)) {
            case ((trees, index), params) =>
              val (paramsTrees, paramsIndex) = extractors(params, index)
              (trees :+ paramsTrees, paramsIndex)
          }
        val (tailTrees, tailIndex) = extractors(tail, paramsIndex)
        (q"new $tpe(...$decodedParams)" +: tailTrees, tailIndex)
    }
}
