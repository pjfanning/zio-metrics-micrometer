package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Gauge
import com.github.pjfanning.zio.micrometer.unsafe.AtomicDouble
import zio.{UIO, ZIO}

private[safe] class FallbackGauge extends Gauge {
  private val atomicDouble = new AtomicDouble()
  override def set(value: Double): UIO[Unit] = ZIO.succeed(atomicDouble.set(value))
  override def inc(amount: Double): UIO[Unit] = ZIO.succeed(atomicDouble.addAndGet(amount))
  override def dec(amount: Double): UIO[Unit] = ZIO.succeed(atomicDouble.addAndGet(-amount))
  override def get: UIO[Double] = ZIO.succeed(atomicDouble.get())
}
