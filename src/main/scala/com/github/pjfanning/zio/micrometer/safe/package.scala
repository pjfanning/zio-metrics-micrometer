package com.github.pjfanning.zio.micrometer

import com.github.pjfanning.zio.micrometer.unsafe.{Registry => UnsafeRegistry}
import io.micrometer.core.instrument
import zio.{Has, RIO, Semaphore, UIO, ULayer, ZIO}

import scala.collection.JavaConverters._

package object safe {
  type Registry = Has[Registry.Service]

  object Registry {
    trait Service {
      def meterRegistry: UIO[instrument.MeterRegistry]

      def updateRegistry[A](f: instrument.MeterRegistry => UIO[A]): UIO[A]

      def collect: UIO[Seq[instrument.Meter]]

      def unsafeRegistryLayer: ULayer[UnsafeRegistry]
    }

    private final class ServiceImpl(registry: instrument.MeterRegistry, lock: Semaphore) extends Service {

      def meterRegistry: UIO[instrument.MeterRegistry] = ZIO.succeed(registry)

      def updateRegistry[A](f: instrument.MeterRegistry => UIO[A]): UIO[A] = lock.withPermit {
        f(registry)
      }

      def collect: zio.UIO[Seq[instrument.Meter]] =
        ZIO.effectTotal(registry.getMeters.asScala.toSeq)

      lazy val unsafeRegistryLayer: ULayer[UnsafeRegistry] = {
        UnsafeRegistry.makeWith(registry)
      }
    }

    private object ServiceImpl {
      def makeWith(registry: instrument.MeterRegistry): UIO[ServiceImpl] =
        Semaphore
          .make(permits = 1)
          .map(new ServiceImpl(registry, _))
    }

    def makeWith(registry: instrument.MeterRegistry): ULayer[Registry] = ServiceImpl.makeWith(registry).toLayer
  }

  def meterRegistry: RIO[Registry, instrument.MeterRegistry] =
    ZIO.serviceWith(_.meterRegistry)

  def updateRegistry[A](f: instrument.MeterRegistry => UIO[A]): RIO[Registry, A] =
    ZIO.serviceWith(_.updateRegistry(f))

}
