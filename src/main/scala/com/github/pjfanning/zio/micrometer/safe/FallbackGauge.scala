package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Gauge
import com.github.pjfanning.zio.micrometer.unsafe.AtomicDouble
import zio.{UIO, URIO}

private[safe] class FallbackGauge extends Gauge {
  private val atomicDouble = new AtomicDouble()
  override def set(value: Double): UIO[Unit] = URIO.succeed(atomicDouble.set(value))
  override def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
  override def dec(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(-amount))
  override def get: UIO[Double] = URIO.succeed(atomicDouble.get())
}
