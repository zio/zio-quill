package io.getquill.context.qzio

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, StreamingContext }
import zio.ZIO
import zio.stream.ZStream

trait ZioContext[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends Context[Idiom, Naming]
  with StreamingContext[Idiom, Naming] {

  type Error
  type Environment

  // It's nice that we don't actually have to import any JDBC libraries to have a Connection type here
  override type StreamResult[T] = ZStream[Environment, Error, T]
  override type Result[T] = ZIO[Environment, Error, T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): ZIO[Environment, Error, List[T]]
  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): ZIO[Environment, Error, T]
}
