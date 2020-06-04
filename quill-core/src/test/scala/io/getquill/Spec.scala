package io.getquill

import io.getquill.ast.{ Ident, StatelessTransformer }
import io.getquill.norm.capture.TemporaryIdent
import io.getquill.quat.Quat
import io.getquill.quat.Quat.Probity
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

abstract class Spec extends AnyFreeSpec with Matchers with BeforeAndAfterAll {
  val QV = Quat.Value
  val QEP = Quat.Product.empty
  def QP(fields: String*) = Quat.LeafProduct(fields: _*)
  def pppsdfsdfsdfsdfasdfsfdsdfsdsdfsdfsdfsdfsdfsfsdfdasd = "sdsdfbppsssdfsfddfsssdfsdfsdfsdfdfsdsdfsd"

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

  implicit class ProdOrQuatOps(quat: Probity) {
    def rename(fields: (String, String)*): Probity =
      fields.foldLeft(quat) { case (quat, (field, value)) => quat.prove.stashRename(field, value) }.applyRenames
  }

  def await[T](f: Future[T]): T = Await.result(f, Duration.Inf)
}
