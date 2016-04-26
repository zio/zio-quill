package io.getquill.sources

import java.lang.StringBuilder
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

sealed trait Binding[S] {
  val index: Int
}

case class SingleBinding[S, T](index: Int, value: T, enc: Encoder[S, T]) extends Binding[S]
case class SetBinding[S, T](index: Int, values: Traversable[T], enc: Encoder[BindedStatementBuilder[S], T]) extends Binding[S]

class BindedStatementBuilder[S] {

  private val bindings = ListBuffer[Binding[S]]()
  private val HolderPattern = "(?<=\\s|\\()\\?".r

  def single[T](idx: Int, value: T, enc: Encoder[S, T]) = {
    bindings += SingleBinding[S, T](idx, value, enc)
    this
  }

  def coll[T](idx: Int, values: Traversable[T], enc: Encoder[BindedStatementBuilder[S], T]) = {
    bindings += SetBinding[S, T](idx, values, enc)
    this
  }

  def build(query: String) = {

    @tailrec
    def expand(q: CharSequence, b: List[Binding[S]], sb: StringBuilder): StringBuilder = {
      val fm = HolderPattern.findFirstMatchIn(q)
      (fm, b) match {
        case (Some(m), (b: SetBinding[_, _]) :: btail) =>
          val expanded = List.fill(b.values.size)('?').mkString(", ")
          sb.append(m.before).append(expanded)
          expand(m.after, btail, sb)
        case (Some(m), (b: SingleBinding[_, _]) :: btail) =>
          sb.append(m.before).append('?')
          expand(m.after, btail, sb)
        case (None, Nil) =>
          sb.append(q)
        case _ =>
          throw new IllegalStateException("Number of bindings doesn't match the question marks.")
      }
    }

    val expandedQuery = expand(query, bindings.toList.sortBy(_.index), new StringBuilder)

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
    (expandedQuery.toString, (setValues _).andThen(_._1))
  }
}
