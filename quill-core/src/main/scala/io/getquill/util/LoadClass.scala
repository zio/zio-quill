package io.getquill.util

import scala.util.Try

object LoadClass {

  private val classLoader = getClass.getClassLoader

  def apply(name: String) =
    Try(classLoader.loadClass(name).asInstanceOf[Class[Any]])
}
