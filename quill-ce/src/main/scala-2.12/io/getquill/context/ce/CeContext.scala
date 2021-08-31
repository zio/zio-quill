package io.getquill.context.ce

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, ExecutionInfo, StreamingContext }
import fs2.{ Stream => FStream }
import scala.language.higherKinds

trait CeContext[Idiom <: io.getquill.idiom.Idiom, Naming <: NamingStrategy, F[_]]
  extends Context[Idiom, Naming]
  with StreamingContext[Idiom, Naming] {

  override type StreamResult[T] = FStream[F, T]
  override type Result[T] = F[T]
  override type RunActionResult = Unit
  override type RunBatchActionResult = Unit
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
}
