package io.getquill.util

import scala.reflect.macros.whitebox.Context
import scala.util.Try

object LoadObject {

  def apply[T](c: Context)(tpe: c.Type): Try[T] =
    Try {
      val cls = Class.forName(tpe.typeSymbol.fullName + "$")
      val field = cls.getField("MODULE$")
      field.get(cls).asInstanceOf[T]
    }
}
