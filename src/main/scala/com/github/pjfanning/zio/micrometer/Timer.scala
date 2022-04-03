package com.github.pjfanning.zio.micrometer

import zio.{Duration, UIO}

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait TimerSample {
  def stop(): UIO[Unit]
}

trait ReadOnlyTimeGauge {
  def baseTimeUnit: UIO[TimeUnit]
  def totalTime(timeUnit: TimeUnit): UIO[Double]
}

trait TimerBase extends ReadOnlyTimeGauge {
  def startTimerSample(): UIO[TimerSample]
}

trait TimeGauge extends TimerBase {
  def record(duration: Duration): UIO[Unit]
  def record(duration: FiniteDuration): UIO[Unit]
}

trait TimerWithMinMax extends TimerBase {
  def max(timeUnit: TimeUnit): UIO[Double]
  def mean(timeUnit: TimeUnit): UIO[Double]
}

trait Timer extends TimerWithMinMax {
  def count: UIO[Double]
  def record(duration: Duration): UIO[Unit]
  def record(duration: FiniteDuration): UIO[Unit]
}

trait LongTaskTimer extends TimerWithMinMax
