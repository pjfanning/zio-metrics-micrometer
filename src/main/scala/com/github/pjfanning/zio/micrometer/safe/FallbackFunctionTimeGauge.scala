package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyTimeGauge
import zio.UIO

import scala.concurrent.duration.{Duration, TimeUnit}

private[safe] class FallbackFunctionTimeGauge(baseUnit: TimeUnit, fun: => Double) extends ReadOnlyTimeGauge {
  override def baseTimeUnit: UIO[TimeUnit] = UIO.succeed(baseUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = UIO.succeed {
    Duration(fun, baseUnit).toUnit(timeUnit)
  }
}
