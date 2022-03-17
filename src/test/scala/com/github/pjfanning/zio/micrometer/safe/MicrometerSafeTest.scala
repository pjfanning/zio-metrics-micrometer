package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import com.github.pjfanning.zio.micrometer.safe.Counter
import com.github.pjfanning.zio.micrometer.unsafe.Registry
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.ZIO
import zio.Clock
import zio.logging.{LogFormat, LogLevel, Logging}
import zio.test.Assertion.equalTo
import zio.test.{ZSpec, assert}
import zio.test.ZIOSpecDefault

object MicrometerSafeTest extends ZIOSpecDefault {

  private val LoggingEnv =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("MicrometerSafeTest")
  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val env = Clock.live ++ Registry.makeWith(registry) ++ LoggingEnv

  val counterTestZIO: ZIO[Registry with Logging, Throwable, Counter] = for {
    c <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    _ <- c(Seq("get", "users")).inc
    _ <- c(Array("get", "users")).inc(2)
  } yield c(Seq("get", "users"))

  override def spec = suite("MicrometerSafeTest")(
      suite("Counter")(
        testM("counter increases by `inc` amount") {
          for {
            counter <- counterTestZIO
            counterValue <- counter.get
          } yield assert(counterValue)(equalTo(3.0))
        }
      )
    ).provideCustomLayer(env)test
}
