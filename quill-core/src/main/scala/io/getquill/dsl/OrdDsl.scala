package io.getquill.dsl

import io.getquill.quotation.NonQuotedException
import scala.annotation.compileTimeOnly

private[dsl] trait OrdDsl {

  trait Ord[T]

  @compileTimeOnly(NonQuotedException.message)
  implicit def implicitOrd[T]: Ord[T] = NonQuotedException()

  object Ord {

    @compileTimeOnly(NonQuotedException.message)
    def asc[T]: Ord[T] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def desc[T]: Ord[T] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def ascNullsFirst[T]: Ord[T] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def descNullsFirst[T]: Ord[T] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def ascNullsLast[T]: Ord[T] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def descNullsLast[T]: Ord[T] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2](o1: Ord[T1], o2: Ord[T2]): Ord[(T1, T2)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3]): Ord[(T1, T2, T3)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4]): Ord[(T1, T2, T3, T4)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4, T5](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5]): Ord[(T1, T2, T3, T4, T5)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4, T5, T6](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6]): Ord[(T1, T2, T3, T4, T5, T6)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4, T5, T6, T7](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7]): Ord[(T1, T2, T3, T4, T5, T6, T7)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4, T5, T6, T7, T8](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7], o8: Ord[T8]): Ord[(T1, T2, T3, T4, T5, T6, T7, T8)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7], o8: Ord[T8], o9: Ord[T9]): Ord[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] = NonQuotedException()

    @compileTimeOnly(NonQuotedException.message)
    def apply[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](o1: Ord[T1], o2: Ord[T2], o3: Ord[T3], o4: Ord[T4], o5: Ord[T5], o6: Ord[T6], o7: Ord[T7], o8: Ord[T8], o9: Ord[T9], o10: Ord[T10]): Ord[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = NonQuotedException()
  }
}
