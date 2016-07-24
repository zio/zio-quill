package io.getquill

import scala.util.Failure
import scala.util.Success

import io.getquill.ast.Ast
import io.getquill.context.Context
import io.getquill.context.mirror.MirrorDecoders
import io.getquill.context.mirror.MirrorEncoders
import io.getquill.context.mirror.Row

import scala.language.experimental.macros
import io.getquill.context.mirror.MirrorContextMacro

private[getquill] object mirrorWithQueryProbing extends MirrorContextWithQueryProbing

class MirrorContextWithQueryProbing extends MirrorContext with QueryProbing

class MirrorContext
  extends Context[Row, Row]
  with MirrorEncoders
  with MirrorDecoders {

  override def close = ()

  def run[T](quoted: Quoted[Query[T]]): QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => Query[T]]): P1 => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => Query[T]]): (P1, P2) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => Query[T]]): (P1, P2, P3) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]

  def run[T, O](quoted: Quoted[Action[T, O]]): ActionMirror = macro MirrorContextMacro.run[Row, Row]
  def run[P1, T, O](quoted: Quoted[P1 => Action[T, O]]): List[P1] => BatchActionMirror = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, T, O](quoted: Quoted[(P1, P2) => Action[T, O]]): List[(P1, P2)] => BatchActionMirror = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, P3, T, O](quoted: Quoted[(P1, P2, P3) => Action[T, O]]): List[(P1, P2, P3)] => BatchActionMirror = macro MirrorContextMacro.run[Row, Row]

  def run[T](quoted: Quoted[T]): QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => T]): P1 => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => T]): (P1, P2) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => T]): (P1, P2, P3) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]

  def probe(ast: Ast) =
    if (ast.toString.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  case class ActionMirror(ast: Ast, bind: Row, returning: Option[String])

  def transaction[T](f: MirrorContext => T) = f(this)

  def executeAction[O](ast: Ast, bindParams: Row => Row = identity, returning: Option[String] = None, returningExtractor: Row => O = identity[Row] _) =
    ActionMirror(ast, bindParams(Row()), returning)

  case class BatchActionMirror(ast: Ast, bindList: List[Row], returning: Option[String])

  def executeActionBatch[T, O](ast: Ast, bindParams: T => Row => Row = (_: T) => identity[Row] _, returning: Option[String] = None, returningExtractor: Row => O = identity[Row] _) =
    (values: List[T]) =>
      BatchActionMirror(ast, values.map(bindParams).map(_(Row())), returning)

  case class QueryMirror[T](ast: Ast, binds: Row, extractor: Row => T)

  def executeQuerySingle[T](ast: Ast, extractor: Row => T = identity[Row] _, bind: Row => Row = identity) =
    QueryMirror(ast, bind(Row()), extractor)

  def executeQuery[T](ast: Ast, extractor: Row => T = identity[Row] _, bind: Row => Row = identity) = QueryMirror(ast, bind(Row()), extractor)
}
