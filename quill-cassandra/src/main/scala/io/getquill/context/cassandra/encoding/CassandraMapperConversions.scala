package io.getquill.context.cassandra.encoding

import io.getquill.MappedEncoding

trait CassandraMapperConversions extends CassandraMapperConversionsLowPriorityImplicits {

  implicit def cassandraIdentityMapper[Cas](implicit cas: CassandraType[Cas]): CassandraMapper[Cas, Cas] =
    CassandraMapper(identity)

  implicit def cassandraMapperEncode[T, Cas](
    implicit
    m:   MappedEncoding[T, Cas],
    cas: CassandraType[Cas]
  ): CassandraMapper[T, Cas] = CassandraMapper(m.f)

  implicit def cassandraMapperDecode[T, Cas](
    implicit
    m:   MappedEncoding[Cas, T],
    cas: CassandraType[Cas]
  ): CassandraMapper[Cas, T] = CassandraMapper(m.f)
}

trait CassandraMapperConversionsLowPriorityImplicits {

  implicit def cassandraMapperEncodeRec[I, O, Cas](
    implicit
    me: MappedEncoding[I, O],
    cm: CassandraMapper[O, Cas]
  ): CassandraMapper[I, Cas] = CassandraMapper(me.f.andThen(cm.f))

  implicit def cassandraMapperDecodeRec[I, O, Cas](
    implicit
    m:  MappedEncoding[I, O],
    cm: CassandraMapper[Cas, I]
  ): CassandraMapper[Cas, O] = CassandraMapper(cm.f.andThen(m.f))
}