package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import com.github.pjfanning.zio.micrometer.unsafe.{AtomicDouble, Counter => UnsafeCounter}
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
          val logZio = ZIO.log("Issue creating counter" + t)
          val fallbackZio = URIO.succeed {
            (labelValues: Seq[String]) => {
              val atomicDouble = new AtomicDouble()
              new Counter {
                def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
                def get: UIO[Double] = URIO.succeed(atomicDouble.get())
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
          val logZio = ZIO.log("Issue creating counter" + t)
          val fallbackZio = URIO.succeed {
            val atomicDouble = new AtomicDouble()
            new Counter {
              def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))

              def get: UIO[Double] = URIO.succeed(atomicDouble.get())
            }
          }
          fallbackZio.zipPar(logZio)
      }
    } yield result
  }
}
