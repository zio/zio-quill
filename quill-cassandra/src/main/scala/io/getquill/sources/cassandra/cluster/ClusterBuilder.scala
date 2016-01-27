package io.getquill.sources.cassandra.cluster

import io.getquill.util.Messages._
import scala.util.Try
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueType
import java.lang.reflect.Method
import scala.collection.JavaConversions._
import com.datastax.driver.core.Cluster

object ClusterBuilder {

  def apply(cfg: Config) =
    set(Cluster.builder, cfg)

  private def set[T](instance: T, cfg: Config): T = {
    for (key <- cfg.entrySet.map(_.getKey.split('.').head)) {

      def tryMethod(m: Method) =
        m.getParameterTypes.toList match {
          case Nil =>
            Try(cfg.getBoolean(key)).map {
              case true  => m.invoke(instance)
              case false =>
            }
          case tpe :: Nil =>
            param(key, tpe, cfg)
              .map(p => m.invoke(instance, p.asInstanceOf[AnyRef]))
          case tpe :: tail =>
            val c = cfg.getConfig(key)
            tail.foldLeft(param("0", tpe, c).map(List(_))) {
              case (list, tpe) =>
                list.flatMap { l =>
                  val key = s"${l.size}"
                  param(key, tpe, c).map(l :+ _)
                }
            }.map { params =>
              m.invoke(instance, params.asInstanceOf[List[Object]]: _*)
            }
        }

      def tryMethods(m: List[Method]): Any =
        m match {
          case Nil       => fail(s"Invalid config key '$key'")
          case m :: tail => tryMethod(m).getOrElse(tryMethods(tail))
        }

      tryMethods {
        instance.getClass.getMethods.toList.filter { m =>
          m.getName == key ||
            m.getName == s"with${key.capitalize}" ||
            m.getName == s"add${key.capitalize}" ||
            m.getName == s"set${key.capitalize}"
        }
      }
    }

    instance
  }

  private def param(key: String, tpe: Class[_], cfg: Config) =
    Try {
      if (tpe == classOf[String])
        cfg.getString(key)
      else if (tpe == classOf[Int] || tpe == classOf[Integer])
        cfg.getInt(key)
      else if (tpe.isEnum)
        tpe.getMethod("valueOf", classOf[String]).invoke(tpe, cfg.getString(key))
      else if (cfg.getValue(key).valueType == ConfigValueType.STRING)
        getClass.getClassLoader.loadClass(cfg.getString(key)).newInstance
      else
        set(tpe.newInstance, cfg.getConfig(key))
    }
}
