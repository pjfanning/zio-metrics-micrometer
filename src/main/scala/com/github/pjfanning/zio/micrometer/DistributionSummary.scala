package com.github.pjfanning.zio.micrometer

import zio.UIO

trait DistributionSummary {
  def count: UIO[Double]
  def totalAmount: UIO[Double]
  def max: UIO[Double]
  def mean: UIO[Double]
  def record(value: Double): UIO[Unit]
}
