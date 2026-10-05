package zio.quill

case class MappedEncoding[I, O](f: I => O)
