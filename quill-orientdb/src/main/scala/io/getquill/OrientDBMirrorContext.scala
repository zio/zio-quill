package io.getquill

import io.getquill.context.orientdb.{ OrientDBContext, OrientDBIdiom }

class OrientDBMirrorContext[Naming <: NamingStrategy](naming: Naming)
  extends MirrorContext[OrientDBIdiom, Naming](OrientDBIdiom, naming) with OrientDBContext[Naming] {

  implicit def listDecoder[T]: Decoder[List[T]] = decoderUnsafe[List[T]]
  implicit def setDecoder[T]: Decoder[Set[T]] = decoderUnsafe[Set[T]]
  implicit def mapDecoder[K, V]: Decoder[Map[K, V]] = decoderUnsafe[Map[K, V]]

  implicit def listEncoder[T]: Encoder[List[T]] = encoder[List[T]]
  implicit def setEncoder[T]: Encoder[Set[T]] = encoder[Set[T]]
  implicit def mapEncoder[K, V]: Encoder[Map[K, V]] = encoder[Map[K, V]]
}