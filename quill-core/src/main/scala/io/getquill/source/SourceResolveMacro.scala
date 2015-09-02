package io.getquill.source

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context
import scala.reflect.api.Position
import scala.util.Try
import scala.reflect.api.Types
import scala.util.Success
import scala.util.Failure
import scala.reflect.ClassTag

trait SourceResolveMacro {
  val c: Context
  import c.universe.{ Try => _, _ }

  private val classLoader = getClass.getClassLoader

  def self[T](implicit t: ClassTag[T]) = {
    val tpe = c.prefix.tree.tpe
    SourceResolveMacro.cache
      .getOrElseUpdate(tpe, resolve[T](tpe).toOption)
      .asInstanceOf[Option[T]]
  }

  private def resolve[T](tpe: Type)(implicit t: ClassTag[T]): Try[Any] = {
    val sourceName = tpe.termSymbol.name.decodedName.toString
    println(getClass.getResource("."))
    resolve(sourceName, baseClasses[T](tpe))
  }

  private def baseClasses[T](tpe: Type)(implicit t: ClassTag[T]): List[Class[Any]] =
    tpe.baseClasses.map(_.asClass.fullName)
      .map(name => List(loadClass(name), loadClass(name + "$")).flatten)
      .flatten.filter(t.runtimeClass.isAssignableFrom(_))

  private def resolve(name: String, classes: List[Class[Any]]): Try[Any] =
    classes match {
      case Nil =>
        c.fail("Can't instantiate the source at compile time.")
      case cls :: tail =>
        Try {
          Try(cls.getField("MODULE$")).toOption.map(_.get(cls)).getOrElse {
            val instance = cls.newInstance
            val field = classOf[Source[_, _]].getDeclaredField("configPrefix")
            field.setAccessible(true)
            field.set(instance, name)
            instance
          }
        }.orElse {
          resolve(name, tail)
        }
    }

  private def loadClass(name: String) =
    Try(classLoader.loadClass(name).asInstanceOf[Class[Any]]).toOption
}

object SourceResolveMacro {
  private val cache = collection.mutable.Map[Types#Type, Option[Any]]()
}
