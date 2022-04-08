package com.github.pjfanning.zio.micrometer

import zio.UIO

trait Counter {
  def inc(): UIO[Unit] = inc(1)
  def inc(amount: Double): UIO[Unit]
  def get: UIO[Double]
}
