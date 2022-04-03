package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{Counter, DistributionSummary, Gauge, LongTaskTimer, ReadOnlyGauge, ReadOnlyTimeGauge, TimeGauge, Timer}
import com.github.pjfanning.zio.micrometer.unsafe.{Counter => UnsafeCounter, DistributionSummary => UnsafeDistributionSummary, Gauge => UnsafeGauge, TimeGauge => UnsafeTimeGauge, Timer => UnsafeTimer}
import io.micrometer.core.instrument.distribution.pause.PauseDetector
import zio.clock.Clock
import zio.{URIO, ZIO}
import zio.logging._

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS, TimeUnit}
import scala.util.control.NonFatal

object Counter extends LabelledMetric[Registry with Logging, Counter] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): URIO[Registry with Logging, Seq[String] => Counter] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeCounter.labelled(name, help, labelNames).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating counter", t)
          val fallbackZio = URIO.effectTotal((_: Seq[String]) => new FallbackCounter)
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def unlabelled(
    name: String,
    help: Option[String] = None
  ): URIO[Registry with Logging, Counter] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeCounter.unlabelled(name, help).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating counter", t)
          val fallbackZio = URIO.effectTotal(new FallbackCounter)
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }
}

object Gauge extends LabelledMetric[Registry, Gauge] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): URIO[Registry with Logging, Seq[String] => Gauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.labelled(name, help, labelNames).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating gauge", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackGauge)
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def unlabelled(
    name: String,
    help: Option[String] = None
  ): URIO[Registry with Logging, Gauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.unlabelled(name, help).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating gauge", t)
          val fallbackZio = URIO.succeed(new FallbackGauge)
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def labelledFunction(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    fun: () => Double
  ): URIO[Registry with Logging, Seq[String] => ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.labelledFunction(name, help, labelNames, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating gauge", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackFunctionGauge(fun))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def unlabelledFunction(
    name: String,
    help: Option[String] = None,
    fun: () => Double
  ): URIO[Registry with Logging, ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.unlabelledFunction(name, help, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating gauge", t)
          val fallbackZio = URIO.succeed(new FallbackFunctionGauge(fun))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def labelledTFunction[T](
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    t: T,
    fun: T => Double
  ): URIO[Registry with Logging, Seq[String] => ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.labelledTFunction(name, help, labelNames, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = log.throwable("Issue creating gauge", throwable)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTFunctionGauge(t, fun))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def unlabelledTFunction[T](
    name: String,
    help: Option[String] = None,
    t: T,
    fun: T => Double
  ): URIO[Registry with Logging, ReadOnlyGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeGauge.unlabelledTFunction(name, help, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = log.throwable("Issue creating gauge", throwable)
          val fallbackZio = URIO.succeed(new FallbackTFunctionGauge(t, fun))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }
}

object TimeGauge extends LabelledMetric[Registry, TimeGauge] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    timeUnit: TimeUnit
  ): URIO[Registry with Logging, Seq[String] => TimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.labelled(name, help, labelNames, timeUnit)
        .provideLayer(registry.get.unsafeRegistryLayer ++ Clock.live)
        .catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating time gauge", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTimeGauge(timeUnit))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }
  def unlabelled(
    name: String,
    help: Option[String] = None,
    timeUnit: TimeUnit
  ): URIO[Registry with Logging, TimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.unlabelled(name, help)
        .provideLayer(registry.get.unsafeRegistryLayer ++ Clock.live)
        .catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating time gauge", t)
          val fallbackZio = URIO.succeed(new FallbackTimeGauge(timeUnit))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def labelledFunction(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty,
    timeUnit: TimeUnit,
    fun: => Double
  ): URIO[Registry with Logging, Seq[String] => ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.labelledFunction(name, help, labelNames, timeUnit, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating time gauge", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackFunctionTimeGauge(timeUnit, fun))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def unlabelledFunction(
    name: String,
    help: Option[String] = None,
    timeUnit: TimeUnit,
    fun: => Double
  ): URIO[Registry with Logging, ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.unlabelledFunction(name, help, timeUnit, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(t) =>
          val logZio = log.throwable("Issue creating time gauge", t)
          val fallbackZio = URIO.succeed(new FallbackFunctionTimeGauge(timeUnit, fun))
          fallbackZio.zipPar(logZio).map(_._1)
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
  ): URIO[Registry with Logging, Seq[String] => ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.labelledTFunction(name, help, labelNames, timeUnit, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = log.throwable("Issue creating time gauge", throwable)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTFunctionTimeGauge(timeUnit, t, fun))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }

  def unlabelledTFunction[T](
    name: String,
    help: Option[String] = None,
    timeUnit: TimeUnit,
    t: T,
    fun: T => Double
  ): URIO[Registry with Logging, ReadOnlyTimeGauge] = {
    for {
      registry <- ZIO.environment[Registry]
      result <- UnsafeTimeGauge.unlabelledTFunction(name, help, timeUnit, t, fun).provideLayer(registry.get.unsafeRegistryLayer).catchAll {
        case NonFatal(throwable) =>
          val logZio = log.throwable("Issue creating time gauge", throwable)
          val fallbackZio = URIO.succeed(new FallbackTFunctionTimeGauge(timeUnit, t, fun))
          fallbackZio.zipPar(logZio).map(_._1)
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
  ): URIO[Registry with Logging, Seq[String] => DistributionSummary] = {
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
          val logZio = log.throwable("Issue creating DistributionSummary", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackDistributionSummary)
          fallbackZio.zipPar(logZio).map(_._1)
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
  ): URIO[Registry with Logging, DistributionSummary] = {
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
          val logZio = log.throwable("Issue creating DistributionSummary", t)
          val fallbackZio = URIO.succeed(new FallbackDistributionSummary)
          fallbackZio.zipPar(logZio).map(_._1)
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
  ): URIO[Registry with Logging, Seq[String] => Timer] =
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
          val logZio = log.throwable("Issue creating Timer", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio).map(_._1)
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
  ): URIO[Registry with Logging, Timer] =
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
          val logZio = log.throwable("Issue creating Timer", t)
          val fallbackZio = URIO.succeed(new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio).map(_._1)
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
   ): URIO[Registry with Logging, Seq[String] => LongTaskTimer] =
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
          val logZio = log.throwable("Issue creating LongTaskTimer", t)
          val fallbackZio = URIO.succeed((_: Seq[String]) => new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio).map(_._1)
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
   ): URIO[Registry with Logging, LongTaskTimer] =
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
          val logZio = log.throwable("Issue creating LongTaskTimer", t)
          val fallbackZio = URIO.succeed(new FallbackTimer(NANOSECONDS))
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result

}