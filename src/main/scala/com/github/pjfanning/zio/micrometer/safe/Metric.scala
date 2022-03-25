package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.{Counter, Gauge}
import com.github.pjfanning.zio.micrometer.unsafe.{AtomicDouble, Counter => UnsafeCounter, Gauge => UnsafeGauge}
import zio.{UIO, URIO, ZIO}

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
          val fallbackZio = URIO.succeed {
            (labelValues: Seq[String]) => {
              val atomicDouble = new AtomicDouble()
              new Counter {
                override def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
                override def get: UIO[Double] = URIO.succeed(atomicDouble.get())
              }
            }
          }
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
          val fallbackZio = URIO.succeed {
            val atomicDouble = new AtomicDouble()
            new Counter {
              override def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
              override def get: UIO[Double] = URIO.succeed(atomicDouble.get())
            }
          }
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
          val fallbackZio = URIO.succeed {
            (labelValues: Seq[String]) => {
              val atomicDouble = new AtomicDouble()
              new Gauge {
                override def set(value: Double): UIO[Unit] = URIO.succeed(atomicDouble.set(value))
                override def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
                override def dec(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(-amount))
                override def get: UIO[Double] = URIO.succeed(atomicDouble.get())
              }
            }
          }
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
          val logZio = ZIO.log("Issue creating counter" + t)
          val fallbackZio = URIO.succeed {
            val atomicDouble = new AtomicDouble()
            new Gauge {
              override def set(value: Double): UIO[Unit] = URIO.succeed(atomicDouble.set(value))
              override def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
              override def dec(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(-amount))
              override def get: UIO[Double] = URIO.succeed(atomicDouble.get())
            }
          }
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
}