package io.getquill.norm.select

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context

trait SelectResultExtraction extends SelectValues {

  val c: Context
  import c.universe._

  def selectResultExtractor[R](value: SelectValue)(implicit r: WeakTypeTag[R]) = {
    val (tree, _) = extractor(value)
    q"""
    (row: $r) => $tree
    """
  }

  private def extractor(value: SelectValue, index: Int = 0): (Tree, Int) =
    value match {
      case SimpleSelectValue(_, decoder, _) =>
        (q"$decoder($index, row)", index + 1)
      case TupleSelectValue(elems) =>
        val (trees, newIndex) = extractors(elems, index)
        (q"(..$trees)", newIndex)
      case CaseClassSelectValue(tpe, params) =>
        val (decodedParams, paramsIndex) =
          params.foldLeft((List[List[Tree]](), index)) {
            case ((trees, index), params) =>
              val (tree, newIndex) = extractors(params, index)
              (trees :+ tree, newIndex)
          }
        (q"new $tpe(...$decodedParams)", paramsIndex)
      case OptionSelectValue(value) =>
        optionalExtractor(value, index)
    }

  private def optionalExtractor(value: SelectValue, index: Int): (Tree, Int) =
    value match {
      case SimpleSelectValue(ast, _, None) =>
        c.fail(s"Source doesn't know how to decode the optional value $ast")
      case SimpleSelectValue(_, _, Some(decoder)) =>
        (q"$decoder($index, row)", index + 1)
      case TupleSelectValue(elems) =>
        val (trees, newIndex) = optionalExtractors(elems, index)
        (joinOptions(trees), newIndex)
      case CaseClassSelectValue(tpe, params) =>
        val (decodedParams, paramsIndex) =
          params.foldLeft((List[List[Tree]](), index)) {
            case ((trees, index), params) =>
              val (tree, newIndex) = optionalExtractors(params, index)
              (trees :+ tree, newIndex)
          }
        (q"${joinOptions(decodedParams.map(joinOptions(_)))}.map((${tpe.typeSymbol.companion}.apply _).tupled)", paramsIndex)
      case OptionSelectValue(value) =>
        val (tree, idx) = optionalExtractor(value, index)
        (q"Option($tree)", idx)
    }

  private def joinOptions(trees: List[Tree], index: Int = 0): Tree =
    trees match {
      case Nil => q"Some((..${(0 until index).map(i => TermName(s"o$i"))}))"
      case head :: tail =>
        val o = q"val ${TermName(s"o$index")} = ${EmptyTree}"
        q"$head.flatMap(($o) => ${joinOptions(tail, index + 1)})"
    }

  private def extractors(values: List[SelectValue], index: Int) =
    values.foldLeft((List[Tree](), index)) {
      case ((trees, idx), elem) =>
        val (ext, newIdx) = extractor(elem, idx)
        (trees :+ ext, newIdx)
    }

  private def optionalExtractors(values: List[SelectValue], index: Int) =
    values.foldLeft((List[Tree](), index)) {
      case ((trees, idx), elem) =>
        val (ext, newIdx) = optionalExtractor(elem, idx)
        (trees :+ ext, newIdx)
    }
}
