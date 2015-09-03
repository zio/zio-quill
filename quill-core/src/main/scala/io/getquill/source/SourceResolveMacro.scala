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

  def resolveSource[T](implicit t: ClassTag[T]) =
    resolveCached[T] match {
      case (Some(source), errors) => source
      case (None, errors) =>
        fail(s"Can't resolve the source instance at compile time. Trace: \n${errors.mkString("/n")}")
    }

  private def resolveCached[T](implicit t: ClassTag[T]) = {
    val tpe = c.prefix.tree.tpe
    SourceResolveMacro.cache.getOrElseUpdate(tpe, resolve[T](tpe)) match {
      case (None, errors)       => (None, errors)
      case (Some(v: T), errors) => (Some(v), errors)
      case (Some(v), errors) =>
        c.fail(s"The resolved source instance '$v' doesn't have the expected type '${t.runtimeClass}'")
    }
  }

  private def resolve[T](tpe: Type)(implicit t: ClassTag[T]): (Option[Any], List[String]) = {
    val sourceName = tpe.termSymbol.name.decodedName.toString
    resolve(sourceName, baseClasses[T](tpe))
  }

  private def baseClasses[T](tpe: Type)(implicit t: ClassTag[T]): List[Class[Any]] =
    tpe.baseClasses.map(_.asClass.fullName)
      .map(name => List(loadClass(name), loadClass(name + "$")).flatten)
      .flatten.filter(t.runtimeClass.isAssignableFrom(_))

  private def resolve(name: String, classes: List[Class[Any]]): (Option[Any], List[String]) =
    classes match {
      case Nil =>
        (None, List("All source alternatives failed."))
      case cls :: tail =>
        Try {
          Try(cls.getField("MODULE$")).toOption.map(_.get(cls)).getOrElse {
            val instance = cls.newInstance
            val field = classOf[Source[_, _]].getDeclaredField("configPrefix")
            field.setAccessible(true)
            field.set(instance, name)
            instance
          }
        } match {
          case Success(v) => (Some(v), List())
          case Failure(e) =>
            val (value, errors) = resolve(name, tail)
            val error = s"Failed to load from source class '$cls'. Reason: '$e'"
            (value, error +: errors)
        }
    }

  protected def loadClass(name: String) =
    Try(classLoader.loadClass(name).asInstanceOf[Class[Any]]).toOption
}

object SourceResolveMacro {
  private val cache = collection.mutable.Map[Types#Type, (Option[Any], List[String])]()
}
