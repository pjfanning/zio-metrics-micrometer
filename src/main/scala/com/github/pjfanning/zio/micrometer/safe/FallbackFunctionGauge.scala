package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyGauge
import zio.{UIO, ZIO}

private[safe] class FallbackFunctionGauge(fun: () => Double) extends ReadOnlyGauge {
  override def get: UIO[Double] = ZIO.succeed(fun())
}
