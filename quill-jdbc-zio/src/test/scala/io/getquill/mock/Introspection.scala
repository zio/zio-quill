package io.getquill.mock

import scala.reflect.ClassTag
import scala.reflect.runtime.{ universe => ru }

class Introspection[T](t: T)(implicit tt: ru.TypeTag[T], ct: ClassTag[T]) {
  import ru._

  val rm = runtimeMirror(getClass.getClassLoader)
  val instanceMirror = rm.reflect(t)

  val fieldsAndValues =
    typeOf[T].members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }.map(sym => {
      (sym.name.toString, instanceMirror.reflectField(sym.asTerm).get)
    }).toList.reverse

  val map = fieldsAndValues.toMap

  def getIndex(i: Int) = fieldsAndValues(i - 1)._2 // Subtract 1 because DB result sets are 1-indexed
  def getField(name: String) = map(name)
}
