package io.getquill.codegen

import java.time.LocalDateTime

import io.getquill.codegen.dag.CatalogBasedAncestry
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

import scala.reflect.{ ClassTag, classTag }

// I.e. something the type-ancestry does not know about
class UnknownClass

class CodeGeneratorRunnerDagTest extends AnyFunSuite with BeforeAndAfter {

  case class TestCase[O](one: ClassTag[_], twos: Seq[ClassTag[_]], result: ClassTag[_])

  val cases = Seq(
    TestCase(classTag[Int], Seq(classTag[Long]), classTag[Long]),
    TestCase(classTag[Long], Seq(classTag[Boolean], classTag[Int], classTag[Byte], classTag[Long]), classTag[Long]),
    TestCase(classTag[Int], Seq(classTag[Boolean], classTag[Int], classTag[Byte]), classTag[Int]),
    TestCase(
      classTag[BigDecimal],
      Seq(
        classTag[Boolean], classTag[Int], classTag[Byte], classTag[Long], classTag[BigDecimal]
      ),
      classTag[BigDecimal]
    ),
    TestCase(
      classTag[String],
      Seq(
        classTag[Boolean], classTag[Int], classTag[Long], classTag[Byte],
        classTag[BigDecimal], classTag[java.time.LocalDate], classTag[java.time.LocalDateTime]
      ),
      classTag[String]
    ),
    TestCase(classTag[java.time.LocalDate], Seq(classTag[LocalDateTime]), classTag[LocalDateTime]),
    TestCase(classTag[Short], Seq(classTag[Boolean], classTag[Byte]), classTag[Short]),
    TestCase(classTag[Short], Seq(classTag[Int]), classTag[Int]),
    TestCase(classTag[Int], Seq(classTag[Short]), classTag[Int]),
    TestCase(classTag[UnknownClass], Seq(classTag[String]), classTag[String]),
    TestCase(classTag[UnknownClass], Seq(classTag[UnknownClass]), classTag[UnknownClass]),
    // Don't know ancestry of unknown class to an Int (or any kind) so go directly to root of the ancestry i.e. String.
    TestCase(classTag[UnknownClass], Seq(classTag[Int]), classTag[String])
  )

  val casesIter = for {
    cas <- cases
    two <- cas.twos
  } yield (cas.one, two, cas.result)

  casesIter.foreach({
    case (one, two, expected) =>
      test(s"Common Ancestry between ${one} and ${two} should be ${expected}") {
        new CatalogBasedAncestry().apply(one, two) should equal(expected)
      }
  })
}
