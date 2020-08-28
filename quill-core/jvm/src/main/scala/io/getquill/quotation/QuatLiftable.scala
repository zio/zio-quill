package io.getquill.quotation

import scala.reflect.macros.whitebox.Context
import io.getquill.quat.Quat

import scala.collection.mutable

trait QuatLiftable {
  val c: Context

  import c.universe.{ Ident => _, Constant => _, Function => _, If => _, Block => _, _ }

  // Liftables are invariant so want to have a separate liftable for Quat.Product since it is used directly inside Entity
  // which should be lifted/unlifted without the need for casting.
  implicit val quatProductLiftable: Liftable[Quat.Product] = Liftable[Quat.Product] {
    // If we are in the JVM, use Kryo to serialize our Quat due to JVM 64KB method limit that we will run into of the Quat Constructor
    // if plainly lifted into the method created by our macro (i.e. the 'ast' method).
    case quat: Quat => q"io.getquill.quat.Quat.Product.fromSerializedJVM(${quat.serializeJVM})"
  }

  implicit val quatLiftable: Liftable[Quat] = Liftable[Quat] {
    case quat: Quat => q"io.getquill.quat.Quat.fromSerializedJVM(${quat.serializeJVM})"
  }

  implicit def linkedHashMapLiftable[K, V](implicit lk: Liftable[K], lv: Liftable[V]): Liftable[mutable.LinkedHashMap[K, V]] = Liftable[mutable.LinkedHashMap[K, V]] {
    case l: mutable.LinkedHashMap[K, V] =>
      // Do not splice using :_* otherwise semanticdb in metals throws syntaxError
      val args = l.map { case (k, v) => q"(${lk(k)}, ${lv(v)})" }.toList
      q"scala.collection.mutable.LinkedHashMap(..$args)"
  }
}