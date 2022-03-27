package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{TimeGauge, TimerSample}
import com.github.pjfanning.zio.micrometer.unsafe.AtomicDouble
import io.micrometer.core.instrument.util.TimeUtils
import zio.clock.Clock
import zio.duration.Duration
import zio.{UIO, ZIO}

import scala.compat.java8.DurationConverters.toScala
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

private[safe] class FallbackTimeGauge(baseTimeUnit: TimeUnit) extends TimeGauge {
  private val atomicDouble = new AtomicDouble()
  override def baseTimeUnit: UIO[TimeUnit] = UIO.succeed(baseTimeUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = UIO.succeed {
    TimeUtils.convert(atomicDouble.get(), baseTimeUnit, timeUnit)
  }
  override def startTimerSample(): UIO[TimerSample] = UIO.succeed {
    new TimerSample {
      val startTime = zio.Runtime.default.unsafeRun(Clock.Service.live.currentTime(baseTimeUnit))
      override def stop(): UIO[Unit] = {
        val task = for {
          clock <- ZIO.service[Clock.Service]
          endTime <-clock.currentTime(baseTimeUnit)
        } yield {
          atomicDouble.addAndGet(endTime - startTime)
          ()
        }
        task.provideLayer(Clock.live)
      }
    }
  }
  override def record(duration: Duration): UIO[Unit] = UIO.succeed {
    val convertedDuration = toScala(duration).toUnit(baseTimeUnit)
    atomicDouble.addAndGet(convertedDuration)
  }
  override def record(duration: FiniteDuration): UIO[Unit] = UIO.succeed {
    atomicDouble.addAndGet(duration.toUnit(baseTimeUnit))
  }
}
