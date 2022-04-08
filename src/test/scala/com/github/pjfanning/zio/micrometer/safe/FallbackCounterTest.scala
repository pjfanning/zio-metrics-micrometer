package com.github.pjfanning.zio.micrometer.safe

import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert}

object FallbackCounterTest extends DefaultRunnableSpec {

  override def spec = suite("FallbackCounterTest")(
    suite("FallbackCounter")(
      testM("counter inits to 0") {
        val counter = new FallbackCounter
        for {
          counterValue <- counter.get
        } yield assert(counterValue)(equalTo(0.0))
      },
      testM("counter increases by `inc`") {
        val counter = new FallbackCounter
        for {
          _ <- counter.inc()
          counterValue <- counter.get
        } yield assert(counterValue)(equalTo(1.0))
      },
      testM("counter increases by `inc` amount by 1") {
        val counter = new FallbackCounter
        for {
          _ <- counter.inc(1.0)
          counterValue <- counter.get
        } yield assert(counterValue)(equalTo(1.0))
      },
      testM("counter increases by `inc` amount by 2.5") {
        val counter = new FallbackCounter
        for {
          _ <- counter.inc(2.5)
          counterValue <- counter.get
        } yield assert(counterValue)(equalTo(2.5))
      }
    )
  )
}