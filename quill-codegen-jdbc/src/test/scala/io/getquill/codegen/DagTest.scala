package io.getquill.codegen

import java.time.LocalDateTime

import org.scalatest._
import Matchers._
import io.getquill.codegen.dag.CatalogBasedAncestry

import scala.reflect.{ ClassTag, classTag }

class CodeGeneratorRunnerDagTest extends FunSuite with BeforeAndAfter {

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
    TestCase(classTag[java.time.LocalDate], Seq(classTag[LocalDateTime]), classTag[LocalDateTime])
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
