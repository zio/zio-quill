package io.getquill.context.zio

import io.getquill.context.ContextEffect
import zio.blocking.Blocking
import zio.{ Has, RIO, Task, ZIO }

import java.sql.Connection

object Runner {
  type RIOConn[T] = RIO[Has[Connection] with Blocking, T]

  def default = new Runner {}
}

trait Runner extends ContextEffect[Runner.RIOConn] {
  override def wrap[T](t: => T): Runner.RIOConn[T] = Task(t)
  override def push[A, B](result: Runner.RIOConn[A])(f: A => B): Runner.RIOConn[B] = result.map(f)
  override def seq[A](list: List[Runner.RIOConn[A]]): Runner.RIOConn[List[A]] = ZIO.collectAll[Has[Connection] with Blocking, Throwable, A, List](list)

  /**
   * Use this method whenever a ResultSet is being wrapped. This has a distinct
   * method because the client may prefer to fail silently on a ResultSet close
   * as opposed to failing the surrounding task.
   */
  // TODO Something about a drain that needs to happen here?
  //def wrapClose(t: => Any): ZIO[Any, Nothing, Unit] = ???
}
