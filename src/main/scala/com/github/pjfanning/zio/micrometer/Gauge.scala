package com.github.pjfanning.zio.micrometer

import zio.UIO

trait ReadOnlyGauge {
  def get: UIO[Double]
}

trait Gauge extends ReadOnlyGauge {
  def set(value: Double): UIO[Unit]
  def inc: UIO[Unit] = inc(1)
  def inc(amount: Double): UIO[Unit]
  def dec: UIO[Unit] = dec(1)
  def dec(amount: Double): UIO[Unit]
}
