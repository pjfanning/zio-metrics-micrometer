package com.github.pjfanning.zio.micrometer.unsafe

import com.github.pjfanning.zio.micrometer.{Counter, Gauge, HasMicrometerMeterId, ReadOnlyGauge}
import io.micrometer.core.instrument
import io.micrometer.core.instrument.Meter
import zio._

import java.util.function.Supplier
import scala.collection.concurrent.TrieMap
import scala.collection.JavaConverters._

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
      .description(help.getOrElse(""))
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

/*
trait Timer {

  /** Returns how much time as elapsed since the timer was started. */
  def elapsed: UIO[Duration]

  /**
   * Records the duration since the timer was started in the associated metric and returns that
   * duration.
   */
  def stop: UIO[Duration]
}

trait TimerMetric {

  /** Starts a timer. When the timer is stopped, the duration is recorded in the metric. */
  def startTimer: UIO[Timer]

  /** A managed timer resource. */
  def timer: UManaged[Timer] = startTimer.toManaged(_.stop)

  /** Runs the given effect and records in the metric how much time it took to succeed or fail. */
  def observe[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] = timer.use(_ => zio)

  /**
   * Runs the given effect and records in the metric how much time it took to succeed. Do not
   * record failures.
   */
  def observeSuccess[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      timer <- startTimer
      a     <- zio
      _     <- timer.stop
    } yield a

  def observe(amount: Duration): UIO[Unit]
}

private abstract class TimerMetricImpl(clock: Clock.Service) extends TimerMetric {
  override def startTimer: UIO[Timer] =
    clock.instant.map { startTime =>
      new Timer {
        def elapsed: zio.UIO[Duration] =
          clock.instant.map(Duration.fromInterval(startTime, _))
        def stop: zio.UIO[Duration] =
          elapsed.tap(observe)
      }
    }
}
*/

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
                                       fun: T => Double) {

  def gaugeFor(labelValues: Seq[String]): ReadOnlyGauge = {
    val tags = zipLabelsAsTags(labelNames, labelValues)
    Gauge.getTFunctionGauge(meterRegistry, name, help, tags, t, fun)
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
        .description(help.getOrElse(""))
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
      .description(help.getOrElse(""))
      .tags(tags.asJava)
      .register(registry)
    new ReadOnlyGauge with HasMicrometerMeterId {
      override def get: UIO[Double]               = ZIO.succeed(fun)
      override def getMeterId: UIO[Meter.Id]      = ZIO.succeed(mGauge.getId)
    }
  }

  private[micrometer] def getTFunctionGauge[T](registry: instrument.MeterRegistry, name: String,
                                               help: Option[String], tags: Seq[instrument.Tag],
                                               t: T, fun: T => Double): ReadOnlyGauge = {
    val mGauge = instrument.Gauge
      .builder(name, new Supplier[Number]() {
        override def get(): Number = fun(t)
      })
      .description(help.getOrElse(""))
      .tags(tags.asJava)
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
    fun: T => Double
  ): ZIO[Registry, Throwable, Seq[String] => ReadOnlyGauge] = {
    for {
      gaugeWrapper <- updateRegistry { r =>
        ZIO.attempt(new TFunctionGaugeWrapper(r, name, help, labelNames, t, fun))
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

/*
sealed trait Buckets
object Buckets {
  object Default                                                          extends Buckets
  final case class Simple(buckets: Seq[Double])                           extends Buckets
  final case class Linear(start: Double, width: Double, count: Int)       extends Buckets
  final case class Exponential(start: Double, factor: Double, count: Int) extends Buckets
}

trait Histogram extends TimerMetric
object Histogram extends LabelledMetricP[Registry with Clock, Throwable, Buckets, Histogram] {
  def labelled(
    name: String,
    buckets: Buckets,
    help: Option[String],
    labels: Seq[String]
  ): ZIO[Registry with Clock, Throwable, Seq[String] => Histogram] =
    for {
      clock <- ZIO.service[Clock.Service]
      pHistogram <- updateRegistry { r =>
                     ZIO.effect {
                       val builder = instrument.Timer
                         .builder(name)
                         .description(help.getOrElse(""))
                         .publishPercentileHistogram()
                       (
                         buckets match {
                           case Buckets.Default              => builder
                           case Buckets.Simple(bs)           => builder.buckets(bs: _*)
                           case Buckets.Linear(s, w, c)      => builder.linearBuckets(s, w, c)
                           case Buckets.Exponential(s, f, c) => builder.exponentialBuckets(s, f, c)
                         }
                       ).register(r)
                     }
                   }
    } yield { (labels: Seq[String]) =>
      val child = pHistogram.labels(labels: _*)
      new TimerMetricImpl(clock) with Histogram {
        override def observe(amount: Duration): UIO[Unit] =
          ZIO.effectTotal(child.observe(amount.toNanos() * 1e-9))
      }
    }
}

final case class Quantile(percentile: Double, tolerance: Double)

trait Summary extends TimerMetric
object Summary extends LabelledMetricP[Registry with Clock, Throwable, List[Quantile], Summary] {
  def labelled(
    name: String,
    quantiles: List[Quantile],
    help: Option[String],
    labels: Seq[String]
  ): ZIO[Registry with Clock, Throwable, Seq[String] => Summary] =
    for {
      clock <- ZIO.service[Clock.Service]
      pHistogram <- updateRegistry { r =>
                     ZIO.effect {
                       val builder = instrument.Summary
                         .build()
                         .name(name)
                         .help(help.getOrElse(""))
                         .labelNames(labels: _*)
                       quantiles.foldLeft(builder)((b, c) => b.quantile(c.percentile, c.tolerance)).register(r)
                     }
                   }
    } yield { (labels: Seq[String]) =>
      val child = pHistogram.labels(labels: _*)
      new TimerMetricImpl(clock) with Summary {
        override def observe(amount: Duration): UIO[Unit] =
          ZIO.effectTotal(child.observe(amount.toNanos() * 1e-9))
      }
    }
}
*/
