package io.getquill

import io.getquill.ast.{Ast, Query, StatefulTransformer}

trait StatefulCache[State] {
  def getOrCache(q: Ast, s: State, default: => (Ast, StatefulTransformer[State])): (Ast, StatefulTransformer[State])
  def getOrCache(q: Query, s: State, default: => (Query, StatefulTransformer[State])): (Query, StatefulTransformer[State])
}
object StatefulCache {
  class NoCache[State] extends StatefulCache[State] {
    override def getOrCache(q: Ast, s: State, default: => (Ast, StatefulTransformer[State])): (Ast, StatefulTransformer[State])       = default
    override def getOrCache(q: Query, s: State, default: => (Query, StatefulTransformer[State])): (Query, StatefulTransformer[State]) = default
  }
  object NoCache {
    def apply[State]() = new NoCache[State]
  }

}

trait StatelessCache {
  def getOrCache(q: Ast): Ast
  def getOrCache(q: Query): Query
}

trait HasStatelessCache {
  def cache: StatelessCache
  def cached(q: Ast)(default: => Ast): Ast       = cache.getOrCache(q)
  def cached(q: Query)(default: => Query): Query = cache.getOrCache(q)
}

trait StatelessCacheOpt {
  def getOrCache(q: Ast): Option[Ast]
  def getOrCache(q: Query): Option[Query]
}

trait HasStatelessCacheOpt {
  def cache: StatelessCacheOpt
  def cached(q: Ast)(default: => Option[Ast]): Option[Ast]       = cache.getOrCache(q)
  def cached(q: Query)(default: => Option[Query]): Option[Query] = cache.getOrCache(q)
}

trait HasStatefulCache[T] {
  def cache: StatefulCache[T]
  def state: T

  def cached(q: Ast)(default: => (Ast, StatefulTransformer[T])): (Ast, StatefulTransformer[T]) =
    cache.getOrCache(
      q,
      this.state,
      // If cache is a miss, evaluate "default" and write it together with the state to the cache
      default
    )

  def cached(q: Query)(default: => (Query, StatefulTransformer[T])): (Query, StatefulTransformer[T]) =
    cache.getOrCache(
      q,
      this.state,
      // If cache is a miss, evaluate "default" and write it together with the state to the cache
      default
    )
}
