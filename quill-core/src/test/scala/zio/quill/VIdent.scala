package zio.quill

import zio.quill.ast.{Ast, Ident}
import zio.quill.quat.Quat

/**
 * For unit tests that do not care about the `Quat` of an ident, use this class
 * to construct an Ident with Quat.Value and ignore the Quat when
 * deconstructing.
 */
object VIdent {
  def unapply(id: Ast): Option[String] =
    id match {
      case Ident(name, _) => Some(name)
      case _              => None
    }

  def apply(value: String) =
    Ident(value, Quat.Value)
}
