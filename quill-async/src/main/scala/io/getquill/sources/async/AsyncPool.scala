package io.getquill.sources.async

import com.github.mauricio.async.db.Connection
import io.netty.util._
import java.util.concurrent.atomic._
import java.util.concurrent.{ CopyOnWriteArrayList, ConcurrentLinkedQueue }
import java.util.concurrent.TimeUnit
import org.joda.time._
import scala.concurrent._

trait AsyncPool {
  def execute[R](a: AsyncIO[R]): Future[R]
  def close(): Unit
}

case class DefaultAsyncPoolConfig(
  host:              String,
  port:              Int,
  user:              String,
  database:          String,
  password:          String,
  poolSize:          Int,
  maxQueueSize:      Int,
  queryTimeout:      duration.FiniteDuration,
  connectionFactory: DefaultAsyncPoolConfig => Connection
)

case class QueuedTask[R](time: DateTime, io: AsyncIO[R], promise: Promise[R])

class DefaultAsyncPool(config: DefaultAsyncPoolConfig) extends AsyncPool {

  private val connections = new CopyOnWriteArrayList[QueuedConnection]
  private val idx = new AtomicInteger(0)
  private val queue = new ConcurrentLinkedQueue[QueuedTask[_]]
  private val timer = new HashedWheelTimer(1, TimeUnit.SECONDS)

  def close() = connections.toArray.foreach(c => c.asInstanceOf[QueuedConnection].close())

  def execute[R](a: AsyncIO[R]): Future[R] = {
    if (connections.size >= config.poolSize) {
      val i = idx.incrementAndGet() % connections.size()
      val c = replaceIfDead(i, connections.get(i))
      c.execute(a)
    } else {
      val c = config.connectionFactory(config)
      val qc = new QueuedConnection(
        c = c,
        config = new QueuedConnectionConfig(
        maxQueueSize = config.maxQueueSize,
        timeout = config.queryTimeout,
        timer = timer
      ), queue = queue
      )
      connections.add(qc)
      qc.execute(a)
    }
  }

  private def replaceIfDead(i: Int, c: QueuedConnection) = {
    if (c.timeoutCount() > 10 || !c.c.isConnected) {
      val newC =
        new QueuedConnection(
          c = config.connectionFactory(config),
          queue = queue,
          config = c.config
        )
      val previous = connections.set(i, newC)
      if (previous != c) {
        newC.close()
        previous
      } else {
        newC
      }
    } else c
  }
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
case object QueueWaitTimeout extends AsyncIOError
case object MaxQueueSizeExceed extends AsyncIOError
case object TransactionAlreadyStarted extends AsyncIOError
case object ConnectionClosed extends AsyncIOError

case class QueuedConnectionConfig(
  maxQueueSize: Int,
  timeout:      duration.FiniteDuration,
  timer:        HashedWheelTimer
)

class QueuedConnection(
  val c:      Connection,
  val queue:  ConcurrentLinkedQueue[QueuedTask[_]],
  val config: QueuedConnectionConfig
) {

  import AsyncPool.trampoline

  private val running = new AtomicBoolean(false)
  private val timeouts = new AtomicInteger(0)
  private val cf = c.connect

  def close() = {
    queue.clear()
  }

  def execute[R](io: AsyncIO[R]): Future[R] = {
    if (queue.size > config.maxQueueSize) {
      Future.failed(MaxQueueSizeExceed)
    } else {
      val p = Promise[R]
      val t = QueuedTask[R](DateTime.now(), io, p)
      val timeout = newTimeout(t)
      queue.offer(t)
      process()
      p.future.andThen {
        case _ => timeout.cancel()
      }
    }
  }

  def isRunning = running.get()
  def timeoutCount() = timeouts.get()

  private def newTimeout(task: QueuedTask[_]) = {
    config.timer.newTimeout(new TimerTask {
      def run(timeout: Timeout) = {
        queue.remove(task)
        task.promise.tryFailure(QueueWaitTimeout)
        timeouts.incrementAndGet()
        ()
      }
    }, config.timeout.toSeconds, TimeUnit.SECONDS)
  }

  private def process(): Unit = {
    if (!queue.isEmpty() && running.compareAndSet(false, true)) {
      val e = queue.poll()
      if (e == null) {
        //No task to run set running to false
        running.compareAndSet(true, false)
        ()
      } else {
        try {
          executeTask(e).onComplete {
            case _ =>
              running.compareAndSet(true, false)
              process()
          }
        } catch {
          case ex: Throwable =>
            running.compareAndSet(true, false)
            e.promise.tryFailure(ex)
            process()
        }
      }
    }
  }

  private def executeTask[R](e: QueuedTask[R]) = {
    val fut = performIO(e.io, false)
    e.promise.tryCompleteWith(fut)
    fut
  }

  private def performIO[R](io: AsyncIO[R], insideTrans: Boolean): Future[R] = {
    io match {
      case AsyncIOValue(v) =>
        Future.successful(v)
      case ExecuteCmd(sql, params, e) =>
        cf.flatMap(_.sendPreparedStatement(sql, params).map(e))
      case TransCmd(action) =>
        if (insideTrans) {
          Future.failed(TransactionAlreadyStarted)
        } else {
          cf.flatMap(_.inTransaction(_ => performIO(action, true)))
        }
      case MapCmd(io, f) =>
        performIO(io, insideTrans).map(f)
      case FlatMapCmd(io, f) =>
        performIO(io, insideTrans).flatMap { ra =>
          performIO(f(ra), insideTrans)
        }
      case QueryCmd(sql, p, e) =>
        cf.flatMap(_.sendPreparedStatement(sql, p).map(e))
      case SqlCmd(sql, e) => cf.flatMap(_.sendQuery(sql).map(e))
    }
  }
}
