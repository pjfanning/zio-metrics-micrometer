package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyTimeGauge
import zio.{UIO, ZIO}

import scala.concurrent.duration.{Duration, TimeUnit}

private[safe] class FallbackFunctionTimeGauge(baseUnit: TimeUnit, fun: => Double) extends ReadOnlyTimeGauge {
  override def baseTimeUnit: UIO[TimeUnit] = ZIO.succeed(baseUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed {
    Duration(fun, baseUnit).toUnit(timeUnit)
  }
}
