package io.getquill.norm

import io.getquill.ast
import io.getquill.ast._
import scala.util.Random

class QueryGenerator(seed: Int) {

  private val random = new Random(seed)

  def apply(i: Int): Query =
    if (i <= 2) {
      Entity(string)
    } else {
      random.nextInt(4) match {
        case 0 => map(i)
        case 1 => flatMap(i)
        case 2 => filter(i)
        case 3 => sortBy(i)
      }
    }

  private def map(i: Int) = {
    val id = ident
    Map(apply(i), id, id)
  }

  private def flatMap(i: Int) = {
    val (a, b) = distribute(i)
    FlatMap(apply(a), ident, apply(b))
  }

  private def filter(i: Int) = {
    val id = ident
    if (i % 2 == 0)
      Filter(apply(i), id, BinaryOperation(id, ast.`!=`, NullValue))
    else
      Filter(apply(i), id, BinaryOperation(Property(id, string), ast.`!=`, Constant(1)))
  }

  private def sortBy(i: Int) = {
    val id = ident
    if (i % 2 == 0)
      SortBy(apply(i), id, id)
    else
      SortBy(apply(i), id, Property(id, string))
  }

  private def distribute(i: Int) = {
    val j = random.nextInt(i - 2) + 1
    val k = i - j
    (j, k)
  }

  private def ident =
    Ident(string)

  private def string = {
    val letters = "abcdefghijklmnopqrstuvwxyz"
    letters.charAt(random.nextInt(letters.size)).toString
  }

}
