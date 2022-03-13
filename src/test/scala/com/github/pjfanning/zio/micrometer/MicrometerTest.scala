package com.github.pjfanning.zio.micrometer

import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.Clock
import zio.test.Assertion._
import zio.test.assert
import zio.ZIO
import zio.test.ZIOSpecDefault

object PrometheusTest extends ZIOSpecDefault {

  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val env = Clock.live ++ Registry.makeWith(registry)

  val counterTestZIO: ZIO[Registry, Throwable, Counter] = for {
    c <- Counter.unsafeLabelled("simple_counter", None, Array("method", "resource"))
    _ <- c(Array("get", "users")).inc
    _ <- c(Array("get", "users")).inc(2)
  } yield c(Array("get", "users"))

  val gaugeTestZIO: ZIO[Registry, Throwable, Gauge] = for {
    g <- Gauge.unsafeLabelled("simple_gauge", None, Array("method", "resource"))
    _ <- g(Array("get", "users")).inc
    _ <- g(Array("get", "users")).inc(2)
    _ <- g(Array("get", "users")).dec(1)
  } yield g(Array("get", "users"))

  override def spec = suite("MicrometerLabelsTest")(
      suite("Counter")(
        test("counter increases by `inc` amount") {
          for {
            counter <- counterTestZIO
            counterValue <- counter.get
          } yield assert(counterValue)(equalTo(3.0))
        }
      ),
      suite("Gauge")(
        test("gauge increases and decreases by `inc/dec` amount") {
          for {
            gauge <- gaugeTestZIO
            gaugeValue <- gauge.get
          } yield assert(gaugeValue)(equalTo(2.0))
        }
      )
    ).provideCustomLayer(env)
}
