package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyTimeGauge
import zio.{UIO, URIO}

import scala.concurrent.duration.{Duration, TimeUnit}

private[safe] class FallbackTFunctionTimeGauge[T](baseUnit: TimeUnit, t: T, fun: T => Double) extends ReadOnlyTimeGauge {
  override def baseTimeUnit: UIO[TimeUnit] = URIO.succeed(baseUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = URIO.succeed {
    Duration(fun(t), baseUnit).toUnit(timeUnit)
  }
}
