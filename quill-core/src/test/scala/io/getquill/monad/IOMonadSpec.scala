package io.getquill.monad

import java.util.concurrent.atomic.AtomicInteger

import io.getquill.Spec

import scala.util.Success
import scala.util.Try
import scala.util.Failure
import io.getquill.monad.Effect.Write
import io.getquill.TestEntities
import io.getquill.context.Context

trait IOMonadSpec extends Spec {

  val ctx: Context[_, _] with IOMonad with TestEntities
  import ctx._

  def eval[T](io: IO[T, _]): T

  def resultValue[T](x: T): Result[T]

  "IO companion object" - {

    "fromTry" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t)
        Try(eval(io)) mustEqual t
      }
      "failure" in {
        val t = Failure(new Exception)
        val io = IO.fromTry(t)
        Try(eval(io)) mustEqual t
      }
    }

    "sequence" - {
      "empty" in {
        val io = IO.sequence(Seq.empty[IO[Int, Write]])
        eval(io) mustEqual Seq()
      }
      "non-empty" in {
        val io = IO.sequence(Seq(IO.successful(1), IO.successful(2)))
        eval(io) mustEqual Seq(1, 2)
      }
    }

    "unit" in {
      eval(IO.unit) mustEqual (())
    }

    "zip" - {
      "success" in {
        val io = IO.zip(IO.successful(1), IO.successful(2))
        eval(io) mustEqual ((1, 2))
      }
      "failure" - {
        "left" in {
          val ex = new Exception
          val io = IO.zip(IO.failed(ex), IO.successful(2))
          Try(eval(io)) mustEqual Failure(ex)
        }
        "right" in {
          val ex = new Exception
          val io = IO.zip(IO.successful(1), IO.failed(ex))
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }

    "failed" in {
      val ex = new Exception
      val io = IO.failed(ex)
      Try(eval(io)) mustEqual Failure(ex)
    }

    "successful" in {
      val io = IO.successful(1)
      eval(io) mustEqual 1
    }

    "apply" - {
      "success" in {
        val io = IO(1)
        eval(io) mustEqual 1
      }
      "failure" in {
        val ex = new Exception
        val io = IO(throw ex)
        Try(eval(io)) mustEqual Failure(ex)
      }
    }

    "foldLeft" - {
      "success" in {
        val ios = List(IO(1), IO(2))
        val io =
          IO.foldLeft(ios)(0) {
            case (a, b) => a + b
          }
        eval(io) mustEqual 3
      }
      "empty" in {
        val io =
          IO.foldLeft(List.empty[IO[Int, Effect]])(0) {
            case (a, b) => a + b
          }
        eval(io) mustEqual 0
      }
      "failure" - {
        "ios" in {
          val ex = new Exception
          val ios = List(IO(1), IO.failed[Int](ex))
          val io =
            IO.foldLeft(ios)(0) {
              case (a, b) => a + b
            }
          Try(eval(io)) mustEqual Failure(ex)
        }
        "op" in {
          val ex = new Exception
          val ios = List(IO(1), IO(2))
          val io =
            IO.foldLeft(ios)(0) {
              case (a, b) => throw ex
            }
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }

    "reduceLeft" - {
      "success" in {
        val ios = List(IO(1), IO(2))
        val io =
          IO.reduceLeft(ios) {
            case (a, b) => a + b
          }
        eval(io) mustEqual 3
      }
      "empty" in {
        val io =
          IO.reduceLeft(List.empty[IO[Int, Effect]]) {
            case (a, b) => a + b
          }
        intercept[UnsupportedOperationException](eval(io))
      }
      "failure" - {
        "ios" in {
          val ex = new Exception
          val ios = List(IO(1), IO.failed[Int](ex))
          val io =
            IO.reduceLeft(ios) {
              case (a, b) => a + b
            }
          Try(eval(io)) mustEqual Failure(ex)
        }
        "op" in {
          val ex = new Exception
          val ios = List(IO(1), IO(2))
          val io =
            IO.reduceLeft(ios) {
              case (a, b) => throw ex
            }
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }

    "traverse" - {
      "empty" in {
        val ios = List.empty[IO[Int, Effect]]
        val io = IO.traverse(ios)(IO.successful)
        eval(io) mustEqual List()
      }
      "success" in {
        val ints = List(1, 2)
        val io = IO.traverse(ints)(IO.successful)
        eval(io) mustEqual ints
      }
      "failure" in {
        val ex = new Exception
        val ints = List(1, 2)
        val io = IO.traverse(ints)(_ => throw ex)
        Try(eval(io)) mustEqual Failure(ex)
      }
    }
  }

  "IO instance" - {

    "transformWith" - {
      "base io" - {
        "success" in {
          val t = Success(1)
          val io = IO.fromTry(t).transformWith(IO.fromTry)
          Try(eval(io)) mustEqual t
        }
        "failure" in {
          val t = Failure[Int](new Exception)
          val io = IO.fromTry(t).transformWith(IO.fromTry)
          Try(eval(io)) mustEqual t
        }
      }
      "transformed io" - {
        "success" in {
          val t = Success(1)
          val io = IO.successful(()).transformWith(_ => IO.fromTry(t))
          Try(eval(io)) mustEqual t
        }
        "failure" in {
          val t = Failure[Int](new Exception)
          val io = IO.successful(()).transformWith(_ => IO.fromTry(t))
          Try(eval(io)) mustEqual t
        }
      }
      "transformation" - {
        "success" in {
          val io = IO.successful(1).transformWith(t => IO.fromTry(t.map(_ + 1)))
          Try(eval(io)) mustEqual Success(2)
        }
        "failure" in {
          val ex = new Exception
          val io = IO.successful(()).transformWith(_ => throw ex)
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }

    "transform" - {
      "base io" - {
        "success" in {
          val t = Success(1)
          val io = IO.fromTry(t).transform(identity)
          Try(eval(io)) mustEqual t
        }
        "failure" in {
          val t = Failure[Int](new Exception)
          val io = IO.fromTry(t).transform(identity)
          Try(eval(io)) mustEqual t
        }
      }
      "transformed try" - {
        "success" in {
          val t = Success(1)
          val io = IO.successful(()).transform(_ => t)
          Try(eval(io)) mustEqual t
        }
        "failure" in {
          val t = Failure[Int](new Exception)
          val io = IO.successful(()).transform(_ => t)
          Try(eval(io)) mustEqual t
        }
      }
      "transformation" - {
        "success" in {
          val io = IO.successful(1).transform(_.map(_ + 1))
          Try(eval(io)) mustEqual Success(2)
        }
        "failure" in {
          val ex = new Exception
          val io = IO.successful(()).transform(_ => throw ex)
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }

    "lowerFromTry" - {
      "success" in {
        val t = Success(1)
        val io = IO.successful(t).lowerFromTry
        Try(eval(io)) mustEqual t
      }
      "failure" in {
        val t = Failure(new Exception)
        val io = IO.successful(t).lowerFromTry
        Try(eval(io)) mustEqual t
      }
    }

    "liftToTry" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t).liftToTry
        eval(io) mustEqual t
      }
      "failure" in {
        val t = Failure(new Exception)
        val io = IO.fromTry(t).liftToTry
        eval(io) mustEqual t
      }
    }

    "failed" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t).failed
        intercept[NoSuchElementException](eval(io))
      }
      "failure" in {
        val ex = new Exception
        val t = Failure(ex)
        val io = IO.fromTry(t).failed
        eval(io) mustEqual ex
      }
    }

    "map" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t).map(_ + 1)
        eval(io) mustEqual 2
      }
      "failure" in {
        val t = Failure[Int](new Exception)
        val io = IO.fromTry(t).map(_ + 1)
        Try(eval(io)) mustEqual t
      }
    }

    "flatMap" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t).flatMap(i => IO(i + 1))
        eval(io) mustEqual 2
      }
      "failure" in {
        val t = Failure[Int](new Exception)
        val io = IO.fromTry(t).flatMap(i => IO(i + 1))
        Try(eval(io)) mustEqual t
      }
    }

    "filter" - {
      "success" in {
        val io = IO(1).filter(_ == 1)
        eval(io) mustEqual 1
      }
      "failure" in {
        val io = IO(1).filter(_ == 2)
        intercept[NoSuchElementException](eval(io))
      }
    }

    "withFilter" - {
      "success" in {
        val io = IO(1).filter(_ == 1)
        eval(io) mustEqual 1
      }
      "failure" in {
        val io = IO(1).filter(_ == 2)
        intercept[NoSuchElementException](eval(io))
      }
    }

    "collect" - {
      "success" in {
        val io = IO(1).collect { case 1 => 2 }
        eval(io) mustEqual 2
      }
      "failure" in {
        val io = IO(1).collect { case 2 => 3 }
        intercept[NoSuchElementException](eval(io))
      }
    }

    "recover" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t).recover { case _ => 2 }
        eval(io) mustEqual 1
      }
      "failure" in {
        val t = Failure[Int](new Exception)
        val io = IO.fromTry(t).recover { case _ => 2 }
        eval(io) mustEqual 2
      }
    }

    "recoverWith" - {
      "success" in {
        val t = Success(1)
        val io = IO.fromTry(t).recoverWith { case _ => IO(2) }
        eval(io) mustEqual 1
      }
      "failure" in {
        val t = Failure[Int](new Exception)
        val io = IO.fromTry(t).recoverWith { case _ => IO(2) }
        eval(io) mustEqual 2
      }
      "flatMap and recoverWith" in {
        val evalCount = new AtomicInteger(0)
        val e = new Exception("failure")
        val io = IO.unit.map(_ => {
          resultValue[Int](evalCount.incrementAndGet())
        }).flatMap { _ =>
          IO.failed(e)
        }.recoverWith {
          case _: VirtualMachineError => IO.successful(-1) // won't match the error
        }
        Try(eval(io)) mustEqual Failure(e)
        evalCount.get() mustEqual 1
      }
    }

    "zip" - {
      "success" in {
        val io = IO(1).zip(IO(2))
        eval(io) mustEqual ((1, 2))
      }
      "failure" - {
        "left" in {
          val ex = new Exception
          val io = IO.failed(ex).zip(IO(2))
          Try(eval(io)) mustEqual Failure(ex)
        }
        "right" in {
          val ex = new Exception
          val io = IO(1).zip(IO.failed(ex))
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }

    "zipWith" - {
      "success" in {
        val io = IO(1).zip(IO(2))
        eval(io) mustEqual ((1, 2))
      }
      "failure" - {
        "left" in {
          val ex = new Exception
          val io = IO.failed(ex).zip(IO(2))
          Try(eval(io)) mustEqual Failure(ex)
        }
        "right" in {
          val ex = new Exception
          val io = IO(1).zip(IO.failed(ex))
          Try(eval(io)) mustEqual Failure(ex)
        }
      }
    }
  }

}
