package io.getquill.quat

import java.util.Base64
import com.esotericsoftware.kryo.io.{ Input, Output }
import com.esotericsoftware.kryo.{ Kryo, Serializer }
import com.twitter.chill.{ IKryoRegistrar, KryoPool, ScalaKryoInstantiator }
import io.getquill.util.Messages

import scala.collection.mutable

object KryoQuatSerializer {
  import scala.collection.mutable.LinkedHashMap

  // Needs to be lazy or will be initialize by ScalaJS and it doesn't exist there.
  lazy val registrar = new IKryoRegistrar {
    override def apply(k: Kryo): Unit = {
      k.register(classOf[Quat])
      k.register(classOf[Quat.Product])
      k.register(classOf[Quat.Product.Id])
      k.register(Quat.Value.getClass)
      k.register(Quat.BooleanValue.getClass)
      k.register(Quat.BooleanExpression.getClass)
      k.register(Quat.Null.getClass)
      k.register(Quat.Generic.getClass)
      k.register(Quat.Unknown.getClass)
      k.register(classOf[Quat.Product.Type])
      k.register(Quat.Product.Type.Concrete.getClass)
      k.register(Quat.Product.Type.Abstract.getClass)

      // Need to make sure LinkedHashMap is converted to a list of key,value tuples before being convered
      // otherwise very strange things happen after kryo deserialization with the operation of LinkedHashMap.
      // for example, Quat.zip on LinkedHashMap would cause:
      // java.lang.ClassCastException: scala.collection.mutable.DefaultEntry cannot be cast to scala.collection.mutable.LinkedEntry
      // or even SIGSEGV on String.equals in the same place:
      // Stack: [0x00007f4c681f2000,0x00007f4c682f3000],  sp=0x00007f4c682e36e0,  free space=965k
      // Native frames: (J=compiled Java code, j=interpreted, Vv=VM code, C=native code)
      // J 15 C2 java.lang.String.equals(Ljava/lang/Object;)Z (81 bytes) @ 0x00007f4c851180da [0x00007f4c851180a0+0x3a]
      // J 5794 C2 scala.collection.immutable.Map$Map1.get(Ljava/lang/Object;)Lscala/Option; (27 bytes) @ 0x00007f4c8603c1c4 [0x00007f4c8603c040+0x184]
      // J 5794 C2 scala.collection.immutable.Map$Map1.get(Ljava/lang/Object;)Lscala/Option; (27 bytes) @ 0x00007f4c8603c1c4 [0x00007f4c8603c040+0x184]
      // J 19573 C2 io.getquill.quat.LinkedHashMapOps$.zipWith(Lscala/collection/mutable/LinkedHashMap;Lscala/collection/mutable/LinkedHashMap;Lscala/PartialFunction;)Lscala/collection/immutable/List; (49 bytes) @ 0x00007f4c860491f8 [0x00007f4c860485e0+0xc18]
      k.register(
        classOf[LinkedHashMap[String, Quat]],
        new Serializer[LinkedHashMap[String, Quat]]() {
          override def write(kryo: Kryo, output: Output, `object`: mutable.LinkedHashMap[String, Quat]): Unit = {
            kryo.writeObject(output, `object`.toList)
          }
          override def read(kryo: Kryo, input: Input, `type`: Class[mutable.LinkedHashMap[String, Quat]]): mutable.LinkedHashMap[String, Quat] = {
            val list = kryo.readObject(input, classOf[List[(String, Quat)]])
            LinkedHashMap(list: _*)
          }
        }
      )
      ()
    }
  }
  // Needs to be lazy or will be initialize by ScalaJS and it doesn't exist there.
  lazy val kryo =
    KryoPool.withByteArrayOutputStream(
      Messages.quatKryoPoolSize,
      new ScalaKryoInstantiator().withRegistrar(registrar)
    )

  def serialize(quat: Quat): String = {
    Base64.getEncoder.encodeToString(kryo.toBytesWithClass(quat))
  }

  def deserialize(str: String): Quat = {
    val bytes = Base64.getDecoder.decode(str)
    kryo.fromBytes(bytes).asInstanceOf[Quat]
  }
}
