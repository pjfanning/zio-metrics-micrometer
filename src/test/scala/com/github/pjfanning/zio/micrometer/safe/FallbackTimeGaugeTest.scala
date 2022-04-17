package com.github.pjfanning.zio.micrometer.safe

import zio.test.Assertion.equalTo
import zio.test.{ZIOSpecDefault, assert}

import scala.concurrent.duration._

object FallbackTimeGaugeTest extends ZIOSpecDefault {

  override def spec = suite("FallbackTimeGaugeTest")(
    suite("FallbackTimeGauge")(
      test("gauge inits to 0") {
        val gauge = new FallbackTimeGauge(SECONDS)
        for {
          gaugeValue <- gauge.totalTime(HOURS)
        } yield assert(gaugeValue)(equalTo(0.0))
      },
      test("gauge supports bastTimeUnit") {
        val gauge = new FallbackTimeGauge(MICROSECONDS)
        for {
          unitValue <- gauge.baseTimeUnit
        } yield assert(unitValue)(equalTo(MICROSECONDS))
      },
      test("gauge increases by recorded finite duration") {
        val gauge = new FallbackTimeGauge(MILLISECONDS)
        for {
          _ <- gauge.record(10.seconds)
          gaugeValue <- gauge.totalTime(SECONDS)
        } yield assert(gaugeValue)(equalTo(10.0))
      },
      test("gauge increases by recorded finite duration") {
        val gauge = new FallbackTimeGauge(NANOSECONDS)
        for {
          _ <- gauge.record(zio.Duration(123, MILLISECONDS))
          gaugeValue <- gauge.totalTime(SECONDS)
        } yield assert(gaugeValue)(equalTo(0.123))
      }
    )
  )
}
