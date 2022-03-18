package com.github.pjfanning.zio.micrometer.unsafe

import com.github.pjfanning.zio.micrometer.{Counter, DistributionSummary, Gauge, HasMicrometerMeterId, LongTaskTimer, ReadOnlyGauge, Timer, TimerSample}
import io.micrometer.core.instrument
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.distribution.pause.PauseDetector
import zio._

import java.util.function.Supplier
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConverters._
import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.{FiniteDuration, TimeUnit}

private case class MeterKey(name: String, tags: Iterable[instrument.Tag])

private class CounterWrapper(meterRegistry: instrument.MeterRegistry,
                             name: String,
                             help: Option[String],
                             labelNames: Seq[String]) {

  def counterFor(labelValues: Seq[String]): instrument.Counter = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Counter.getCounter(meterRegistry, name, help, tags)
  }
}

object Counter extends LabelledMetric[Registry, Throwable, Counter] {

  private[micrometer] def getCounter(registry: instrument.MeterRegistry, name: String,
                                     help: Option[String], tags: Seq[instrument.Tag]): instrument.Counter = {
    instrument.Counter
      .builder(name)
      .description(help.orNull)
      .tags(tags.asJava)
      .register(registry)
  }

  def labelled(
    name: String,
    help: Option[String],
    labelNames: Seq[String]
  ): ZIO[Registry, Throwable, Seq[String] => Counter] =
    for {
      counterWrapper <- updateRegistry { r =>
        ZIO.attempt(new CounterWrapper(r, name, help, labelNames))
      }
    } yield { (labelValues: Seq[String]) =>
      new Counter with HasMicrometerMeterId {
        private lazy val counter = counterWrapper.counterFor(labelValues)

        override def inc(amount: Double): UIO[Unit] = ZIO.succeed(counter.increment(amount))

        override def get: UIO[Double] = ZIO.succeed(counter.count())

        override def getMeterId: UIO[instrument.Meter.Id] = ZIO.succeed(counter.getId)
      }
    }
}

private class DistributionSummaryWrapper(meterRegistry: instrument.MeterRegistry,
                                         name: String,
                                         help: Option[String],
                                         labelNames: Seq[String],
                                         scale: Double = 1.0,
                                         minimumExpectedValue: Option[Double] = None,
                                         maximumExpectedValue: Option[Double] = None,
                                         serviceLevelObjectives: Seq[Double] = Seq.empty,
                                         distributionStatisticExpiry: Option[FiniteDuration] = None,
                                         distributionStatisticBufferLength: Option[Int] = None,
                                         publishPercentiles: Seq[Double] = Seq.empty,
                                         publishPercentileHistogram: Option[Boolean] = None,
                                         percentilePrecision: Option[Int] = None,
                                         baseUnit: Option[String] = None) {

  def summaryFor(labelValues: Seq[String]): DistributionSummary = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    DistributionSummary.getDistributionSummary(meterRegistry, name, help, tags, scale = scale,
      minimumExpectedValue = minimumExpectedValue, maximumExpectedValue = maximumExpectedValue,
      serviceLevelObjectives = serviceLevelObjectives, distributionStatisticExpiry = distributionStatisticExpiry,
      distributionStatisticBufferLength = distributionStatisticBufferLength, publishPercentiles = publishPercentiles,
      publishPercentileHistogram = publishPercentileHistogram, percentilePrecision = percentilePrecision,
      baseUnit = baseUnit
    )
  }
}

object DistributionSummary extends LabelledMetric[Registry, Throwable, DistributionSummary] {

  private[micrometer] def getDistributionSummary(registry: instrument.MeterRegistry, name: String,
                                                 help: Option[String], tags: Seq[instrument.Tag],
                                                 scale: Double = 1.0,
                                                 minimumExpectedValue: Option[Double] = None,
                                                 maximumExpectedValue: Option[Double] = None,
                                                 serviceLevelObjectives: Seq[Double] = Seq.empty,
                                                 distributionStatisticExpiry: Option[FiniteDuration] = None,
                                                 distributionStatisticBufferLength: Option[Int] = None,
                                                 publishPercentiles: Seq[Double] = Seq.empty,
                                                 publishPercentileHistogram: Option[Boolean] = None,
                                                 percentilePrecision: Option[Int] = None,
                                                 baseUnit: Option[String] = None): DistributionSummary = {
    val dsBuilder = instrument.DistributionSummary
      .builder(name)
      .description(help.orNull)
      .tags(tags.asJava)
      .scale(scale)
    minimumExpectedValue match {
      case Some(min) => dsBuilder.minimumExpectedValue(min)
      case _ =>
    }
    maximumExpectedValue match {
      case Some(max) => dsBuilder.maximumExpectedValue(max)
      case _ =>
    }
    if (serviceLevelObjectives.nonEmpty) dsBuilder.serviceLevelObjectives(serviceLevelObjectives: _*)
    distributionStatisticExpiry match {
      case Some(exp) => dsBuilder.distributionStatisticExpiry(toJava(exp))
      case _ =>
    }
    distributionStatisticBufferLength match {
      case Some(len) => dsBuilder.distributionStatisticBufferLength(len)
      case _ =>
    }
    if (publishPercentiles.nonEmpty) dsBuilder.publishPercentiles(publishPercentiles: _*)
    publishPercentileHistogram match {
      case Some(bool) => dsBuilder.publishPercentileHistogram(bool)
      case _ =>
    }
    percentilePrecision match {
      case Some(len) => dsBuilder.percentilePrecision(len)
      case _ =>
    }
    baseUnit match {
      case Some(unit) => dsBuilder.baseUnit(unit)
      case _ =>
    }
    val ds = dsBuilder.register(registry)
    new DistributionSummary with HasMicrometerMeterId {
      override def count: UIO[Double] = ZIO.succeed(ds.count())
      override def totalAmount: UIO[Double] = ZIO.succeed(ds.totalAmount())
      override def max: UIO[Double] = ZIO.succeed(ds.max())
      override def mean: UIO[Double] = ZIO.succeed(ds.mean())
      override def getMeterId: UIO[instrument.Meter.Id] = ZIO.succeed(ds.getId)
      override def record(value: Double): UIO[Unit] = ZIO.succeed(ds.record(value))
    }
  }

  override def labelled(
     name: String,
     help: Option[String],
     labelNames: Seq[String]
   ): ZIO[Registry, Throwable, Seq[String] => DistributionSummary] =
    labelledWithOptions(name, help, labelNames)

  def labelledWithOptions(
    name: String,
    help: Option[String],
    labelNames: Seq[String],
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
  ): ZIO[Registry, Throwable, Seq[String] => DistributionSummary] =
    for {
      summaryWrapper <- updateRegistry { r =>
        ZIO.attempt(new DistributionSummaryWrapper(r, name = name, help = help, labelNames = labelNames,
          scale = scale, minimumExpectedValue = minimumExpectedValue, maximumExpectedValue = maximumExpectedValue,
          serviceLevelObjectives = serviceLevelObjectives, distributionStatisticExpiry = distributionStatisticExpiry,
          distributionStatisticBufferLength = distributionStatisticBufferLength,
          publishPercentiles = publishPercentiles, publishPercentileHistogram = publishPercentileHistogram,
          percentilePrecision = percentilePrecision, baseUnit = baseUnit
        ))
      }
    } yield (labelValues: Seq[String]) =>
        summaryWrapper.summaryFor(labelValues)
}

private class TimerWrapper(meterRegistry: instrument.MeterRegistry,
                           name: String,
                           help: Option[String],
                           labelNames: Seq[String],
                           minimumExpectedValue: Option[FiniteDuration] = None,
                           maximumExpectedValue: Option[FiniteDuration] = None,
                           serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
                           distributionStatisticExpiry: Option[FiniteDuration] = None,
                           distributionStatisticBufferLength: Option[Int] = None,
                           publishPercentiles: Seq[Double] = Seq.empty,
                           publishPercentileHistogram: Option[Boolean] = None,
                           percentilePrecision: Option[Int] = None,
                           pauseDetector: Option[PauseDetector] = None) {

  def timerFor(labelValues: Seq[String]): Timer = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Timer.getTimer(meterRegistry, name, help, tags,
      minimumExpectedValue = minimumExpectedValue, maximumExpectedValue = maximumExpectedValue,
      serviceLevelObjectives = serviceLevelObjectives, distributionStatisticExpiry = distributionStatisticExpiry,
      distributionStatisticBufferLength = distributionStatisticBufferLength, publishPercentiles = publishPercentiles,
      publishPercentileHistogram = publishPercentileHistogram, percentilePrecision = percentilePrecision,
      pauseDetector = pauseDetector
    )
  }

  def longTaskTimerFor(labelValues: Seq[String]): LongTaskTimer = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Timer.getLongTaskTimer(meterRegistry, name, help, tags,
      minimumExpectedValue = minimumExpectedValue, maximumExpectedValue = maximumExpectedValue,
      serviceLevelObjectives = serviceLevelObjectives, distributionStatisticExpiry = distributionStatisticExpiry,
      distributionStatisticBufferLength = distributionStatisticBufferLength, publishPercentiles = publishPercentiles,
      publishPercentileHistogram = publishPercentileHistogram, percentilePrecision = percentilePrecision
    )
  }

}

object Timer extends LabelledMetric[Registry, Throwable, Timer] {

  private[micrometer] def getTimer(registry: instrument.MeterRegistry, name: String,
                                   help: Option[String], tags: Seq[instrument.Tag],
                                   minimumExpectedValue: Option[FiniteDuration] = None,
                                   maximumExpectedValue: Option[FiniteDuration] = None,
                                   serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
                                   distributionStatisticExpiry: Option[FiniteDuration] = None,
                                   distributionStatisticBufferLength: Option[Int] = None,
                                   publishPercentiles: Seq[Double] = Seq.empty,
                                   publishPercentileHistogram: Option[Boolean] = None,
                                   percentilePrecision: Option[Int] = None,
                                   pauseDetector: Option[PauseDetector] = None): Timer = {
    val builder = instrument.Timer
      .builder(name)
      .description(help.orNull)
      .tags(tags.asJava)
    minimumExpectedValue match {
      case Some(min) => builder.minimumExpectedValue(toJava(min))
      case _ =>
    }
    maximumExpectedValue match {
      case Some(max) => builder.maximumExpectedValue(toJava(max))
      case _ =>
    }
    if (serviceLevelObjectives.nonEmpty) {
      builder.serviceLevelObjectives(serviceLevelObjectives.map(toJava): _*)
    }
    distributionStatisticExpiry match {
      case Some(exp) => builder.distributionStatisticExpiry(toJava(exp))
      case _ =>
    }
    distributionStatisticBufferLength match {
      case Some(len) => builder.distributionStatisticBufferLength(len)
      case _ =>
    }
    if (publishPercentiles.nonEmpty) builder.publishPercentiles(publishPercentiles: _*)
    publishPercentileHistogram match {
      case Some(bool) => builder.publishPercentileHistogram(bool)
      case _ =>
    }
    percentilePrecision match {
      case Some(len) => builder.percentilePrecision(len)
      case _ =>
    }
    pauseDetector match {
      case Some(detector) => builder.pauseDetector(detector)
      case _ =>
    }
    val timer = builder.register(registry)
    new Timer with HasMicrometerMeterId {
      override def baseTimeUnit: UIO[TimeUnit] = ZIO.succeed(timer.baseTimeUnit())
      override def count: UIO[Double] = ZIO.succeed(timer.count())
      override def totalTime(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed(timer.totalTime(timeUnit))
      override def max(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed(timer.max(timeUnit))
      override def mean(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed(timer.mean(timeUnit))
      override def getMeterId: UIO[instrument.Meter.Id] = ZIO.succeed(timer.getId)
      override def record(duration: Duration): UIO[Unit] = ZIO.succeed(timer.record(duration))
      override def record(duration: FiniteDuration): UIO[Unit] = ZIO.succeed(timer.record(toJava(duration)))

      override def startTimerSample(): UIO[TimerSample] = ZIO.succeed {
        val sample = instrument.Timer.start()
        new TimerSample {
          override def stop(): UIO[Unit] = ZIO.succeed(sample.stop(timer))
        }
      }
    }
  }

  private[micrometer] def getLongTaskTimer(registry: instrument.MeterRegistry, name: String,
                                           help: Option[String], tags: Seq[instrument.Tag],
                                           minimumExpectedValue: Option[FiniteDuration] = None,
                                           maximumExpectedValue: Option[FiniteDuration] = None,
                                           serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
                                           distributionStatisticExpiry: Option[FiniteDuration] = None,
                                           distributionStatisticBufferLength: Option[Int] = None,
                                           publishPercentiles: Seq[Double] = Seq.empty,
                                           publishPercentileHistogram: Option[Boolean] = None,
                                           percentilePrecision: Option[Int] = None): LongTaskTimer = {
    val builder = instrument.LongTaskTimer
      .builder(name)
      .description(help.orNull)
      .tags(tags.asJava)
    minimumExpectedValue match {
      case Some(min) => builder.minimumExpectedValue(toJava(min))
      case _ =>
    }
    maximumExpectedValue match {
      case Some(max) => builder.maximumExpectedValue(toJava(max))
      case _ =>
    }
    if (serviceLevelObjectives.nonEmpty) {
      builder.serviceLevelObjectives(serviceLevelObjectives.map(toJava): _*)
    }
    distributionStatisticExpiry match {
      case Some(exp) => builder.distributionStatisticExpiry(toJava(exp))
      case _ =>
    }
    distributionStatisticBufferLength match {
      case Some(len) => builder.distributionStatisticBufferLength(len)
      case _ =>
    }
    if (publishPercentiles.nonEmpty) builder.publishPercentiles(publishPercentiles: _*)
    publishPercentileHistogram match {
      case Some(bool) => builder.publishPercentileHistogram(bool)
      case _ =>
    }
    percentilePrecision match {
      case Some(len) => builder.percentilePrecision(len)
      case _ =>
    }
    val timer = builder.register(registry)
    new LongTaskTimer with HasMicrometerMeterId {
      override def baseTimeUnit: UIO[TimeUnit] = ZIO.succeed(timer.baseTimeUnit())
      override def totalTime(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed(timer.duration(timeUnit))
      override def max(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed(timer.max(timeUnit))
      override def mean(timeUnit: TimeUnit): UIO[Double] = ZIO.succeed(timer.mean(timeUnit))
      override def getMeterId: UIO[instrument.Meter.Id] = ZIO.succeed(timer.getId)

      override def startTimerSample(): UIO[TimerSample] = ZIO.succeed {
        val sample = timer.start()
        new TimerSample {
          override def stop(): UIO[Unit] = ZIO.succeed(sample.stop())
        }
      }
    }
  }

  override def labelled(
     name: String,
     help: Option[String],
     labelNames: Seq[String]
   ): ZIO[Registry, Throwable, Seq[String] => Timer] =
    labelledWithOptions(name, help, labelNames)

  def labelledLongTaskTimer(
    name: String,
    help: Option[String],
    labelNames: Seq[String],
    minimumExpectedValue: Option[FiniteDuration] = None,
    maximumExpectedValue: Option[FiniteDuration] = None,
    serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None
  ): ZIO[Registry, Throwable, Seq[String] => LongTaskTimer] =
    for {
      timerWrapper <- updateRegistry { r =>
        ZIO.attempt(new TimerWrapper(r, name = name, help = help, labelNames = labelNames,
          minimumExpectedValue = minimumExpectedValue, maximumExpectedValue = maximumExpectedValue,
          serviceLevelObjectives = serviceLevelObjectives, distributionStatisticExpiry = distributionStatisticExpiry,
          distributionStatisticBufferLength = distributionStatisticBufferLength,
          publishPercentiles = publishPercentiles, publishPercentileHistogram = publishPercentileHistogram,
          percentilePrecision = percentilePrecision
        ))
      }
    } yield (labelValues: Seq[String]) =>
      timerWrapper.longTaskTimerFor(labelValues)

  def labelledWithOptions(
    name: String,
    help: Option[String],
    labelNames: Seq[String],
    minimumExpectedValue: Option[FiniteDuration] = None,
    maximumExpectedValue: Option[FiniteDuration] = None,
    serviceLevelObjectives: Seq[FiniteDuration] = Seq.empty,
    distributionStatisticExpiry: Option[FiniteDuration] = None,
    distributionStatisticBufferLength: Option[Int] = None,
    publishPercentiles: Seq[Double] = Seq.empty,
    publishPercentileHistogram: Option[Boolean] = None,
    percentilePrecision: Option[Int] = None,
    pauseDetector: Option[PauseDetector] = None
  ): ZIO[Registry, Throwable, Seq[String] => Timer] =
    for {
      timerWrapper <- updateRegistry { r =>
        ZIO.attempt(new TimerWrapper(r, name = name, help = help, labelNames = labelNames,
          minimumExpectedValue = minimumExpectedValue, maximumExpectedValue = maximumExpectedValue,
          serviceLevelObjectives = serviceLevelObjectives, distributionStatisticExpiry = distributionStatisticExpiry,
          distributionStatisticBufferLength = distributionStatisticBufferLength,
          publishPercentiles = publishPercentiles, publishPercentileHistogram = publishPercentileHistogram,
          percentilePrecision = percentilePrecision, pauseDetector = pauseDetector
        ))
      }
    } yield (labelValues: Seq[String]) =>
      timerWrapper.timerFor(labelValues)
}

private class GaugeWrapper(meterRegistry: instrument.MeterRegistry,
                           name: String,
                           help: Option[String],
                           labelNames: Seq[String]) {

  def gaugeFor(labelValues: Seq[String]): Gauge = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Gauge.getGauge(meterRegistry, name, help, tags)
  }
}

private class FunctionGaugeWrapper(meterRegistry: instrument.MeterRegistry,
                                   name: String,
                                   help: Option[String],
                                   labelNames: Seq[String],
                                   fun: => Double) {

  def gaugeFor(labelValues: Seq[String]): ReadOnlyGauge = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Gauge.getFunctionGauge(meterRegistry, name, help, tags, fun)
  }
}

private class TFunctionGaugeWrapper[T](meterRegistry: instrument.MeterRegistry,
                                       name: String,
                                       help: Option[String],
                                       labelNames: Seq[String],
                                       t: T,
                                       fun: T => Double,
                                       strongReference: Boolean = false) {

  def gaugeFor(labelValues: Seq[String]): ReadOnlyGauge = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Gauge.getTFunctionGauge(meterRegistry, name, help, tags, t, fun, strongReference)
  }
}

object Gauge extends LabelledMetric[Registry, Throwable, Gauge] {

  private val gaugeRegistryMap = TrieMap[instrument.MeterRegistry, TrieMap[MeterKey, Gauge]]()

  private def gaugeMap(registry: instrument.MeterRegistry): TrieMap[MeterKey, Gauge] = {
    gaugeRegistryMap.getOrElseUpdate(registry, TrieMap[MeterKey, Gauge]())
  }

  private[micrometer] def getGauge(registry: instrument.MeterRegistry, name: String,
                                   help: Option[String], tags: Seq[instrument.Tag]): Gauge = {
    gaugeMap(registry).getOrElseUpdate(MeterKey(name, tags), {
      val atomicDouble = new AtomicDouble()
      val mGauge = instrument.Gauge
        .builder(name, new Supplier[Number]() {
          override def get(): Number = atomicDouble.get()
        })
        .description(help.orNull)
        .tags(tags.asJava)
        .register(registry)
      new Gauge with HasMicrometerMeterId {
        override def get: UIO[Double]               = ZIO.succeed(atomicDouble.get())
        override def set(value: Double): UIO[Unit]  = ZIO.succeed(atomicDouble.set(value))
        override def inc(amount: Double): UIO[Unit] = ZIO.succeed(atomicDouble.addAndGet(amount))
        override def dec(amount: Double): UIO[Unit] = ZIO.succeed(atomicDouble.addAndGet(-amount))
        override def getMeterId: UIO[Meter.Id]      = ZIO.succeed(mGauge.getId)
      }
    })
  }

  private[micrometer] def getFunctionGauge(registry: instrument.MeterRegistry, name: String,
                                           help: Option[String], tags: Seq[instrument.Tag],
                                           fun: => Double): ReadOnlyGauge = {
    val mGauge = instrument.Gauge
      .builder(name, new Supplier[Number]() {
        override def get(): Number = fun
      })
      .description(help.orNull)
      .tags(tags.asJava)
      .register(registry)
    new ReadOnlyGauge with HasMicrometerMeterId {
      override def get: UIO[Double]               = ZIO.succeed(fun)
      override def getMeterId: UIO[Meter.Id]      = ZIO.succeed(mGauge.getId)
    }
  }

  private[micrometer] def getTFunctionGauge[T](registry: instrument.MeterRegistry, name: String,
                                               help: Option[String], tags: Seq[instrument.Tag],
                                               t: T, fun: T => Double, strongReference: Boolean = false): ReadOnlyGauge = {
    val mGauge = instrument.Gauge
      .builder(name, new Supplier[Number]() {
        override def get(): Number = fun(t)
      })
      .description(help.orNull)
      .tags(tags.asJava)
      .strongReference(strongReference)
      .register(registry)
    new ReadOnlyGauge with HasMicrometerMeterId {
      override def get: UIO[Double]               = ZIO.succeed(fun(t))
      override def getMeterId: UIO[Meter.Id]      = ZIO.succeed(mGauge.getId)
    }
  }

  def labelled(
    name: String,
    help: Option[String],
    labelNames: Seq[String],
    fun: () => Double
  ): ZIO[Registry, Throwable, Seq[String] => ReadOnlyGauge] = {
    for {
      gaugeWrapper <- updateRegistry { r =>
        ZIO.attempt(new FunctionGaugeWrapper(r, name, help, labelNames, fun()))
      }
    } yield { (labelValues: Seq[String]) =>
      gaugeWrapper.gaugeFor(labelValues)
    }
  }

  def labelled[T](
    name: String,
    help: Option[String],
    labelNames: Seq[String],
    t: T,
    fun: T => Double,
    strongReference: Boolean = false
  ): ZIO[Registry, Throwable, Seq[String] => ReadOnlyGauge] = {
    for {
      gaugeWrapper <- updateRegistry { r =>
        ZIO.attempt(new TFunctionGaugeWrapper(r, name, help, labelNames, t, fun, strongReference))
      }
    } yield { (labelValues: Seq[String]) =>
      gaugeWrapper.gaugeFor(labelValues)
    }
  }

  def labelled(
    name: String,
    help: Option[String],
    labelNames: Seq[String]
  ): ZIO[Registry, Throwable, Seq[String] => Gauge] =
    for {
      gaugeWrapper <- updateRegistry { r =>
        ZIO.attempt(new GaugeWrapper(r, name, help, labelNames))
      }
    } yield { (labelValues: Seq[String]) =>
      gaugeWrapper.gaugeFor(labelValues)
    }
}