package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyTimeGauge
import zio.{UIO, URIO}

import scala.concurrent.duration.{Duration, TimeUnit}

private[safe] class FallbackTFunctionTimeGauge[T](baseTimeUnit: TimeUnit, t: T, fun: T => Double) extends ReadOnlyTimeGauge {
  override def baseTimeUnit: UIO[TimeUnit] = URIO.succeed(baseTimeUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = URIO.succeed {
    Duration(fun(t), timeUnit).toUnit(baseTimeUnit)
  }
}
