package io.getquill.context.monix

import io.getquill.Spec
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.schedulers.CanBlock
import org.scalatest.matchers.should.Matchers._

import scala.util.Failure

class RunnerSpec extends Spec {

  class SideEffect {
    private var state = 0
    def apply() = state = 1
    def applied = state == 1
  }

  implicit val scheduler = Scheduler.global

  "plain runner" - {
    val runner = Runner.default
    import runner._

    "should lazily evaluate" in {
      val sideEffect = new SideEffect
      val task = wrap(sideEffect())
      sideEffect.applied should equal(false)
      task.runSyncUnsafe()
      sideEffect.applied should equal(true)
    }

    "should encapsulate exception throw" in {
      wrap(throw new RuntimeException("Surprise!")).materialize.runSyncUnsafe() should matchPattern {
        case Failure(e) if (e.getMessage == "Surprise!") =>
      }
    }

    "should push an effect correctly" in {
      push(Task(1))(_ + 1).runSyncUnsafe() should equal(2)
    }

    "should convert a sequence correctly" in {
      seq(List(Task(1), Task(2), Task(3))).runSyncUnsafe() should equal(List(1, 2, 3))
    }

    "plain schedule should be a no-op" in {
      val t = Task(1)
      (schedule(t) eq (t)) must equal(true)
    }

    "boundary operator must force async boundary" in {
      val (first, second) =
        boundary(Task(Thread.currentThread().getName))
          .flatMap(prevName => Task((prevName, Thread.currentThread().getName)))
          .runSyncUnsafe()
      first must not equal (second)
    }
  }

  "using scheduler runner" - {
    val prefix = "quill-test-pool"
    val customScheduler = Scheduler.io(prefix)
    val runner = Runner.using(customScheduler)
    import runner._

    "should run in specified scheduler" in {
      // the global scheduler is imported but want to explicitly tell this to run on it, just for clarity
      val threadName =
        schedule(Task(Thread.currentThread().getName)).runSyncUnsafe()(Scheduler.global, CanBlock.permit)

      threadName.startsWith(prefix) must equal(true)
    }

    "should async-boundary in specified scheduler" in {
      // the global scheduler is imported but want to explicitly tell this to run on it, just for clarity
      val threadName =
        boundary(Task(Thread.currentThread().getName)).runSyncUnsafe()(Scheduler.global, CanBlock.permit)

      threadName.startsWith(prefix) must equal(true)
    }

    "should async-boundary correctly" in {
      val prefix2 = "quill-test-pool2"
      // the global scheduler is imported but want to explicitly tell this to run on it, just for clarity
      val (first, second) =
        Task(Thread.currentThread().getName)
          .executeOn(Scheduler.io(prefix2))
          .flatMap(prevName => boundary(Task((prevName, Thread.currentThread().getName))))
          .runSyncUnsafe()(Scheduler.global, CanBlock.permit)

      first.startsWith(prefix2) must equal(true)
      second.startsWith(prefix) must equal(true)
      first must not equal second
    }
  }
}
