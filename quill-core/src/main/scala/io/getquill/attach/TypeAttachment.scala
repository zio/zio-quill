package io.getquill.attach

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox.Context

case class Attachment(data: Any) extends StaticAnnotation

trait TypeAttachment {
  val c: Context
  import c.universe._

  class Attachable[T](implicit t: WeakTypeTag[T]) {

    val typ =
      if (!t.tpe.typeSymbol.asClass.isTrait)
        t.tpe.baseType(t.tpe.baseClasses.find(_.asClass.isTrait).get)
      else t.tpe

    def attach[M](metadata: M)(implicit lift: Liftable[M]): Tree =
      attach[M](q"()", metadata)

    def attach[M](data: Tree, metadata: M)(implicit lift: Liftable[M]) =
      q"""
        new $typ {
           def data = $data
           @${c.weakTypeOf[Attachment]}($metadata)
           def metadata = $metadata
        }  
      """
  }

  def to[T](implicit t: WeakTypeTag[T]) =
    new Attachable[T]

  def attachmentMetadata[D](tree: Tree)(implicit unlift: Unliftable[D]) = {
    val method =
      tree.tpe.decls.find(_.name.decodedName.toString == "metadata")
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the metadata method at '${tree.tpe}'."))
    val annotation =
      method.annotations.headOption
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the metadata annotation at '$method'."))
    val data =
      annotation.tree.children.lastOption
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the data field from metadata annotation '$annotation'."))
    unlift.unapply(data)
      .getOrElse(c.abort(c.enclosingPosition, s"Can't unlift metadata '$data'."))
  }

  def attachmentDataTypeSymbol(tree: Tree) =
    tree.tpe.member(TermName("data")).typeSignature.typeSymbol.asType

  def attachmentData(tree: Tree) =
    q"$tree.data"

  def debugg[T](v: T) = {
    c.info(c.enclosingPosition, v.toString(), false)
    v
  }
}
