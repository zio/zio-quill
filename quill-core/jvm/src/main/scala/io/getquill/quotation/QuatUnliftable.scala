package io.getquill.quotation

import io.getquill.util.MacroContextExt._

import scala.collection.mutable.LinkedHashMap
import scala.reflect.macros.whitebox.Context
import io.getquill.quat.Quat

trait QuatUnliftable {
  val mctx: Context
  import mctx.universe.{ Constant => _, Function => _, Ident => _, If => _, _ }

  implicit val quatProductUnliftable: Unliftable[Quat.Product] = Unliftable[Quat.Product] {
    // On JVM, a Quat must be serialized and then lifted from the serialized state i.e. as a FromSerialized using JVM (due to 64KB method limit)
    case q"$pack.Quat.Product.fromSerializedJVM(${ str: String })" => Quat.Product.fromSerializedJVM(str)
    case q"$pack.Quat.Product.WithRenames.apply(${ fields: LinkedHashMap[String, Quat] }, ${ renames: List[(String, String)] })" => Quat.Product.WithRenames(fields, renames)
  }

  implicit val quatUnliftable: Unliftable[Quat] = Unliftable[Quat] {
    // On JVM, a Quat must be serialized and then lifted from the serialized state i.e. as a FromSerialized using JVM (due to 64KB method limit)
    case q"$pack.Quat.fromSerializedJVM(${ str: String })" => Quat.fromSerializedJVM(str)
    case q"$pack.Quat.Product.WithRenames.apply(${ fields: LinkedHashMap[String, Quat] }, ${ renames: List[(String, String)] })" => Quat.Product.WithRenames(fields, renames)
    case q"$pack.Quat.Product.apply(${ fields: List[(String, Quat)] })" => Quat.Product(fields)
    case q"$pack.Quat.Value" => Quat.Value
    case q"$pack.Quat.Null" => Quat.Null
    case q"$pack.Quat.Generic" => Quat.Generic
    case q"$pack.Quat.BooleanValue" => Quat.BooleanValue
    case q"$pack.Quat.BooleanExpression" => Quat.BooleanExpression
  }

  implicit def linkedHashMapUnliftable[K, V](implicit uk: Unliftable[K], uv: Unliftable[V]): Unliftable[LinkedHashMap[K, V]] = Unliftable[LinkedHashMap[K, V]] {
    case q"$pack.LinkedHashMap.apply[..$t](..$values)" =>
      LinkedHashMap[K, V](
        values.map {
          case q"($k, $v)" =>
            (
              uk.unapply(k).getOrElse(mctx.fail(s"Can't unlift $k")),
              uv.unapply(v).getOrElse(mctx.fail(s"Can't unlift $v"))
            )
        }: _*
      )
  }

  implicit def listUnliftable[T](implicit u: Unliftable[T]): Unliftable[List[T]] = Unliftable[List[T]] {
    case q"$pack.Nil"                         => Nil
    case q"$pack.List.apply[..$t](..$values)" => values.map(v => u.unapply(v).getOrElse(mctx.fail(s"Can't unlift $v")))
  }
}
