package io.getquill.attach

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox.Context
import scala.reflect.ClassTag

case class Attachment(hash: Int, data: Any) extends StaticAnnotation

trait TypeAttachment {
  val c: Context
  import c.universe._

  class Attachable[T] {

    def apply[M](attachment: M)(implicit lift: Liftable[M], t: WeakTypeTag[T]) = {
      val typ =
        if (!t.tpe.typeSymbol.asClass.isTrait)
          t.tpe.baseType(t.tpe.baseClasses.find(_.asClass.isTrait).get)
        else t.tpe
      Cache.update(attachment)
      q"""
        new $typ {
           @${c.weakTypeOf[Attachment]}(${attachment.hashCode}, $attachment)
           def attachment = $attachment
        }  
      """
    }
  }

  def attach[T] =
    new Attachable[T]

  def detach[D](tree: Tree)(implicit unlift: Unliftable[D]) = {
    val method =
      tree.tpe.decls.find(_.name.decodedName.toString == "attachment")
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the attachment method at '${tree.tpe}'. $tree"))
    val annotation =
      method.annotations.headOption
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the attachment annotation at '$method'. $tree"))
    val q"${ hash: Int }" :: data :: Nil = annotation.tree.children.drop(1).toList
    Cache.getOrElseUpdate(hash) {
      unlift.unapply(data)
        .getOrElse(c.abort(c.enclosingPosition, s"Can't unlift attachment '$data'."))
    }
  }
}
