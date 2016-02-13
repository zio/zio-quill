package io.getquill.util

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context
import scala.reflect.ClassTag

object LoadObject {

  def apply[T: ClassTag](c: Context)(typ: c.Type) = {
    LoadClass(typ.typeSymbol.fullName.trim + "$").toOption
      .map(cls => cls.getField("MODULE$").get(cls)) match {
        case Some(d: T) => d
        case other      => c.fail(s"Can't load object '${typ.typeSymbol.fullName}'.")
      }
  }
}
