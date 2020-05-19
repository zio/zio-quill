package io.getquill

import io.getquill.context.zio.Runner
import io.getquill.context.ZioJdbc.Prefix
import org.scalatest.matchers.should.Matchers._
import zio.Task

import scala.util.Left

class RunnerSpec extends ZioSpec {
  def prefix = Prefix("testPostgresDB");

  class SideEffect {
    private var state = 0
    def apply() = state = 1
    def applied = state == 1
  }

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
      wrap(throw new RuntimeException("Surprise!")).either.runSyncUnsafe() should matchPattern {
        case Left(e: Throwable) if (e.getMessage == "Surprise!") =>
      }
    }

    "should push an effect correctly" in {
      push(Task(1))(_ + 1).runSyncUnsafe() should equal(2)
    }

    "should convert a sequence correctly" in {
      seq(List(Task(1), Task(2), Task(3))).runSyncUnsafe() should equal(List(1, 2, 3))
    }
  }
}
