package io.getquill

import io.getquill.ast.{ Ident, StatelessTransformer }
import io.getquill.norm.capture.TemporaryIdent
import io.getquill.quat.Quat
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

abstract class Spec extends AnyFreeSpec with Matchers with BeforeAndAfterAll {
  val QV = Quat.Value
  val QEP = Quat.Product.empty
  def QP(fields: String*) = Quat.LeafProduct(fields: _*)

  // Used by various tests to replace temporary idents created by AttachToEntity with 'x'
  val replaceTempIdent = new StatelessTransformer {
    override def applyIdent(id: Ident): Ident =
      id match {
        case TemporaryIdent(tid) =>
          Ident("x", id.quat)
        case _ =>
          id
      }
  }

  implicit class QuatOps(quat: Quat) {
    def productOrFail() =
      quat match {
        case p: Quat.Product => p
        case _               => throw new IllegalArgumentException(s"The quat ${quat} is expected to be a product but is not")
      }
  }

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
