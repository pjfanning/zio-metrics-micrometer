package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import com.github.pjfanning.zio.micrometer.unsafe.{AtomicDouble, Registry, Counter => UnsafeCounter}
import zio.{UIO, URIO}

import scala.util.control.NonFatal

object Counter extends LabelledMetric[Registry with Logging, Counter] {

  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): URIO[Registry with Logging, Seq[String] => Counter] = {
    UnsafeCounter.labelled(name, help, labelNames).catchAll {
      case NonFatal(t) =>
        val logZio = log.throwable("Issue creating counter", t)
        val fallbackZio = URIO.succeed {
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
  }

  def unlabelled(
    name: String,
    help: Option[String] = None
  ): URIO[Registry with Logging, Counter] = {
    UnsafeCounter.unlabelled(name, help).catchAll {
      case NonFatal(t) =>
        val logZio = log.throwable("Issue creating counter", t)
        val fallbackZio = URIO.succeed {
          val atomicDouble = new AtomicDouble()
          new Counter {
            def inc(amount: Double): UIO[Unit] = URIO.succeed(atomicDouble.addAndGet(amount))
            def get: UIO[Double] = URIO.succeed(atomicDouble.get())
          }
        }
        fallbackZio.zipPar(logZio).map(_._1)
    }
  }
}
