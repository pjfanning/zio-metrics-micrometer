package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.{Clock, ZIO}
import zio.test.Assertion.equalTo
import zio.test.{ZIOSpecDefault, assert}

object MicrometerSafeTest extends ZIOSpecDefault {

  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val env = Registry.makeWith(registry)

  val counterTestZIO: ZIO[Registry, Throwable, Counter] = for {
    c <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    _ <- c(Seq("get", "users")).inc
    _ <- c(Array("get", "users")).inc(2)
  } yield c(Seq("get", "users"))

  override def spec = suite("MicrometerSafeTest")(
      suite("Counter")(
        test("counter increases by `inc` amount") {
          for {
            counter <- counterTestZIO
            counterValue <- counter.get
          } yield assert(counterValue)(equalTo(3.0))
        }
      )
    ).provideCustomLayer(env)
}
