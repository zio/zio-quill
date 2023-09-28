package io.getquill.context.mirror

final case class MirrorSession(name: String)
object MirrorSession {
  def default: MirrorSession = MirrorSession("DefaultMirrorSession")
}
