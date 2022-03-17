package com.github.pjfanning.zio.micrometer

import io.micrometer.core.instrument.Meter
import zio.UIO

trait HasMicrometerMeterId {
  def getMeterId: UIO[Meter.Id]
}
