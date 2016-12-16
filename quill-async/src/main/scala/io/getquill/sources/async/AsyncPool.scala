package io.getquill.sources.async

import com.github.mauricio.async.db.Connection
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import org.joda.time._
import scala.concurrent._

trait AsyncPool {
  def execute[R](a: AsyncIO[R]): Future[R]
}

object AsyncPool {

  implicit object trampoline extends ExecutionContextExecutor {
    def execute(runnable: Runnable): Unit = {
      runnable.run()
    }
    def reportFailure(t: Throwable): Unit = t.printStackTrace()
  }
}

sealed trait AsyncIOError extends Exception
case object MaxQueueSizeExceed extends AsyncIOError
case object QueueWaitTimeout extends AsyncIOError

/**
 * Reliable connection pooling
 * Just with
 */
class DefaultAsyncPool[C <: Connection](
  maxQueueSize:       Int,
  timeoutSeconds:     Int,
  maxConnectionCount: Int,
  connectionFactory:  () => Future[C]
) {

  private val connectionCount = new AtomicInteger(0)

  case class Task[R](
    startTime: DateTime,
    promise:   Promise[R],
    io:        AsyncIO[R]
  )

  val queue = new ConcurrentLinkedQueue[Task[_]]
  val free = new ConcurrentLinkedQueue[C]

  def execute[R](io: AsyncIO[R]): Future[R] = {
    if (queue.size > maxQueueSize) {
      Future.failed(MaxQueueSizeExceed)
    } else {
      val startTime = DateTime.now()
      val promise = Promise[R]()
      val t = Task(startTime, promise, io)
      queue.offer(t)
      promise.future
    }
  }

  private def triggerProcess(): Unit = {
    if (!queue.isEmpty) {
      val c = free.poll()
      if (c != null) {
        processMore(c)
      }
    }
  }

  private def processMore(c: C): Unit = {
    import AsyncPool.trampoline
    val i = queue.poll()
    if (i != null) {
      try {
        performIO(c, i).onComplete {
          case _ => processMore(c)
        }
      } catch {
        case e: Throwable =>
          i.promise.tryFailure(e)
          ()
      }
    } else {
      free.offer(c)
      ()
    }
  }

  private def performIO[R](c: C, t: Task[R]): Future[Unit] = {
    ???
  }

}
