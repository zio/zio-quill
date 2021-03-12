package io.getquill.context.zio

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, StreamingContext }
import zio.blocking.Blocking
import zio.stream.ZStream
import zio.{ Has, ZIO }

trait ZioContext[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy] extends Context[Idiom, Naming]
  with StreamingContext[Idiom, Naming] {

  type Error

  // It's nice that we don't actually have to import any JDBC libraries to have a Connection type here
  override type StreamResult[T] = ZStream[Has[Session] with Blocking, Error, T]
  override type Result[T] = ZIO[Has[Session] with Blocking, Error, T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T

  // Need explicit return-type annotations due to scala/bug#8356. Otherwise macro system will not understand Result[Long]=Task[Long] etc...
  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): ZIO[Has[Session] with Blocking, Error, List[T]]
  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): ZIO[Has[Session] with Blocking, Error, T]
}
