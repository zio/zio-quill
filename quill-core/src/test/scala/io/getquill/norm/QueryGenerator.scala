package io.getquill.norm

import scala.util.Random

import io.getquill.ast
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Constant
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.NullValue
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.SortBy

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
