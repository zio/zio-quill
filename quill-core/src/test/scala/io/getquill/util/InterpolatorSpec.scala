package io.getquill.util

import io.getquill.base.Spec
import java.io.{ByteArrayOutputStream, PrintStream}
import io.getquill.util.Messages.TraceType.Standard

class InterpolatorSpec extends Spec {

  val interp =
    new Interpolator(Standard, TraceConfig.Empty, defaultIndent = 0, color = false, globalTracesEnabled = _ => true)
  import interp._

  case class Small(id: Int)
  val small = Small(123)

  "traces small objects on single line - single" in {
    trace"small object: $small".generateString() mustEqual (("small object: Small(123) ", 0))
  }

  "traces multiple small objects on single line" in {
    trace"small object: $small and $small".generateString() mustEqual (("small object: Small(123) and Small(123) ", 0))
  }

  "traces multiple small objects multiline text" in {
    trace"""small object: $small and foo
and bar $small""".generateString() mustEqual (
      (
        """small object:
          ||  Small(123)
          ||and foo
          ||and bar
          ||  Small(123)
          |""".stripMargin,
        0
      )
    )
  }

  case class Large(
    id: Int,
    one: String,
    two: String,
    three: String,
    four: String,
    five: String,
    six: String,
    seven: String,
    eight: String,
    nine: String,
    ten: String
  )
  val vars  = (0 until 10).map(i => (0 until i).map(_ => "Test").mkString("")).toList
  val large = Large(123, vars(0), vars(1), vars(2), vars(3), vars(4), vars(5), vars(6), vars(7), vars(8), vars(9))

  "traces large objects on multiple line - single" in {
    trace"large object: $large".generateString() mustEqual ((
      """large object:
        ||  Large(
        ||    123,
        ||    "",
        ||    "Test",
        ||    "TestTest",
        ||    "TestTestTest",
        ||    "TestTestTestTest",
        ||    "TestTestTestTestTest",
        ||    "TestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTestTestTest"
        ||  )
        |""".stripMargin,
      0
    ))
  }

  "traces large objects on multiple line - single - custom indent" in {
    trace"%2 large object: $large".generateString() mustEqual ((
      """    large object:
        |    |  Large(
        |    |    123,
        |    |    "",
        |    |    "Test",
        |    |    "TestTest",
        |    |    "TestTestTest",
        |    |    "TestTestTestTest",
        |    |    "TestTestTestTestTest",
        |    |    "TestTestTestTestTestTest",
        |    |    "TestTestTestTestTestTestTest",
        |    |    "TestTestTestTestTestTestTestTest",
        |    |    "TestTestTestTestTestTestTestTestTest"
        |    |  )
        |""".stripMargin,
      2
    ))
  }

  "traces large objects on multiple line - multi" in {
    trace"large object: $large and $large".generateString() mustEqual ((
      """large object:
        ||  Large(
        ||    123,
        ||    "",
        ||    "Test",
        ||    "TestTest",
        ||    "TestTestTest",
        ||    "TestTestTestTest",
        ||    "TestTestTestTestTest",
        ||    "TestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTestTestTest"
        ||  )
        ||and
        ||  Large(
        ||    123,
        ||    "",
        ||    "Test",
        ||    "TestTest",
        ||    "TestTestTest",
        ||    "TestTestTestTest",
        ||    "TestTestTestTestTest",
        ||    "TestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTestTest",
        ||    "TestTestTestTestTestTestTestTestTest"
        ||  )
        |""".stripMargin,
      0
    ))
  }

  "should log to print stream" - {
    "do not log if traces disabled" in {
      val buff = new ByteArrayOutputStream()
      val ps   = new PrintStream(buff)
      val interp = new Interpolator(
        Standard,
        TraceConfig.Empty,
        defaultIndent = 0,
        color = false,
        globalTracesEnabled = _ => false,
        out = ps
      )
      import interp._

      trace"small object: $small".andLog()
      ps.flush()
      buff.toString mustEqual ""
    }

    "log if traces disabled" in {
      val buff = new ByteArrayOutputStream()
      val ps   = new PrintStream(buff)
      val interp = new Interpolator(
        Standard,
        TraceConfig.Empty,
        defaultIndent = 0,
        color = false,
        globalTracesEnabled = _ => true,
        out = ps
      )
      import interp._

      trace"small object: $small".andLog()
      ps.flush()
      buff.toString mustEqual "small object: Small(123) \n"
    }

    "traces large objects on multiple line - multi - with return" in {
      val buff = new ByteArrayOutputStream()
      val ps   = new PrintStream(buff)
      val interp = new Interpolator(
        Standard,
        TraceConfig.Empty,
        defaultIndent = 0,
        color = false,
        globalTracesEnabled = _ => true,
        out = ps
      )
      import interp._

      trace"large object: $large and $large".andReturn(large) mustEqual large

      buff.toString mustEqual (
        """large object:
          ||  Large(
          ||    123,
          ||    "",
          ||    "Test",
          ||    "TestTest",
          ||    "TestTestTest",
          ||    "TestTestTestTest",
          ||    "TestTestTestTestTest",
          ||    "TestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTestTestTest"
          ||  )
          ||and
          ||  Large(
          ||    123,
          ||    "",
          ||    "Test",
          ||    "TestTest",
          ||    "TestTestTest",
          ||    "TestTestTestTest",
          ||    "TestTestTestTestTest",
          ||    "TestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTestTestTest"
          ||  )
          |
          |>
          ||  Large(
          ||    123,
          ||    "",
          ||    "Test",
          ||    "TestTest",
          ||    "TestTestTest",
          ||    "TestTestTestTest",
          ||    "TestTestTestTestTest",
          ||    "TestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTestTest",
          ||    "TestTestTestTestTestTestTestTestTest"
          ||  )
          |""".stripMargin
      )
    }
  }
}
