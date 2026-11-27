package zio.quill.context.mirror

case class MirrorSession(name: String)
object MirrorSession {
  def default = MirrorSession("DefaultMirrorSession")
}
