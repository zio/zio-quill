package zio.quill.idiom

import scala.reflect.macros.whitebox.Context
import scala.util.Try

import zio.quill.NamingStrategy
import zio.quill.util.CollectTry
import zio.quill.util.LoadObject
import zio.quill.CompositeNamingStrategy

object LoadNaming {

  def static(c: Context)(tpe: c.Type): Try[NamingStrategy] =
    CollectTry {
      strategies(c)(tpe).map(LoadObject[NamingStrategy](c)(_))
    }.map(NamingStrategy(_))

  private def strategies(c: Context)(tpe: c.Type) =
    tpe <:< c.typeOf[CompositeNamingStrategy] match {
      case true =>
        tpe.typeArgs
          .filterNot(_ =:= c.weakTypeOf[NamingStrategy])
          .filterNot(_ =:= c.weakTypeOf[scala.Nothing])
      case false =>
        List(tpe)
    }
}
