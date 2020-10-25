package io.getquill.norm

import scala.util.Random
import io.getquill.ast._
import io.getquill.quat.Quat

class QueryGenerator(seed: Int) {

  private val random = new Random(seed)

  implicit class IdentExt(id: Ident) {
    def randomProperty =
      id.quat match {
        case Quat.Product(fields) =>
          Property(id, fields.toList(random.nextInt(fields.size))._1)
        case _ =>
          id
      }
  }

  def string(size: Int): String =
    size match {
      case 0    => ""
      case size => char + string(size - 1)
    }

  def char: String = {
    val letters = "abcdefghijklmnopqrstuvwxyz"
    letters.charAt(random.nextInt(letters.size)).toString
  }

  def apply(i: Int): Query = {
    if (i <= 2) {
      val quat = Quat.Product()
      val s = string(3)
      Entity(s, Nil, Quat.Product((1 to 20).map(i => (string(3), Quat.Value)).toList.distinct: _*))
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
  }

  private def take(i: Int) =
    Take(apply(i - 1), Constant.auto(i))

  private def drop(i: Int) =
    Drop(apply(i - 1), Constant.auto(i))

  private def map(i: Int) = {
    val q = apply(i)
    val id = Ident(char, q.quat)
    Map(q, id, id)
  }

  private def flatMap(i: Int) = {
    val (a, b) = distribute(i)
    val q = apply(a)
    FlatMap(q, Ident(char, q.quat), apply(b))
  }

  private def filter(i: Int) = {
    val q = apply(i)
    val id = Ident(char, q.quat)
    Filter(q, id, BinaryOperation(id.randomProperty, EqualityOperator.`!=`, Constant.auto(1)))
  }

  private def sortBy(i: Int) = {
    val q = apply(i)
    val id = Ident(char, q.quat)
    SortBy(apply(i), id, id.randomProperty, AscNullsFirst)
  }

  private def groupBy(i: Int) = {
    val q = apply(i)
    val id = Ident(char, q.quat)
    val group = GroupBy(q, id, id.randomProperty)
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
}

case class IsAggregated(state: Boolean = false) extends StatefulTransformer[Boolean] {

  override def apply(q: Query) =
    q match {
      case q: Aggregation => (q, IsAggregated(true))
      case other          => super.apply(q)
    }
}
