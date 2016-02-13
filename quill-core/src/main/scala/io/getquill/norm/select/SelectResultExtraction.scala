package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context
import io.getquill.sources.EncodingMacro

trait SelectResultExtraction extends EncodingMacro {

  val c: Context
  import c.universe._

  def selectResultExtractor[R](value: Value)(implicit r: WeakTypeTag[R]) = {
    val (tree, _) = extractor(value)
    q"""
    (row: $r) => $tree
    """
  }

  private def extractor(value: Value, index: Int = 0): (Tree, Int) =
    value match {
      case OptionValue(value) =>
        optionalExtractor(value, index)
      case SimpleValue(_, decoder, _) =>
        (q"$decoder($index, row)", index + 1)
      case CaseClassValue(tpe, params) =>
        val (decodedParams, paramsIndex) =
          params.foldLeft((List[List[Tree]](), index)) {
            case ((trees, index), params) =>
              val (tree, newIndex) = extractors(params, index)
              (trees :+ tree, newIndex)
          }
        (q"new $tpe(...$decodedParams)", paramsIndex)
    }

  private def optionalExtractor(value: Value, index: Int): (Tree, Int) =
    value match {
      case OptionValue(value) =>
        val (tree, idx) = optionalExtractor(value, index)
        (q"Option($tree)", idx)
      case SimpleValue(_, _, decoder) =>
        (q"$decoder($index, row)", index + 1)
      case CaseClassValue(tpe, params) =>
        val (decodedParams, paramsIndex) =
          params.foldLeft((List[List[Tree]](), index)) {
            case ((trees, index), params) =>
              val (tree, newIndex) = optionalExtractors(params, index)
              (trees :+ tree, newIndex)
          }
        val tree =
          if (tpe.typeSymbol.fullName.startsWith("scala.Tuple"))
            joinOptions(decodedParams.map(joinOptions(_)))
          else
            q"""
            val tuple = ${joinOptions(decodedParams.map(joinOptions(_)))}
            tuple.map((${tpe.typeSymbol.companion}.apply _).tupled)
          """
        (tree, paramsIndex)
    }

  private def joinOptions(trees: List[Tree], index: Int = 0): Tree =
    trees match {
      case Nil => q"Some((..${(0 until index).map(i => TermName(s"o$i"))}))"
      case head :: tail =>
        val o = q"val ${TermName(s"o$index")} = ${EmptyTree}"
        q"$head.flatMap(($o) => ${joinOptions(tail, index + 1)})"
    }

  private def extractors(values: List[Value], index: Int) =
    values.foldLeft((List[Tree](), index)) {
      case ((trees, idx), elem) =>
        val (ext, newIdx) = extractor(elem, idx)
        (trees :+ ext, newIdx)
    }

  private def optionalExtractors(values: List[Value], index: Int) =
    values.foldLeft((List[Tree](), index)) {
      case ((trees, idx), elem) =>
        val (ext, newIdx) = optionalExtractor(elem, idx)
        (trees :+ ext, newIdx)
    }
}
