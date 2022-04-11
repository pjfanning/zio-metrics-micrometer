package com.github.pjfanning.zio.micrometer.safe

import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert}

import java.text.DecimalFormat

object FallbackDistributionSummaryTest extends DefaultRunnableSpec {

  override def spec = suite("FallbackDistributionSummaryTest")(
    suite("FallbackDistributionSummary")(
      testM("summary count inits to 0") {
        val summary = emptySummary()
        for {
          countValue <- summary.count
        } yield assert(countValue)(equalTo(0.0))
      },
      testM("summary max inits to 0") {
        val summary = emptySummary()
        for {
          maxValue <- summary.max
        } yield assert(maxValue)(equalTo(0.0))
      },
      testM("summary mean inits to 0") {
        val summary = emptySummary()
        for {
          meanValue <- summary.mean
        } yield assert(meanValue)(equalTo(0.0))
      },
      testM("summary totalAmount inits to 0") {
        val summary = emptySummary()
        for {
          minValue <- summary.totalAmount
        } yield assert(minValue)(equalTo(0.0))
      },
      testM("summary counts correctly") {
        for {
          summary <- summaryWith2Records()
          countValue <- summary.count
        } yield assert(countValue)(equalTo(2.0))
      },
      testM("summary has right max") {
        for {
          summary <- summaryWith2Records()
          maxValue <- summary.max
        } yield assert(maxValue)(equalTo(2.2))
      },
      testM("summary has right mean") {
        for {
          summary <- summaryWith2Records()
          meanValue <- summary.mean
        } yield assert(formattedDouble(meanValue))(equalTo("1.65"))
      },
      testM("summary has right total") {
        for {
          summary <- summaryWith2Records()
          totalValue <- summary.totalAmount
        } yield assert(formattedDouble(totalValue))(equalTo("3.3"))
      }
    )
  )

  private val numberFormatter = new DecimalFormat("#0.###")

  private def formattedDouble(d: Double): String = numberFormatter.format(d)

  private def emptySummary(): FallbackDistributionSummary = new FallbackDistributionSummary

  private def summaryWith2Records(): ZIO[Any, Nothing, FallbackDistributionSummary] = {
    val summary = new FallbackDistributionSummary
    for {
      _ <- summary.record(1.1)
      _ <- summary.record(2.2)
    } yield summary
  }

}
