package io.getquill.quotation

import io.getquill.quat.Quat
import io.getquill.util.MacroContextExt._

import scala.reflect.macros.whitebox.Context

trait QuatUnliftable {
  val mctx: Context
  import mctx.universe.{Constant => _, Function => _, Ident => _, If => _, _}

  def unliftQuat(v: Tree): Quat                                         = quatUnliftable.unapply(v).getOrElse(mctx.fail(s"Can't unlift $v"))
  def unliftQuats(v: Seq[Tree]): Seq[Quat]                                   = v.map(unliftQuat(_))
  def unliftString(v: Tree)(implicit u: Unliftable[String]): String       = u.unapply(v).getOrElse(mctx.fail(s"Can't unlift $v"))
  def unliftStrings(v: Seq[Tree])(implicit u: Unliftable[String]): Seq[String] = v.map(unliftString(_))

  implicit val quatProductUnliftable: Unliftable[Quat.Product] = Unliftable[Quat.Product] {
    // On JVM, a Quat must be serialized and then lifted from the serialized state i.e. as a FromSerialized using JVM (due to 64KB method limit)
    case q"$pack.Quat.Product.fromSerialized(${str: String})" => Quat.Product.fromSerialized(str)
    case q"$pack.Quat.Product.WithRenamesCompact.apply(${name: String}, ${tpe: Quat.Product.Type})(..$fields)(..$values)(..$renamesFrom)(..$renamesTo)" =>
      Quat.Product.WithRenamesCompact(name, tpe)(unliftStrings(fields): _*)(unliftQuats(values): _*)(
        unliftStrings(renamesFrom): _*
      )(unliftStrings(renamesTo): _*)
  }

  implicit val quatProductTypeUnliftable: Unliftable[Quat.Product.Type] = Unliftable[Quat.Product.Type] {
    case q"$pack.Quat.Product.Type.Concrete" => Quat.Product.Type.Concrete
    case q"$pack.Quat.Product.Type.Abstract" => Quat.Product.Type.Abstract
  }

  implicit val quatUnliftable: Unliftable[Quat] = Unliftable[Quat] {
    // On JVM, a Quat must be serialized and then lifted from the serialized state i.e. as a FromSerialized using JVM (due to 64KB method limit)
    case q"$pack.Quat.fromSerialized(${str: String})" => Quat.fromSerialized(str)
    case q"$pack.Quat.Product.WithRenamesCompact.apply(${name: String}, ${tpe: Quat.Product.Type})(..$fields)(..$values)(..$renamesFrom)(..$renamesTo)" =>
      Quat.Product.WithRenamesCompact(name, tpe)(unliftStrings(fields): _*)(unliftQuats(values): _*)(
        unliftStrings(renamesFrom): _*
      )(unliftStrings(renamesTo): _*)
    case q"$pack.Quat.Product.apply(${name: String}, ${fields: List[(String, Quat)]})" => Quat.Product(name, fields)
    case q"$pack.Quat.Value"                                                           => Quat.Value
    case q"$pack.Quat.Null"                                                            => Quat.Null
    case q"$pack.Quat.Generic"                                                         => Quat.Generic
    case q"$pack.Quat.Unknown"                                                         => Quat.Unknown
    case q"$pack.Quat.BooleanValue"                                                    => Quat.BooleanValue
    case q"$pack.Quat.BooleanExpression"                                               => Quat.BooleanExpression
  }

  implicit def listUnliftable[T](implicit u: Unliftable[T]): Unliftable[List[T]] = Unliftable[List[T]] {
    case q"$pack.Nil"                         => Nil
    case q"$pack.List.apply[..$t](..$values)" => values.map(v => u.unapply(v).getOrElse(mctx.fail(s"Can't unlift $v")))
  }
}
