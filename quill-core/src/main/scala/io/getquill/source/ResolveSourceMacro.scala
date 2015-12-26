package io.getquill.source

import scala.reflect.ClassTag
import scala.reflect.api.Types
import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import java.io.StringWriter
import java.io.PrintWriter
import scala.concurrent.duration._
import scala.reflect.api.Trees
import io.getquill.util.Cache
import java.io.Closeable

trait ResolveSourceMacro {
  val c: Context
  import c.universe.{ Try => _, _ }

  private val classLoader = getClass.getClassLoader

  def resolveSource[T <: Source[_, _]](implicit t: ClassTag[T]): Option[T] = {
    val tpe = c.prefix.tree.tpe
    ResolveSourceMacro.cache
      .getOrElseUpdate(tpe, resolve[T](tpe), 10.seconds)
      .asInstanceOf[Option[T]]
  }

  private def resolve[T <: Source[_, _]](tpe: Type)(implicit t: ClassTag[T]): Option[Source[_, _]] = {
    val sourceName = tpe.termSymbol.name.decodedName.toString
    resolve(sourceName, baseClasses[T](tpe)) match {
      case (None, errors) =>
        c.warning(NoPosition, s"Can't load the source '$sourceName' at compile time. The sql probing is disabled for the source. Trace: \n${errors.mkString("\n")}")
        None
      case (some, errors) =>
        some
    }
  }

  private def baseClasses[T](tpe: Type)(implicit ct: ClassTag[T]): List[Class[Any]] =
    tpe.baseClasses.map(_.asClass.fullName)
      .map(name => List(loadClass(name), loadClass(name + "$")).flatten).flatten
      .filter(ct.runtimeClass.isAssignableFrom(_))

  private def resolve(name: String, classes: List[Class[Any]]): (Option[Source[_, _]], List[String]) =
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
          case Success(v) => (Some(v.asInstanceOf[Source[_, _]]), Nil)
          case Failure(e) =>
            val (value, errors) = resolve(name, tail)
            val error = s"Failed to load from source class '$cls'. Stack trace:\n${stackTraceToString(e)}"
            (value, error +: errors)
        }
    }

  private def stackTraceToString(e: Throwable) = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString()
  }

  protected def loadClass(name: String) =
    Try(classLoader.loadClass(name).asInstanceOf[Class[Any]]).toOption
}

object ResolveSourceMacro {
  private val cache = new Cache[Types#Type, Source[_, _]]
}
