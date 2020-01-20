package io.getquill.norm

import scala.util.Random
import io.getquill.ast._

class QueryGenerator(seed: Int) {

  private val random = new Random(seed)

  def apply(i: Int): Query =
    if (i <= 2) {
      val s = string(3)
      Entity(s, Nil)
    } else {
      random.nextInt(8) match {
        case 0 => map(i)
        case 1 => flatMap(i)
        case 2 => filter(i)
        case 3 => sortBy(i)
        case 4 => take(i)
        case 5 => drop(i)
        case 6 => groupBy(i)
        case 7 => aggregation(i)
      }
    }

  private def take(i: Int) =
    Take(apply(i - 1), Constant(i))

  private def drop(i: Int) =
    Drop(apply(i - 1), Constant(i))

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
    Filter(apply(i), id, BinaryOperation(Property(id, string), EqualityOperator.`!=`, Constant(1)))
  }

  private def sortBy(i: Int) = {
    val id = ident
    SortBy(apply(i), id, Property(id, string), AscNullsFirst)
  }

  private def groupBy(i: Int) = {
    val id = ident
    val group = GroupBy(apply(i), id, Property(id, string))
    Map(group, id, id)
  }

  private def aggregation(i: Int) = {
    val m = map(i)
    IsAggregated()(m)._2.state match {
      case true  => m
      case false => Aggregation(AggregationOperator.max, m)
    }
  }

  private def distribute(i: Int) = {
    val j = random.nextInt(i - 2) + 1
    val k = i - j
    (j, k)
  }

  private def ident =
    Ident(string)

  private def string(size: Int): String =
    size match {
      case 0    => ""
      case size => string + string(size - 1)
    }

  private def string: String = {
    val letters = "abcdefghijklmnopqrstuvwxyz"
    letters.charAt(random.nextInt(letters.size)).toString
  }
}

case class IsAggregated(state: Boolean = false) extends StatefulTransformer[Boolean] {

  override def apply(q: Query) =
    q match {
      case q: Aggregation => (q, IsAggregated(true))
      case other          => super.apply(q)
    }
}
