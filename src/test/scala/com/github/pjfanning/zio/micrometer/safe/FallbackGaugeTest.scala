package com.github.pjfanning.zio.micrometer.safe

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert}

object FallbackGaugeTest extends DefaultRunnableSpec {

  override def spec = suite("FallbackGaugeTest")(
    suite("FallbackGauge")(
      testM("gauge inits to 0") {
        val gauge = new FallbackGauge
        for {
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(0.0))
      },
      testM("gauge increases by `inc`") {
        val gauge = new FallbackGauge
        for {
          _ <- gauge.inc()
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(1.0))
      },
      testM("gauge increases by `inc` amount by 1") {
        val gauge = new FallbackGauge
        for {
          _ <- gauge.inc(1.0)
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(1.0))
      },
      testM("gauge increases by `dec` amount by 1") {
        val gauge = new FallbackGauge
        for {
          _ <- gauge.dec(1.0)
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(-1.0))
      },
      testM("gauge increases by `inc` amount by 2.5") {
        val gauge = new FallbackGauge
        for {
          _ <- gauge.inc(2.5)
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(2.5))
      },
      testM("gauge increases by `dec` amount by 2.5") {
        val gauge = new FallbackGauge
        for {
          _ <- gauge.dec(2.5)
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(-2.5))
      },
      testM("gauge set amount to -2.5") {
        val gauge = new FallbackGauge
        for {
          _ <- gauge.set(-2.5)
          gaugeValue <- gauge.get
        } yield assert(gaugeValue)(equalTo(-2.5))
      }
    )
  )
}
