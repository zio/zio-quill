package io.getquill.context.monix
import io.getquill.context.ContextEffect
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

object Runner {
  def default = new Runner {}
  def using(scheduler: Scheduler) = new Runner {
    override def schedule[T](t: Task[T]): Task[T] = t.executeOn(scheduler, true)
    override def boundary[T](t: Task[T]): Task[T] = t.executeOn(scheduler, true)
    override def scheduleObservable[T](o: Observable[T]): Observable[T] = o.executeOn(scheduler, true)
  }
}

trait Runner extends ContextEffect[Task] {
  override def wrap[T](t: => T): Task[T] = Task(t)
  override def push[A, B](result: Task[A])(f: A => B): Task[B] = result.map(f)
  override def seq[A, B](list: List[Task[A]]): Task[List[A]] = Task.sequence(list)
  def schedule[T](t: Task[T]): Task[T] = t
  def scheduleObservable[T](o: Observable[T]): Observable[T] = o
  def boundary[T](t: Task[T]): Task[T] = t.asyncBoundary

  /**
   * Use this method whenever a ResultSet is being wrapped. This has a distinct
   * method because the client may prefer to fail silently on a ResultSet close
   * as opposed to failing the surrounding task.
   */
  def wrapClose(t: => Unit): Task[Unit] = Task(t)
}
