package com.github.pjfanning.zio.micrometer

import zio.UIO
import zio.duration.Duration

import scala.concurrent.duration.{FiniteDuration, TimeUnit}

trait TimerSample {
  def stop(): UIO[Unit]
}

trait Timer {
  def baseTimeUnit: UIO[TimeUnit]
  def count: UIO[Double]
  def totalTime(timeUnit: TimeUnit): UIO[Double]
  def max(timeUnit: TimeUnit): UIO[Double]
  def mean(timeUnit: TimeUnit): UIO[Double]
  def record(duration: Duration): UIO[Unit]
  def record(duration: FiniteDuration): UIO[Unit]
  def startTimerSample(): UIO[TimerSample]
}
