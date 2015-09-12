package io.getquill.source

import scala.reflect.macros.whitebox.Context
import scala.reflect.api.Position
import scala.util.Try
import scala.reflect.api.Types
import scala.util.Success
import scala.util.Failure
import scala.reflect.ClassTag
import scala.util.Success
import scala.util.Failure

trait ResolveSourceMacro {
  val c: Context
  import c.universe.{ Try => _, _ }

  private val classLoader = getClass.getClassLoader

  def resolveSource[T](implicit t: ClassTag[T]) =
    resolveCached[T] match {
      case (Some(source), errors) =>
        Success(source)
      case (None, errors) =>
        Failure(new IllegalStateException(s"Can't resolve the source instance at compile time. Trace: \n${errors.mkString("/n")}"))
    }

  private def resolveCached[T](implicit t: ClassTag[T]) = {
    val tpe = c.prefix.tree.tpe
    ResolveSourceMacro.cache.getOrElseUpdate(tpe, resolve[T](tpe)) match {
      case (None, errors)       => (None, errors)
      case (Some(v: T), errors) => (Some(v), errors)
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
            Source.configPrefix.withValue(Some(name)) {
              cls.newInstance
            }
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

object ResolveSourceMacro {
  private val cache = collection.mutable.Map[Types#Type, (Option[Any], List[String])]()
}
