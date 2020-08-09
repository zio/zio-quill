package io.getquill.quat

import java.nio.ByteBuffer
import java.util.Base64

import boopickle.Default._

import scala.collection.mutable.LinkedHashMap

object BooQuatSerializer {
  case class ProductWithRenames(data: LinkedHashMap[String, Quat], renames: List[(String, String)])

  //  implicit val productPickler: Pickler[Quat.Product] =
  //    transformPickler(
  //      (pr: ProductWithRenames) => if (pr.renames.isEmpty) Quat.Product(pr.data) else Quat.Product.WithRenames(pr.data, pr.renames)
  //    )(
  //        (p: Quat.Product) => ProductWithRenames(p.fields, p.renames)
  //      )

  implicit object productPickler extends Pickler[Quat.Product] {
    override def pickle(value: Quat.Product)(implicit state: PickleState): Unit = {
      state.pickle(value.fields)
      state.pickle(value.renames)
      ()
    }
    override def unpickle(implicit state: UnpickleState): Quat.Product = {
      Quat.Product.WithRenames(
        state.unpickle[LinkedHashMap[String, Quat]],
        state.unpickle[List[(String, String)]]
      )
    }
  }

  implicit val quatProductPickler: Pickler[Quat] =
    compositePickler[Quat]
      .addConcreteType[Quat.Product](productPickler, scala.reflect.classTag[Quat.Product])
      .addConcreteType[Quat.Generic.type]
      .addConcreteType[Quat.Value.type]
      .addConcreteType[Quat.Null.type]

  def serialize(quat: Quat): String = {
    Base64.getEncoder.encodeToString(Pickle.intoBytes(quat).array())
  }

  def deserialize(str: String): Quat = {
    val bytes = Base64.getDecoder.decode(str)
    Unpickle[Quat].fromBytes(ByteBuffer.wrap(bytes))
  }
}
