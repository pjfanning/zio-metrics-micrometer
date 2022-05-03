package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.DistributionSummary
import zio.{Semaphore, UIO, ZIO}

private[safe] class FallbackDistributionSummary extends DistributionSummary {
  private val semaphore = Semaphore.make(permits = 1)
  private var _count: Int = 0
  private var _max: Double = 0
  private var _total: Double = 0.0
  override def count: UIO[Double] = ZIO.succeed(_count)
  override def totalAmount: UIO[Double] = ZIO.succeed(_total)
  override def max: UIO[Double] = ZIO.succeed(_max)
  override def mean: UIO[Double] = ZIO.succeed {
    if (_count == 0) 0.0 else _total / _count
  }
  override def record(value: Double): UIO[Unit] = semaphore.map { _ =>
    _count += 1
    _total += value
    if (_count == 1) {
      _max = value
    } else {
      _max = Math.max(_max, value)
    }
  }
}
