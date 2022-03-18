package com.github.pjfanning.zio.micrometer

import zio.UIO
import zio.Duration

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait TimerSample {
  def stop(): UIO[Unit]
}

trait TimerBase {
  def baseTimeUnit: UIO[TimeUnit]
  def totalTime(timeUnit: TimeUnit): UIO[Double]
  def max(timeUnit: TimeUnit): UIO[Double]
  def mean(timeUnit: TimeUnit): UIO[Double]
  def startTimerSample(): UIO[TimerSample]
}

trait Timer extends TimerBase {
  def count: UIO[Double]
  def record(duration: Duration): UIO[Unit]
  def record(duration: FiniteDuration): UIO[Unit]
}

trait LongTaskTimer extends TimerBase