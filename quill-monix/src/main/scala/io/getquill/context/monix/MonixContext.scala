package io.getquill.context.monix

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, StreamingContext }
import monix.eval.Task
import monix.reactive.Observable

trait MonixContext[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends Context[Idiom, Naming]
  with StreamingContext[Idiom, Naming] {

  override type StreamResult[T] = Observable[T]
  override type Result[T] = Task[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[List[T]]
  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): Task[T]

  protected val effect: Runner
}
