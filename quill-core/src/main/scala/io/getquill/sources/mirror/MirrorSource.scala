package io.getquill.sources.mirror

import scala.reflect.macros.whitebox.Context
import language.experimental.macros
import scala.util.Failure
import scala.util.Success
import io.getquill._
import io.getquill.ast.{ Ast, Ident }
import io.getquill.quotation.Quoted
import io.getquill.sources.Source
import io.getquill.sources.SourceMacro
import io.getquill.util.Messages.RichContext
import io.getquill.norm.Normalize
import io.getquill.quotation.IsDynamic
import io.getquill.sources.SourceConfig

class MirrorSource(config: SourceConfig[MirrorSource])
    extends Source[Row, Row]
    with MirrorEncoders
    with MirrorDecoders {

  override def close = ()

  def run[T](quoted: Quoted[Query[T]]): QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => Query[T]]): P1 => QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => Query[T]]): (P1, P2) => QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => Query[T]]): (P1, P2, P3) => QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]

  def run[T](quoted: Quoted[Action[T]]): ActionMirror = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => Action[T]]): List[P1] => BatchActionMirror = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => Action[T]]): List[(P1, P2)] => BatchActionMirror = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => Action[T]]): List[(P1, P2, P3)] => BatchActionMirror = macro MirrorSourceMacro.run[Row, Row]

  def run[T](quoted: Quoted[T]): QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, T](quoted: Quoted[P1 => T]): P1 => QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, P2, T](quoted: Quoted[(P1, P2) => T]): (P1, P2) => QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]
  def run[P1, P2, P3, T](quoted: Quoted[(P1, P2, P3) => T]): (P1, P2, P3) => QueryMirror[T] = macro MirrorSourceMacro.run[Row, Row]

  def mirrorConfig = config

  def probe(ast: Ast) =
    if (ast.toString.contains("Fail"))
      Failure(new IllegalStateException("The ast contains 'Fail'"))
    else
      Success(())

  case class ActionMirror(ast: Ast)

  def execute(ast: Ast) =
    ActionMirror(ast)

  case class BatchActionMirror(ast: Ast, bindList: List[Row])

  def execute[T](ast: Ast, bindParams: T => Row => Row) =
    (values: List[T]) =>
      BatchActionMirror(ast, values.map(bindParams).map(_(Row())))

  case class QueryMirror[T](ast: Ast, binds: Row, extractor: Row => T)

  def query[T](ast: Ast, bind: Row => Row, extractor: Row => T) =
    QueryMirror(ast, bind(Row()), extractor)
}

class MirrorSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) =
    IsDynamic(ast) match {
      case false =>
        val normalized = Normalize(ast)
        resolveSource[MirrorSource].map(_.probe(normalized)) match {
          case Some(Failure(e)) => c.warn(s"Probe failed. Reason $e")
          case other            =>
        }
        c.info(normalized.toString)
        q"($normalized, $params)"
      case true =>
        q"""
          import io.getquill.norm._
          import io.getquill.ast._
          (Normalize($ast: Ast), $params)
        """
    }
}
