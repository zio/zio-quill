package io.getquill.source.memory

import collection.mutable
import scala.collection.mutable.ListBuffer
import io.getquill.Queryable
import io.getquill.quotation.Quoted
import io.getquill.ast._
import language.experimental.macros
import io.getquill.Actionable
import io.getquill.source.Source

class TransientMemorySource extends Source[mutable.Map[String, Any], mutable.Map[String, Any]] {

  type R = mutable.Map[String, Any]
  type S = mutable.Map[String, Any]
  private val data = mutable.Map[String, ListBuffer[mutable.Map[String, Any]]]()

  def run[T](query: Queryable[T]): Any = macro QueryMacro.run[R, S, T]
  def run[P1, T](query: P1 => Queryable[T])(p1: P1): Any = macro QueryMacro.run1[P1, R, S, T]
  def run[P1, P2, T](query: (P1, P2) => Queryable[T])(p1: P1, p2: P2): Any = macro QueryMacro.run2[P1, P2, R, S, T]

  def run[T](action: Actionable[T]): Any = macro ActionMacro.run[R, S, T]
  def run[P1, T](action: P1 => Actionable[T])(bindings: Iterable[P1]): Any = macro ActionMacro.run1[P1, R, S, T]
  def run[P1, P2, T](action: (P1, P2) => Actionable[T])(bindings: Iterable[(P1, P2)]): Any = macro ActionMacro.run2[P1, P2, R, S, T]

  def execute(action: Action) =
    action

  def execute(action: Action, bindList: List[R => S]) = action

  def query[T](query: Query, bind: R => S, extractor: S => T) = {
    query
  }
}
