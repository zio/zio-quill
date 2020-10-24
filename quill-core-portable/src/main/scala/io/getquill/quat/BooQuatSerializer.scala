package io.getquill.quat

import java.nio.ByteBuffer
import java.util.Base64

import boopickle.Default._

import scala.collection.mutable.LinkedHashMap

object BooQuatSerializer {
  case class ProductWithRenames(data: LinkedHashMap[String, Quat], renames: List[(String, String)])

  implicit object productPickler extends Pickler[Quat.Product] {
    override def pickle(value: Quat.Product)(implicit state: PickleState): Unit = {
      state.pickle(value.tpe)
      state.pickle(value.fields)
      state.pickle(value.renames)
      ()
    }
    override def unpickle(implicit state: UnpickleState): Quat.Product = {
      Quat.Product.WithRenames(
        state.unpickle[Quat.Product.Type],
        state.unpickle[LinkedHashMap[String, Quat]],
        state.unpickle[LinkedHashMap[String, String]]
      )
    }
  }

  implicit val quatProductTypePickler: Pickler[Quat.Product.Type] =
    compositePickler[Quat.Product.Type]
      .addConcreteType[Quat.Product.Type.Concrete.type]
      .addConcreteType[Quat.Product.Type.Abstract.type]

  implicit val quatProductPickler: Pickler[Quat] =
    compositePickler[Quat]
      .addConcreteType[Quat.Product](productPickler, scala.reflect.classTag[Quat.Product])
      .addConcreteType[Quat.Generic.type]
      .addConcreteType[Quat.Unknown.type]
      .addConcreteType[Quat.Value.type]
      .addConcreteType[Quat.BooleanValue.type]
      .addConcreteType[Quat.BooleanExpression.type]
      .addConcreteType[Quat.Null.type]

  def serialize(quat: Quat): String = {
    val bytes = Pickle.intoBytes(quat)
    val arr: Array[Byte] = new Array[Byte](bytes.remaining())
    bytes.get(arr)
    Base64.getEncoder.encodeToString(arr)
  }

  def deserialize(str: String): Quat = {
    val bytes = Base64.getDecoder.decode(str)
    Unpickle[Quat].fromBytes(ByteBuffer.wrap(bytes))
  }
}
