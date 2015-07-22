package io.getquill.attach

trait Attachable[T] {
  def attachment: T
}