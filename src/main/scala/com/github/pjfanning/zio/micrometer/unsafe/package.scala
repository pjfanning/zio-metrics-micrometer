package com.github.pjfanning.zio.micrometer

import io.micrometer.core.instrument
import zio.{RIO, Semaphore, Task, UIO, ULayer, ZIO}

import scala.collection.JavaConverters._

package object unsafe {
  type Registry = Registry.Service

  object Registry {
    trait Service {
      def meterRegistry: UIO[instrument.MeterRegistry]

      def updateRegistry[A](f: instrument.MeterRegistry => Task[A]): Task[A]

      def collect: UIO[Seq[instrument.Meter]]
    }

    private final class ServiceImpl(registry: instrument.MeterRegistry, lock: Semaphore) extends Service {

      def meterRegistry: UIO[instrument.MeterRegistry] = ZIO.succeed(registry)

      def updateRegistry[A](f: instrument.MeterRegistry => Task[A]): Task[A] = lock.withPermit {
        f(registry)
      }

      def collect: zio.UIO[Seq[instrument.Meter]] =
        ZIO.succeed(registry.getMeters.asScala.toSeq)
    }

    private object ServiceImpl {
      def makeWith(registry: instrument.MeterRegistry): UIO[ServiceImpl] =
        Semaphore
          .make(permits = 1)
          .map(new ServiceImpl(registry, _))
    }

    def makeService(registry: instrument.MeterRegistry): UIO[Registry.Service] = ServiceImpl.makeWith(registry)
    def makeWith(registry: instrument.MeterRegistry): ULayer[Registry] = makeService(registry).toLayer
  }

  def meterRegistry: RIO[Registry, instrument.MeterRegistry] =
    ZIO.serviceWithZIO(_.meterRegistry)

  def updateRegistry[A](f: instrument.MeterRegistry => Task[A]): RIO[Registry, A] =
    ZIO.serviceWithZIO(_.updateRegistry(f))

  def zipLabelsAsTags(labelNames: Seq[String], labelValues: Seq[String]): Seq[instrument.Tag] = {
    labelNames.zip(labelValues).map { case (labelName, labelValue) =>
      new instrument.ImmutableTag(labelName, labelValue)
    }
  }
}
