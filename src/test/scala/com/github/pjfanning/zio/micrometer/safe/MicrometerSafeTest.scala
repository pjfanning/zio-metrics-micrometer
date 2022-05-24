package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{Counter, Gauge, HasMicrometerMeterId, ReadOnlyGauge, TimeGauge, Timer}
import io.micrometer.core.instrument.Meter
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import zio.ZIO
import zio.clock.Clock
import zio.logging.{LogFormat, LogLevel, Logging}
import zio.test.Assertion.{equalTo, isGreaterThanEqualTo}
import zio.test.{DefaultRunnableSpec, ZSpec, assert}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.{DurationLong, NANOSECONDS}
import scala.util.Random

object MicrometerSafeTest extends DefaultRunnableSpec {

  private val LoggingEnv =
    Logging.console(
      logLevel = LogLevel.Info,
      format = LogFormat.ColoredLogFormat()
    ) >>> Logging.withRootLoggerName("MicrometerSafeTest")
  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val env = Clock.live ++ Registry.makeWith(registry) ++ LoggingEnv

  val counterTestZIO: ZIO[Registry with Logging, Throwable, Counter] = for {
    c <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    _ <- c(Seq("get", "users")).inc()
    _ <- c(Array("get", "users")).inc(2)
  } yield c(Seq("get", "users"))

  val counterZipTestZIO: ZIO[Registry with Logging, Throwable, (Meter.Id, Meter.Id)] = for {
    c1 <- Counter.labelled("simple_counter", None, Seq("method", "resource"))
    c2 <- Counter.labelled("simple_counter", None, Array("method", "resource"))
    id1 <- c1(Seq("get", "users")).asInstanceOf[HasMicrometerMeterId].getMeterId
    id2 <- c2(Array("get", "users")).asInstanceOf[HasMicrometerMeterId].getMeterId
  } yield (id1, id2)

  val gaugeTestZIO: ZIO[Registry with Logging, Throwable, Gauge] = for {
    g <- Gauge.labelled("simple_gauge", None, Array("method", "resource"))
    _ <- g(Array("get", "users")).inc()
    _ <- g(Array("get", "users")).inc(2)
    _ <- g(Array("get", "users")).dec(0.5)
  } yield g(Seq("get", "users"))

  val functionGaugeHolder = new AtomicReference[Double](10.0)
  val functionGaugeTestZIO: ZIO[Registry with Logging, Throwable, ReadOnlyGauge] = for {
    g <- Gauge.labelledFunction("simple_gauge", None, Array("method", "resource"),
      () => functionGaugeHolder.get())
  } yield g(Seq("get", "users"))

  val tFunctionGaugeHolder = new AtomicReference[Double](10.0)
  val tFunctionGaugeTestZIO: ZIO[Registry with Logging, Throwable, ReadOnlyGauge] = for {
    g <- Gauge.labelledTFunction[AtomicReference[Double]]("simple_gauge", None, Array("method", "resource"),
      tFunctionGaugeHolder, _.get())
  } yield g(Seq("get", "users"))

  val timeGaugeTestZIO: ZIO[Registry with Logging, Throwable, TimeGauge] = for {
    g <- TimeGauge.labelled("plain_time_gauge", None, Array("method", "resource"))
    _ <- g(Array("get", "users")).record(10.seconds)
  } yield g(Seq("get", "users"))

  val timeGaugeTimerTestZIO: ZIO[Registry with Logging with Clock, Throwable, TimeGauge] = for {
    g <- TimeGauge.labelled("timer_time_gauge", None, Array("method", "resource"))
    timer <- g(Array("get", "users")).startTimerSample()
    _ <- ZIO.sleep(zio.duration.Duration.fromMillis(250))
    _ <- timer.stop()
  } yield g(Seq("get", "users"))

  val timerTimerTestZIO: ZIO[Registry with Logging with Clock, Throwable, Timer] = for {
    g <- Timer.labelled("timer_timer", None, Array("method", "resource"))
    timer <- g(Array("get", "users")).startTimerSample()
    _ <- ZIO.sleep(zio.duration.Duration.fromMillis(250))
    _ <- timer.stop()
  } yield g(Seq("get", "users"))

  override def spec: ZSpec[Environment, Failure] =
    suite("MicrometerSafeTest")(
      suite("Counter")(
        testM("counter increases by `inc` amount") {
          for {
            counter <- counterTestZIO
            counterValue <- counter.get
          } yield assert(counterValue)(equalTo(3.0))
        },
        testM("counter ids match") {
          for {
            tuple <- counterZipTestZIO
            (id1, id2) = tuple
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
      ),
      suite("TimeGauge")(
        testM("gauge records duration") {
          for {
            gauge <- timeGaugeTestZIO
            gaugeValue <- gauge.totalTime(NANOSECONDS)
          } yield assert(gaugeValue)(equalTo(10.0 * 1000000000))
        },
        testM("gauge applies timer") {
          for {
            gauge <- timeGaugeTimerTestZIO
            gaugeValue <- gauge.totalTime(NANOSECONDS)
          } yield assert(gaugeValue)(isGreaterThanEqualTo(250.0))
        }
      ),
      suite("Timer")(
        testM("timer applies timer") {
          for {
            timer <- timerTimerTestZIO
            timerValue <- timer.totalTime(NANOSECONDS)
          } yield assert(timerValue)(isGreaterThanEqualTo(250.0))
        }
      )
    ).provideCustomLayer(env)
}
