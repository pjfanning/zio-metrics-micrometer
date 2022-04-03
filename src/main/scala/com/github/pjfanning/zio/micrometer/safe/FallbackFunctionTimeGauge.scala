package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyTimeGauge
import zio.{UIO, URIO}

import scala.concurrent.duration.{Duration, TimeUnit}

private[safe] class FallbackFunctionTimeGauge(baseTimeUnit: TimeUnit, fun: => Double) extends ReadOnlyTimeGauge {
  override def baseTimeUnit: UIO[TimeUnit] = URIO.succeed(baseTimeUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = URIO.succeed {
    Duration(fun, timeUnit).toUnit(baseTimeUnit)
  }
}
