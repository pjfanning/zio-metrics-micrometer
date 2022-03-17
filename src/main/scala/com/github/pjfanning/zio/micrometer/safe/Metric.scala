package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.Counter
import com.github.pjfanning.zio.micrometer.unsafe.{AtomicDouble, Registry, Counter => UnsafeCounter}
import zio.{UIO, URIO, ZIO}
import zio.logging._

import scala.util.control.NonFatal

object Counter extends LabelledMetric[Registry with Logging, Counter] {

  def labelled(
    name: String,
    help: Option[String],
    labelNames: Seq[String]
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
        ZIO.tupledPar(fallbackZio, logZio).map(_._1)
    }
  }
}
