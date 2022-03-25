package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import com.github.pjfanning.zio.micrometer.unsafe.{AtomicDouble, Counter => UnsafeCounter}
import zio.{UIO, URIO, ZIO}
import zio.logging._

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
          val fallbackZio = URIO.effectTotal {
            (labelValues: Seq[String]) => {
              val atomicDouble = new AtomicDouble()
              new Counter {
                def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
                def get: UIO[Double] = URIO.succeed(atomicDouble.get())
              }
            }
          }
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
          val fallbackZio = URIO.effectTotal {
            val atomicDouble = new AtomicDouble()
            new Counter {
              def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
              def get: UIO[Double] = URIO.succeed(atomicDouble.get())
            }
          }
          fallbackZio.zipPar(logZio).map(_._1)
      }
    } yield result
  }
}
