package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{Counter, TimeGauge, Timer}
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.{Duration, ZIO}
import zio.test.TestAspect._
import zio.test.Assertion.{equalTo, isGreaterThanEqualTo}
import zio.test.{ZIOSpecDefault, assert}

import scala.concurrent.duration.{DurationLong, MILLISECONDS, NANOSECONDS}

object MicrometerSafeTest extends ZIOSpecDefault {

  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val env = Registry.makeWith(registry)

  val counterTestZIO: ZIO[Registry, Throwable, Counter] = for {
    c <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    _ <- c(Seq("get", "users")).inc()
    _ <- c(Array("get", "users")).inc(2)
  } yield c(Seq("get", "users"))

  val timeGaugeTestZIO: ZIO[Registry, Throwable, TimeGauge] = for {
    g <- TimeGauge.labelled("plain_time_gauge", None, Array("method", "resource"))
    _ <- g(Array("get", "users")).record(10.seconds)
  } yield g(Seq("get", "users"))

  val timeGaugeTimerTestZIO: ZIO[Registry, Throwable, TimeGauge] = for {
    g <- TimeGauge.labelled("timer_time_gauge", None, Array("method", "resource"))
    timer <- g(Array("get", "users")).startTimerSample()
    _ <- ZIO.sleep(Duration.fromMillis(250))
    _ <- timer.stop()
  } yield g(Seq("get", "users"))

  val timerTimerTestZIO: ZIO[Registry, Throwable, Timer] = for {
    g <- Timer.labelled("timer_timer", None, Array("method", "resource"))
    timer <- g(Array("get", "users")).startTimerSample()
    _ <- ZIO.sleep(Duration.fromMillis(250))
    _ <- timer.stop()
  } yield g(Seq("get", "users"))

  override def spec = suite("MicrometerSafeTest")(
    suite("Counter")(
      test("counter increases by `inc` amount") {
        for {
          counter <- counterTestZIO
          counterValue <- counter.get
        } yield assert(counterValue)(equalTo(3.0))
      }
    ),
    suite("TimeGauge")(
      test("gauge records duration") {
        for {
          gauge <- timeGaugeTestZIO
          gaugeValue <- gauge.totalTime(NANOSECONDS)
        } yield assert(gaugeValue)(equalTo(10.0 * 1000000000))
      },
      test("gauge applies timer") {
        for {
          gauge <- timeGaugeTimerTestZIO
          gaugeValue <- gauge.totalTime(MILLISECONDS)
        } yield assert(gaugeValue)(isGreaterThanEqualTo(250.0))
      } @@ withLiveClock
    ),
    suite("Timer")(
      test("gauge applies timer") {
        for {
          timer <- timerTimerTestZIO
          timerValue <- timer.totalTime(MILLISECONDS)
        } yield assert(timerValue)(isGreaterThanEqualTo(250.0))
      } @@ withLiveClock
    )
  ).provideCustomLayer(env)
}
