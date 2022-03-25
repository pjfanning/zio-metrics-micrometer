package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.DistributionSummary
import zio.{Semaphore, UIO}

private[safe] class FallbackDistributionSummary extends DistributionSummary {
  private val semaphore = Semaphore.make(permits = 1)
  private var _count: Int = 0
  private var _max: Double = Double.NaN
  private var _total: Double = 0.0
  override def count: UIO[Double] = UIO.succeed(_count)
  override def totalAmount: UIO[Double] = UIO.succeed(_total)
  override def max: UIO[Double] = UIO.succeed(_max)
  override def mean: UIO[Double] = UIO.succeed(_total / _count)
  override def record(value: Double): UIO[Unit] = semaphore.map { _ =>
    _count += 1
    _total += value
    _max = Math.max(_max, value)
  }
}
