package io

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Try }

package object getquill {

  type Query[+T] = Model.Query[T]
  type JoinQuery[A, B, R] = Model.JoinQuery[A, B, R]
  type EntityQueryModel[T] = Model.EntityQueryModel[T]
  type Action[E] = Model.Action[E]
  type Insert[E] = Model.Insert[E]
  type Update[E] = Model.Update[E]
  type ActionReturning[E, +Output] = Model.ActionReturning[E, Output]
  type Delete[E] = Model.Delete[E]
  type BatchAction[+A <: Model.QAC[_, _] with Action[_]] = Model.BatchAction[A]

  object testContext extends TestMirrorContextTemplate(MirrorIdiom, Literal) with TestEntities
  object testAsyncContext extends AsyncMirrorContext(MirrorIdiom, Literal) with TestEntities {

    // hack to avoid Await.result since scala.js doesn't support it
    implicit val immediateEC = new ExecutionContext {
      def execute(runnable: Runnable) = runnable.run()
      def reportFailure(cause: Throwable) = ()
    }

    def eval[T](f: Future[T]): T = {
      var res: Try[T] = Failure(new IllegalStateException())
      f.onComplete(res = _)
      res.get
    }
  }

}
