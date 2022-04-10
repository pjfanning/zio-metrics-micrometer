package com.github.pjfanning.zio.micrometer.safe

import zio.test.Assertion.equalTo
import zio.test.{ZIOSpecDefault, assert}

object FallbackDistributionSummaryTest extends ZIOSpecDefault {

  override def spec = suite("FallbackDistributionSummaryTest")(
    suite("FallbackDistributionSummary")(
      test("summary count inits to 0") {
        val summary = new FallbackDistributionSummary
        for {
          countValue <- summary.count
        } yield assert(countValue)(equalTo(0.0))
      },
      /*
      test("summary max inits to NaN") {
        val summary = new FallbackDistributionSummary
        for {
          maxValue <- summary.max
        } yield assert(maxValue)(equalTo(Double.NaN))
      },
      test("summary mean inits to NaN") {
        val summary = new FallbackDistributionSummary
        for {
          meanValue <- summary.mean
        } yield assert(meanValue)(equalTo(Double.NaN))
      },
      */
      test("summary totalAmount inits to 0") {
        val summary = new FallbackDistributionSummary
        for {
          minValue <- summary.totalAmount
        } yield assert(minValue)(equalTo(0.0))
      }
    )
  )
}
