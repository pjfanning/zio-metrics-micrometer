package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{Counter, DistributionSummary, Gauge, ReadOnlyGauge}
import com.github.pjfanning.zio.micrometer.unsafe.{Counter => UnsafeCounter, DistributionSummary => UnsafeDistributionSummary, Gauge => UnsafeGauge}
import zio.{URIO, ZIO}

import scala.concurrent.duration.FiniteDuration
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