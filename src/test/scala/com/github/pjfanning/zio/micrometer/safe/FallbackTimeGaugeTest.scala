package com.github.pjfanning.zio.micrometer.safe

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert}

import scala.concurrent.duration._

object FallbackTimeGaugeTest extends DefaultRunnableSpec {

  override def spec = suite("FallbackTimeGaugeTest")(
    suite("FallbackTimeGauge")(
      testM("gauge inits to 0") {
        val gauge = new FallbackTimeGauge(SECONDS)
        for {
          gaugeValue <- gauge.totalTime(HOURS)
        } yield assert(gaugeValue)(equalTo(0.0))
      },
      testM("gauge supports bastTimeUnit") {
        val gauge = new FallbackTimeGauge(MICROSECONDS)
        for {
          unitValue <- gauge.baseTimeUnit
        } yield assert(unitValue)(equalTo(MICROSECONDS))
      },
      testM("gauge increases by recorded finite duration") {
        val gauge = new FallbackTimeGauge(MILLISECONDS)
        for {
          _ <- gauge.record(10.seconds)
          gaugeValue <- gauge.totalTime(SECONDS)
        } yield assert(gaugeValue)(equalTo(10.0))
      },
      testM("gauge increases by recorded finite duration") {
        val gauge = new FallbackTimeGauge(NANOSECONDS)
        for {
          _ <- gauge.record(zio.duration.Duration(123, MILLISECONDS))
          gaugeValue <- gauge.totalTime(SECONDS)
        } yield assert(gaugeValue)(equalTo(0.123))
      }
    )
  )
}
