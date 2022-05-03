package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{TimeGauge, TimerSample}
import com.github.pjfanning.zio.micrometer.unsafe.AtomicDouble
import io.micrometer.core.instrument.util.TimeUtils
import zio.{Clock, Duration, UIO, ZIO}

import scala.compat.java8.DurationConverters.toScala
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

private[safe] class FallbackTimeGauge(baseUnit: TimeUnit) extends TimeGauge {
  private val atomicDouble = new AtomicDouble()
  override def baseTimeUnit: UIO[TimeUnit] = ZIO.succeed(baseUnit)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed {
    TimeUtils.convert(atomicDouble.get(), baseUnit, timeUnit)
  }
  override def startTimerSample(): UIO[TimerSample] = ZIO.succeed {
    new TimerSample {
      val startTime = zio.Runtime.default.unsafeRun(Clock.currentTime(baseUnit))
      override def stop(): UIO[Unit] = {
        for {
          endTime <- Clock.currentTime(baseUnit)
        } yield {
          atomicDouble.addAndGet(endTime - startTime)
          ()
        }
      }
    }
  }
  override def record(duration: Duration): UIO[Unit] = ZIO.succeed {
    val convertedDuration = toScala(duration).toUnit(baseUnit)
    atomicDouble.addAndGet(convertedDuration)
  }
  override def record(duration: FiniteDuration): UIO[Unit] = ZIO.succeed {
    atomicDouble.addAndGet(duration.toUnit(baseUnit))
  }
}
