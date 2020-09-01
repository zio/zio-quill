package io.getquill.quotation

import io.getquill.util.MacroContextExt._

import scala.collection.mutable.LinkedHashMap
import scala.reflect.macros.whitebox.Context
import io.getquill.quat.Quat

trait QuatUnliftable {
  val c: Context
  import c.universe.{ Constant => _, Function => _, Ident => _, If => _, _ }

  implicit val quatProductUnliftable: Unliftable[Quat.Product] = Unliftable[Quat.Product] {
    // On JVM, a Quat must be serialized and then lifted from the serialized state i.e. as a FromSerialized using JVM (due to 64KB method limit)
    case q"$pack.Quat.Product.fromSerializedJVM(${ str: String })" => Quat.Product.fromSerializedJVM(str)
  }

  implicit val quatUnliftable: Unliftable[Quat] = Unliftable[Quat] {
    // On JVM, a Quat must be serialized and then lifted from the serialized state i.e. as a FromSerialized using JVM (due to 64KB method limit)
    case q"$pack.Quat.fromSerializedJVM(${ str: String })" => Quat.fromSerializedJVM(str)
  }

  implicit def linkedHashMapUnliftable[K, V](implicit uk: Unliftable[K], uv: Unliftable[V]): Unliftable[LinkedHashMap[K, V]] = Unliftable[LinkedHashMap[K, V]] {
    case q"$pack.LinkedHashMap.apply[..$t](..$values)" =>
      LinkedHashMap[K, V](
        values.map {
          case q"($k, $v)" =>
            (
              uk.unapply(k).getOrElse(c.fail(s"Can't unlift $k")),
              uv.unapply(v).getOrElse(c.fail(s"Can't unlift $v"))
            )
        }: _*
      )
  }

  implicit def listUnliftable[T](implicit u: Unliftable[T]): Unliftable[List[T]] = Unliftable[List[T]] {
    case q"$pack.Nil"                         => Nil
    case q"$pack.List.apply[..$t](..$values)" => values.map(v => u.unapply(v).getOrElse(c.fail(s"Can't unlift $v")))
  }
}
