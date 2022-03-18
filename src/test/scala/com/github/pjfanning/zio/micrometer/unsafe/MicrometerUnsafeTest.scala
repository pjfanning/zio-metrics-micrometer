package com.github.pjfanning.zio.micrometer.unsafe

import com.github.pjfanning.zio.micrometer.{Counter, Gauge, HasMicrometerMeterId, ReadOnlyGauge}
import io.micrometer.core.instrument.Meter
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.Clock
import zio.test.Assertion._
import zio.test.{ZSpec, assert}
import zio.ZIO

import java.util.concurrent.atomic.AtomicReference
import scala.util.Random
import zio.test.ZIOSpecDefault

object MicrometerUnsafeTest extends ZIOSpecDefault {

  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val env = Clock.live ++ Registry.makeWith(registry)

  val counterTestZIO: ZIO[Registry, Throwable, Counter] = for {
    c <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    _ <- c(Seq("get", "users")).inc
    _ <- c(Array("get", "users")).inc(2)
  } yield c(Seq("get", "users"))

  val counterZipTestZIO: ZIO[Registry, Throwable, (Meter.Id, Meter.Id)] = for {
    c1 <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    c2 <- Counter.labelled("simple_counter", None, Array("method", "resource"))
    id1 <- c1(Seq("get", "users")).asInstanceOf[HasMicrometerMeterId].getMeterId
    id2 <- c2(Array("get", "users")).asInstanceOf[HasMicrometerMeterId].getMeterId
  } yield (id1, id2)

  val gaugeTestZIO: ZIO[Registry, Throwable, Gauge] = for {
    g <- Gauge.labelled("simple_gauge", None, Array("method", "resource"))
    _ <- g(Array("get", "users")).inc
    _ <- g(Array("get", "users")).inc(2)
    _ <- g(Array("get", "users")).dec(0.5)
  } yield g(Seq("get", "users"))

  val functionGaugeHolder = new AtomicReference[Double](10.0)
  val functionGaugeTestZIO: ZIO[Registry, Throwable, ReadOnlyGauge] = for {
    g <- Gauge.labelledFunction("simple_gauge", None, Array("method", "resource"),
      () => functionGaugeHolder.get())
  } yield g(Seq("get", "users"))

  val tFunctionGaugeHolder = new AtomicReference[Double](10.0)
  val tFunctionGaugeTestZIO: ZIO[Registry, Throwable, ReadOnlyGauge] = for {
    g <- Gauge.labelledTFunction[AtomicReference[Double]]("simple_gauge", None, Array("method", "resource"),
      tFunctionGaugeHolder, _.get())
  } yield g(Seq("get", "users"))

  override def spec = suite("MicrometerUnsafeTest")(
      suite("Counter")(
        testM("counter increases by `inc` amount") {
          for {
            counter <- counterTestZIO
            counterValue <- counter.get
          } yield assert(counterValue)(equalTo(3.0))
        },
        testM("counter ids match") {
          for {
            (id1, id2) <- counterZipTestZIO
          } yield assert(id1)(equalTo(id2))
        }
      ),
      suite("Gauge")(
        testM("gauge increases and decreases by `inc/dec` amount") {
          for {
            gauge <- gaugeTestZIO
            gaugeValue <- gauge.get
          } yield assert(gaugeValue)(equalTo(2.5))
        },
        testM("gauge based on function works") {
          for {
            gauge <- functionGaugeTestZIO
            gaugeValue <- gauge.get
          } yield assert(gaugeValue)(equalTo(functionGaugeHolder.get()))
          functionGaugeHolder.set(Random.nextDouble())
          for {
            gauge <- functionGaugeTestZIO
            gaugeValue <- gauge.get
          } yield assert(gaugeValue)(equalTo(functionGaugeHolder.get()))
        },
        testM("gauge based on t function works") {
          for {
            gauge <- tFunctionGaugeTestZIO
            gaugeValue <- gauge.get
          } yield assert(gaugeValue)(equalTo(tFunctionGaugeHolder.get()))
          tFunctionGaugeHolder.set(Random.nextDouble())
          for {
            gauge <- tFunctionGaugeTestZIO
            gaugeValue <- gauge.get
          } yield assert(gaugeValue)(equalTo(tFunctionGaugeHolder.get()))
        }
      )
    ).provideCustomLayer(env)testtesttesttesttest
}
