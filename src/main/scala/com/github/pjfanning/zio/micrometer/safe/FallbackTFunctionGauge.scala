package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.ReadOnlyGauge
import zio.{UIO, URIO}

private[safe] class FallbackTFunctionGauge[T](t: T, fun: T => Double) extends ReadOnlyGauge {
  override def get: UIO[Double] = URIO.succeed(fun(t))
}
