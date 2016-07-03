package io.getquill.util

import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.util.Messages.RichContext

object LoadObject {

  def apply[T: ClassTag](c: Context)(typ: c.Type) = {
    LoadClass(typ.typeSymbol.fullName.trim + "$").toOption
      .map(cls => cls.getField("MODULE$").get(cls)) match {
        case Some(d: T) => d
        case other      => c.fail(s"Can't load object '${typ.typeSymbol.fullName}'.")
      }
  }
}
