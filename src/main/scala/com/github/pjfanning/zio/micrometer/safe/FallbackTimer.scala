package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{LongTaskTimer, Timer, TimerSample}
import zio.{Clock, Semaphore, UIO}

import scala.compat.java8.DurationConverters.toScala
import scala.concurrent.duration.{Duration, FiniteDuration, TimeUnit}

private[safe] class FallbackTimer(baseUnit: TimeUnit) extends Timer with LongTaskTimer {
  private val semaphore = Semaphore.make(permits = 1)
  private var _count: Int = 0
  private var _max: Double = 0.0
  private var _total: Double = 0.0
  override def count: UIO[Double] = UIO.succeed(_count)
  override def totalTime(timeUnit: TimeUnit): UIO[Double] = UIO.succeed(Duration(_total, baseUnit).toUnit(timeUnit))
  override def max(timeUnit: TimeUnit): UIO[Double] = UIO.succeed(Duration(_max, baseUnit).toUnit(timeUnit))
  override def mean(timeUnit: TimeUnit): UIO[Double] = UIO.succeed {
    if (_count == 0) {
      Double.NaN
    } else {
      val avg = _total / _count
      Duration(avg, baseUnit).toUnit(timeUnit)
    }
  }
  override def record(duration: FiniteDuration): UIO[Unit] = semaphore.map { _ =>
    _count += 1
    val value = duration.toUnit(baseUnit)
    _total += value
    _max = Math.max(_max, value)
  }
  override def record(duration: zio.Duration): UIO[Unit] = {
    record(toScala(duration))
  }
  override def startTimerSample(): UIO[TimerSample] = UIO.succeed {
    new TimerSample {
      val startTime = zio.Runtime.default.unsafeRun(Clock.currentTime(baseUnit))
      override def stop(): UIO[Unit] = {
        for {
          endTime <- Clock.currentTime(baseUnit)
          _ <- record(FiniteDuration(endTime - startTime, baseUnit))
        } yield ()
      }
    }
  }
  override def baseTimeUnit: UIO[TimeUnit] = UIO.succeed(baseUnit)
}
