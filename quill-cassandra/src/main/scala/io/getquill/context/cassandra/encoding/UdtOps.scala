package io.getquill.context.cassandra.encoding

import com.datastax.oss.driver.api.core.data.UdtValue

import scala.jdk.CollectionConverters._

class UdtValueOps(val udt: UdtValue) extends AnyVal {
  def getScalaList[A](name: String, cls: Class[A]) =
    udt.getList(name, cls).asScala

  def getScalaSet[A](name: String, cls: Class[A]) =
    udt.getSet(name, cls).asScala

  def getScalaMap[K, V](name: String, kcls: Class[K], vcls: Class[V]) =
    udt.getMap(name, kcls, vcls).asScala

  def setScalaList[A](name: String, v: Seq[A], cls: Class[A]) =
    udt.setList(name, v.asJava, cls)

  def setScalaSet[A](name: String, v: Set[A], cls: Class[A]) =
    udt.setSet(name, v.asJava, cls)

  def setScalaMap[K, V](name: String, v: Map[K, V], kcls: Class[K], vcls: Class[V]) =
    udt.setMap(name, v.asJava, kcls, vcls)
}

object UdtValueOps {
  def apply(udt: UdtValue) = new UdtValueOps(udt)
}
