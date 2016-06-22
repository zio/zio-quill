package io.getquill.context.mirror

import scala.reflect.macros.whitebox.{Context => MacroContext}
import language.experimental.macros
import scala.util.Failure
import scala.util.Success
import io.getquill.ast.{ Ast, Ident }
import io.getquill.context._
import io.getquill.util.Messages.RichContext
import io.getquill.norm.Normalize
import io.getquill.quotation.IsDynamic
import io.getquill.QueryProbing

class MirrorContextTemplateWithQueryProbing extends MirrorContext with QueryProbing

class MirrorContext
  extends Context[Row, Row]
  with MirrorEncoders
  with MirrorDecoders {

  override def close = ()

  def run[T](quoted: Quoted[Query[T]]): QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => Query[T]]): P1 => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => Query[T]]): (P1, P2) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => Query[T]]): (P1, P2, P3) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]

  def run[T](quoted: Quoted[Action[T]]): ActionMirror = macro MirrorContextMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => Action[T]]): List[P1] => BatchActionMirror = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => Action[T]]): List[(P1, P2)] => BatchActionMirror = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => Action[T]]): List[(P1, P2, P3)] => BatchActionMirror = macro MirrorContextMacro.run[Row, Row]

  def run[T](quoted: Quoted[T]): QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => T]): P1 => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => T]): (P1, P2) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => T]): (P1, P2, P3) => QueryMirror[T] = macro MirrorContextMacro.run[Row, Row]

  def probe(ast: Ast) =
    if (ast.toString.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  case class ActionMirror(ast: Ast, bind: Row)

  def transaction[T](f: MirrorContext => T) = f(this)

  def executeAction(ast: Ast, bindParams: Row => Row = identity, generated: Option[String] = None) =
    ActionMirror(ast, bindParams(Row()))

  case class BatchActionMirror(ast: Ast, bindList: List[Row])

  def executeActionBatch[T](ast: Ast, bindParams: T => Row => Row = (_: T) => identity[Row] _, generated: Option[String] = None) =
    (values: List[T]) =>
      BatchActionMirror(ast, values.map(bindParams).map(_(Row())))

  case class QueryMirror[T](ast: Ast, binds: Row, extractor: Row => T)

  def executeQuerySingle[T](ast: Ast, extractor: Row => T = identity[Row] _, bind: Row => Row = identity) =
    QueryMirror(ast, bind(Row()), extractor)

  def executeQuery[T](ast: Ast, extractor: Row => T = identity[Row] _, bind: Row => Row = identity) = QueryMirror(ast, bind(Row()), extractor)
}

class MirrorContextMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) =
    IsDynamic(ast) match {
      case false =>
        val normalized = Normalize(ast)
        probeQuery[MirrorContext](_.probe(normalized))
        c.info(normalized.toString)
        val (entity, insert) = ExtractEntityAndInsertAction(normalized)
        val isInsert = insert.isDefined
        val generated = if (isInsert) entity.flatMap(_.generated) else None
        q"($normalized, $params, $generated)"
      case true =>
        q"""
          import io.getquill.norm._
          import io.getquill.ast._
          import io.getquill.context.ExtractEntityAndInsertAction

          val normalized = Normalize($ast: Ast)
          val (entity, insert) = ExtractEntityAndInsertAction(normalized)
          val isInsert = insert.isDefined
          val generated = if (isInsert) entity.flatMap(_.generated) else None

          (normalized, $params, generated)
        """
    }
}
