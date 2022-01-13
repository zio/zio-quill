package io.getquill

import scala.language.experimental.macros
import io.getquill.dsl.QueryDslMacro
import io.getquill.quotation.NonQuotedException

sealed trait EntityQuery[T]
  extends EntityQueryModel[T] {

  override def withFilter(f: T => Boolean): EntityQuery[T] = NonQuotedException()
  override def filter(f: T => Boolean): EntityQuery[T] = NonQuotedException()
  override def map[R](f: T => R): EntityQuery[R] = NonQuotedException()

  def insertValue(value: T): Insert[T] = macro QueryDslMacro.expandInsert[T]
  @deprecated("EntityQuery.insert(value) is deprecated due to upstream Scala 3 requirements. Use EntityQuery.insertValue(value) instead.", "3.13.0")
  def insert(value: T): Insert[T] = macro QueryDslMacro.expandInsert[T]
  def insert(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Insert[T]

  def update(value: T): Update[T] = macro QueryDslMacro.expandUpdate[T]
  def update(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Update[T]

  def delete: Delete[T]
}
