package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import com.github.pjfanning.zio.micrometer.unsafe.AtomicDouble
import zio.{UIO, ZIO}

private[safe] class FallbackCounter extends Counter {
  private val atomicDouble = new AtomicDouble()
  override def inc(amount: Double): UIO[Unit] = ZIO.succeed(atomicDouble.addAndGet(amount))
  override def get: UIO[Double] = ZIO.succeed(atomicDouble.get())
}
