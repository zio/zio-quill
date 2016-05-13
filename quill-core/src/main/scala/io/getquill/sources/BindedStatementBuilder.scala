package io.getquill.sources

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

sealed trait Binding[S] {
  val index: Int
}

case class SingleBinding[S, T](index: Int, value: T, enc: Encoder[S, T]) extends Binding[S]
case class SetBinding[S, T](index: Int, values: Traversable[T], enc: Encoder[BindedStatementBuilder[S], T]) extends Binding[S]

class BindedStatementBuilder[S] {

  private val bindings = ListBuffer[Binding[S]]()

  def single[T](idx: Int, value: T, enc: Encoder[S, T]) = {
    bindings += SingleBinding[S, T](idx, value, enc)
    this
  }

  def coll[T](idx: Int, values: Traversable[T], enc: Encoder[BindedStatementBuilder[S], T]) = {
    bindings += SetBinding[S, T](idx, values, enc)
    this
  }

  def build(query: String): (String, (S => S)) = {

    @tailrec
    def expand(q: List[Char], b: List[Binding[S]], acc: List[Char]): List[Char] =
      (q, b) match {
        case (Nil, Nil) =>
          acc
        case ('\'' :: qtail, b) => // skip quote
          @tailrec def extractQuote(q: List[Char], acc: List[Char]): (List[Char], List[Char]) =
            q match {
              case '\\' :: '\'' :: tail => extractQuote(tail, acc :+ '\\' :+ '\'')
              case '\'' :: tail         => (acc :+ '\'', tail)
              case head :: tail         => extractQuote(tail, acc :+ head)
              case Nil                  => (acc, Nil)
            }
          val (quote, tail) = extractQuote(qtail, List('\''))
          expand(tail, b, acc ++ quote)
        case (c :: qtail, b) if (c != '?') =>
          expand(qtail, b, acc :+ c)
        case ('?' :: qtail, (bind: SingleBinding[_, _]) :: btail) =>
          expand(qtail, btail, acc :+ '?')
        case ('?' :: qtail, (bind: SetBinding[_, _]) :: btail) =>
          val expanded = if (bind.values.isEmpty) {
            emptySet.toList
          } else {
            List.fill(bind.values.size)('?').mkString(", ").toList
          }
          expand(qtail, btail, acc ++ expanded)
        case other =>
          throw new IllegalStateException("Number of bindings doesn't match the question marks.")
      }

    def expandedQuery = expand(query.toList, bindings.toList.sortBy(_.index), List.empty)

    def setValues(p: S) =
      bindings.foldLeft((p, 0)) {
        case ((p, i), SingleBinding(_, v, enc)) =>
          (enc(i, v, p), i + 1)
        case ((p, i), SetBinding(_, values, enc)) =>
          values.foldLeft((p, i)) {
            case ((p, i), v) =>
              val temp = new BindedStatementBuilder[S]
              enc(i, v, temp)
              temp.bindings.toList match {
                case List(SingleBinding(_, v, raw)) =>
                  (raw(i, v, p), i + 1)
              }

          }
      }

    if (bindings.isEmpty)
      (query, identity[S] _)
    else
      (expandedQuery.mkString, (setValues _).andThen(_._1))
  }

  def emptySet = ""
}
