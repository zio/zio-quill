package io.getquill.dsl

import scala.language.experimental.macros

trait MetaDslLowPriorityImplicits {
  this: MetaDsl =>

  implicit def materializeQueryMeta[T]: QueryMeta[T] = macro MetaDslMacro.materializeQueryMeta[T]
  implicit def materializeUpdateMeta[T]: UpdateMeta[T] = macro MetaDslMacro.materializeUpdateMeta[T]
  implicit def materializeInsertMeta[T]: InsertMeta[T] = macro MetaDslMacro.materializeInsertMeta[T]
  implicit def materializeSchemaMeta[T]: SchemaMeta[T] = macro MetaDslMacro.materializeSchemaMeta[T]
}

trait MetaDsl extends MetaDslLowPriorityImplicits {
  this: CoreDsl =>

  type Embedded = io.getquill.Embedded

  def schemaMeta[T](entity: String, columns: (T => (Any, String))*): SchemaMeta[T] = macro MetaDslMacro.schemaMeta[T]
  def queryMeta[T, R](expand: Quoted[Query[T] => Query[R]])(extract: R => T): QueryMeta[T] = macro MetaDslMacro.queryMeta[T, R]
  def updateMeta[T](exclude: (T => Any)*): UpdateMeta[T] = macro MetaDslMacro.updateMeta[T]
  def insertMeta[T](exclude: (T => Any)*): InsertMeta[T] = macro MetaDslMacro.insertMeta[T]

  trait SchemaMeta[T] {
    def entity: Quoted[EntityQuery[T]]
  }

  trait QueryMeta[T] {
    def expand: Quoted[Query[T] => Query[_]]
    def extract: ResultRow => T
  }

  trait UpdateMeta[T] {
    def expand: Quoted[(EntityQuery[T], T) => Update[T]]
  }

  trait InsertMeta[T] {
    def expand: Quoted[(EntityQuery[T], T) => Insert[T]]
  }
}
