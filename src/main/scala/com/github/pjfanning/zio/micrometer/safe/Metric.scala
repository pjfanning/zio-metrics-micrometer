package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{Counter, DistributionSummary, Gauge, HasMicrometerMeterId, LongTaskTimer, ReadOnlyGauge, ReadOnlyTimeGauge, TimeGauge, Timer, TimerSample}
import com.github.pjfanning.zio.micrometer.unsafe.{Counter => UnsafeCounter, DistributionSummary => UnsafeDistributionSummary, Gauge => UnsafeGauge, TimeGauge => UnsafeTimeGauge, Timer => UnsafeTimer}
import io.micrometer.core.instrument
import io.micrometer.core.instrument.distribution.pause.PauseDetector
import zio.{Clock, Duration, UIO, URIO, ZIO}

import scala.compat.java8.DurationConverters.toJava
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.control.NonFatal

object Counter extends LabelledMetric[Registry, Counter] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): URIO[Registry, Seq[String] => Counter] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeCounter.labelled(name, help, labelNames).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating counter " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackCounter)
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelled(
    name: String,
    help: Option[String] = None
  ): URIO[Registry, Counter] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeCounter.unlabelled(name, help).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating counter " + t)
          val fallbackZio = URIO.succeed(new FallbackCounter)
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
}

object Gauge extends LabelledMetric[Registry, Gauge] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): URIO[Registry, Seq[String] => Gauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.labelled(name, help, labelNames).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating gauge " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackGauge)
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelled(
    name: String,
    help: Option[String] = None
  ): URIO[Registry, Gauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.unlabelled(name, help).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating gauge " + t)
          val fallbackZio = URIO.succeed(new FallbackGauge)
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def labelledFunction(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    fun: () => Double
  ): URIO[Registry, Seq[String] => ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.labelledFunction(name, help, labelNames, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating gauge " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackFunctionGauge(fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelledFunction(
    name: String,
    help: Option[String] = None,
    fun: () => Double
  ): URIO[Registry, ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.unlabelledFunction(name, help, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating gauge " + t)
          val fallbackZio = URIO.succeed(new FallbackFunctionGauge(fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def labelledTFunction[T](
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    t: T,
    fun: T => Double
  ): URIO[Registry, Seq[String] => ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.labelledTFunction(name, help, labelNames, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = ZIO.log("Issue creating gauge " + throwable)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTFunctionGauge(t, fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelledTFunction[T](
    name: String,
    help: Option[String] = None,
    t: T,
    fun: T => Double
  ): URIO[Registry, ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.unlabelledTFunction(name, help, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = ZIO.log("Issue creating gauge " + throwable)
          val fallbackZio = URIO.succeed(new FallbackTFunctionGauge(t, fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
}

object TimeGauge extends LabelledMetric[Registry, TimeGauge] {

  /*
  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    timeUnit: TimeUnit
  ): URIO[Registry with Clock, Seq[String] => TimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      clock <- ZIO.environment[Clock]
      result <- UnsafeTimeGauge.labelled(name, help, labelNames, timeUnit).provideLayer(registry.get.unsafeRegistryLayer ++ clock).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating time gauge " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTimeGauge(timeUnit))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelled(
    name: String,
    help: Option[String] = None,
    timeUnit: TimeUnit
  ): URIO[Registry with Clock, Gauge] = {
    for {
      registry <- ZIO.environment[Registry]
      clock <- ZIO.environment[Clock]
      result <- UnsafeTimeGauge.unlabelled(name, help).provideLayer(registry.get.unsafeRegistryLayer ++ clock).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating time gauge " + t)
          val fallbackZio = URIO.succeed(new FallbackTimeGauge(timeUnit))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
  */

  def labelledFunction(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    timeUnit: TimeUnit,
    fun: => Double
  ): URIO[Registry, Seq[String] => ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.labelledFunction(name, help, labelNames, timeUnit, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating gauge " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackFunctionTimeGauge(timeUnit, fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelledFunction(
    name: String,
    help: Option[String] = None,
    timeUnit: TimeUnit,
    fun: => Double
  ): URIO[Registry, ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.unlabelledFunction(name, help, timeUnit, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating time gauge " + t)
          val fallbackZio = URIO.succeed(new FallbackFunctionTimeGauge(timeUnit, fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def labelledTFunction[T](
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    timeUnit: TimeUnit,
    t: T,
    fun: T => Double
  ): URIO[Registry, Seq[String] => ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.labelledTFunction(name, help, labelNames, timeUnit, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = ZIO.log("Issue creating time gauge " + throwable)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTFunctionTimeGauge(timeUnit, t, fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelledTFunction[T](
    name: String,
    help: Option[String] = None,
    timeUnit: TimeUnit,
    t: T,
    fun: T => Double
  ): URIO[Registry, ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.unlabelledTFunction(name, help, timeUnit, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = ZIO.log("Issue creating time gauge " + throwable)
          val fallbackZio = URIO.succeed(new FallbackTFunctionTimeGauge(timeUnit, t, fun))
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
}

object DistributionSummary extends LabelledMetric[Registry, DistributionSummary] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    scale: Double = 1.0,
    minimumExpectedValue: Option[Double] = None,
    maximumExpectedValue: Option[Double] = None,
    serviceLevelObjectives: Seq[Double] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None,
    baseUnit: Option[String] = None
  ): URIO[Registry, Seq[String] => DistributionSummary] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeDistributionSummary.labelled(name,
        help = help,
        labelNames = labelNames,
        scale = scale,
        minimumExpectedValue = minimumExpectedValue,
        maximumExpectedValue = maximumExpectedValue,
        serviceLevelObjectives = serviceLevelObjectives,
        distributionStatisticExpiry = distributionStatisticExpiry,
        distributionStatisticBufferLength = distributionStatisticBufferLength,
        publishPercentiles = publishPercentiles,
        publishPercentileHistogram = publishPercentileHistogram,
        percentilePrecision = percentilePrecision,
        baseUnit = baseUnit
      ).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating DistributionSummary " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackDistributionSummary)
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }

  def unlabelled(
    name: String,
    help: Option[String] = None,
    scale: Double = 1.0,
    minimumExpectedValue: Option[Double] = None,
    maximumExpectedValue: Option[Double] = None,
    serviceLevelObjectives: Seq[Double] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None,
    baseUnit: Option[String] = None
  ): URIO[Registry, DistributionSummary] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeDistributionSummary.unlabelled(
        name = name,
        help = help,
        scale = scale,
        minimumExpectedValue = minimumExpectedValue,
        maximumExpectedValue = maximumExpectedValue,
        serviceLevelObjectives = serviceLevelObjectives,
        distributionStatisticExpiry = distributionStatisticExpiry,
        distributionStatisticBufferLength = distributionStatisticBufferLength,
        publishPercentiles = publishPercentiles,
        publishPercentileHistogram = publishPercentileHistogram,
        percentilePrecision = percentilePrecision,
        baseUnit = baseUnit
      ).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating DistributionSummary " + t)
          val fallbackZio = URIO.succeed(new FallbackDistributionSummary)
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
}

object Timer extends LabelledMetric[Registry, Timer] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    minimumExpectedValue: Option[FiniteDuration] = None,
    maximumExpectedValue: Option[FiniteDuration] = None,
    serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None,
    pauseDetector: Option[PauseDetector] = None
  ): URIO[Registry, Seq[String] => Timer] =
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimer.labelled(name,
        help = help,
        labelNames = labelNames,
        minimumExpectedValue = minimumExpectedValue,
        maximumExpectedValue = maximumExpectedValue,
        serviceLevelObjectives = serviceLevelObjectives,
        distributionStatisticExpiry = distributionStatisticExpiry,
        distributionStatisticBufferLength = distributionStatisticBufferLength,
        publishPercentiles = publishPercentiles,
        publishPercentileHistogram = publishPercentileHistogram,
        percentilePrecision = percentilePrecision,
        pauseDetector = pauseDetector
      ).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating Timer " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio)
      }
    } yield result

  def unlabelled(
    name: String,
    help: Option[String] = None,
    minimumExpectedValue: Option[FiniteDuration] = None,
    maximumExpectedValue: Option[FiniteDuration] = None,
    serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None,
    pauseDetector: Option[PauseDetector] = None
  ): URIO[Registry, Timer] =
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimer.unlabelled(name,
        help = help,
        minimumExpectedValue = minimumExpectedValue,
        maximumExpectedValue = maximumExpectedValue,
        serviceLevelObjectives = serviceLevelObjectives,
        distributionStatisticExpiry = distributionStatisticExpiry,
        distributionStatisticBufferLength = distributionStatisticBufferLength,
        publishPercentiles = publishPercentiles,
        publishPercentileHistogram = publishPercentileHistogram,
        percentilePrecision = percentilePrecision,
        pauseDetector = pauseDetector
      ).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating Timer " + t)
          val fallbackZio = URIO.succeed(new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio)
      }
    } yield result

  def labelledLongTaskTimer(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    minimumExpectedValue: Option[FiniteDuration] = None,
    maximumExpectedValue: Option[FiniteDuration] = None,
    serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None
  ): URIO[Registry, Seq[String] => LongTaskTimer] =
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimer.labelledLongTaskTimer(name,
        help = help,
        labelNames = labelNames,
        minimumExpectedValue = minimumExpectedValue,
        maximumExpectedValue = maximumExpectedValue,
        serviceLevelObjectives = serviceLevelObjectives,
        distributionStatisticExpiry = distributionStatisticExpiry,
        distributionStatisticBufferLength = distributionStatisticBufferLength,
        publishPercentiles = publishPercentiles,
        publishPercentileHistogram = publishPercentileHistogram,
        percentilePrecision = percentilePrecision,
      ).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating LongTaskTimer " + t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio)
      }
    } yield result

  def unlabelledLongTaskTimer(
     name: String,
     help: Option[String] = None,
     minimumExpectedValue: Option[FiniteDuration] = None,
     maximumExpectedValue: Option[FiniteDuration] = None,
     serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
     distributionStatisticExpiry: Option[FiniteDuration] = None,
     distributionStatisticBufferLength: Option[Int] = None,
     publishPercentiles: Seq[Double] = Seq.empty,
     publishPercentileHistogram: Option[Boolean] = None,
     percentilePrecision: Option[Int] = None
   ): URIO[Registry, LongTaskTimer] =
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimer.unlabelledLongTaskTimer(name,
        help = help,
        minimumExpectedValue = minimumExpectedValue,
        maximumExpectedValue = maximumExpectedValue,
        serviceLevelObjectives = serviceLevelObjectives,
        distributionStatisticExpiry = distributionStatisticExpiry,
        distributionStatisticBufferLength = distributionStatisticBufferLength,
        publishPercentiles = publishPercentiles,
        publishPercentileHistogram = publishPercentileHistogram,
        percentilePrecision = percentilePrecision,
      ).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = ZIO.log("Issue creating LongTaskTimer " + t)
          val fallbackZio = URIO.succeed(new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio)
      }
    } yield result

}