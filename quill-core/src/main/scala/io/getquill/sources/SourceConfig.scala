package io.getquill.sources

import io.getquill.util.Config

trait SourceConfig[T] {
  
  def name: String
  
  def config = Config(name)
}
