package io.getquill

import io.getquill.ast.{ Asc, AscNullsFirst, AscNullsLast, Desc, DescNullsFirst, DescNullsLast, Ordering, TupleOrdering }

case class Ord[T](ord: Ordering)

object Ord {

  def asc[T]: Ord[T] = Ord(Asc)

  def desc[T]: Ord[T] = Ord(Desc)

  def ascNullsFirst[T]: Ord[T] = Ord(AscNullsFirst)

  def descNullsFirst[T]: Ord[T] = Ord(DescNullsFirst)

  def ascNullsLast[T]: Ord[T] = Ord(AscNullsLast)

  def descNullsLast[T]: Ord[T] = Ord(DescNullsLast)

  def apply[T1, T2](o1: Ord[T1], o2: Ord[T2]): Ord[(T1, T2)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord)))

  def apply[T1, T2, T3](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3]): Ord[(T1, T2, T3)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord)))

  def apply[T1, T2, T3, T4](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4]): Ord[(T1, T2, T3, T4)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord)))

  def apply[T1, T2, T3, T4, T5](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5]): Ord[(T1, T2, T3, T4, T5)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord, o5.ord)))

  def apply[T1, T2, T3, T4, T5, T6](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6]): Ord[(T1, T2, T3, T4, T5, T6)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord, o5.ord, o6.ord)))

  def apply[T1, T2, T3, T4, T5, T6, T7](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7]): Ord[(T1, T2, T3, T4, T5, T6, T7)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord, o5.ord, o6.ord, o7.ord)))

  def apply[T1, T2, T3, T4, T5, T6, T7, T8](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7], o8: Ord[T8]): Ord[(T1, T2, T3, T4, T5, T6, T7, T8)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord, o5.ord, o6.ord, o7.ord, o8.ord)))

  def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7], o8: Ord[T8], o9: Ord[T9]): Ord[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord, o5.ord, o6.ord, o7.ord, o8.ord, o9.ord)))

  def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7], o8: Ord[T8], o9: Ord[T9], o10: Ord[T10]): Ord[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] =
    Ord(TupleOrdering(List(o1.ord, o2.ord, o3.ord, o4.ord, o5.ord, o6.ord, o7.ord, o8.ord, o9.ord, o10.ord)))
}
