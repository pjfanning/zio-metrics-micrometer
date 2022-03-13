package com.github.pjfanning.zio

import java.{util => ju}
import io.micrometer.core.instrument
import zio.{Has, RIO, Semaphore, Task, UIO, ULayer, ZIO}

package object micrometer {
  type Registry = Has[Registry.Service]

  object Registry {
    trait Service {
      def collectorRegistry: UIO[instrument.MeterRegistry]

      def updateRegistry[A](f: instrument.MeterRegistry => Task[A]): Task[A]

      def collect: UIO[ju.List[instrument.Meter]]
    }

    private final class ServiceImpl(registry: instrument.MeterRegistry, lock: Semaphore) extends Service {

      def collectorRegistry: UIO[instrument.MeterRegistry] = ZIO.succeed(registry)

      def updateRegistry[A](f: instrument.MeterRegistry => Task[A]): Task[A] = lock.withPermit {
        f(registry)
      }

      def collect: zio.UIO[ju.List[instrument.Meter]] =
        ZIO.effectTotal(registry.getMeters)
    }

    private object ServiceImpl {
      def makeWith(registry: instrument.MeterRegistry): UIO[ServiceImpl] =
        Semaphore
          .make(permits = 1)
          .map(new ServiceImpl(registry, _))
    }

    def makeWith(registry: instrument.MeterRegistry): ULayer[Registry] = ServiceImpl.makeWith(registry).toLayer
  }

  def collectorRegistry: RIO[Registry, instrument.MeterRegistry] =
    ZIO.serviceWith(_.collectorRegistry)

  def updateRegistry[A](f: instrument.MeterRegistry => Task[A]): RIO[Registry, A] =
    ZIO.serviceWith(_.updateRegistry(f))

  def collect: RIO[Registry, ju.List[instrument.Meter]] =
    ZIO.serviceWith(_.collect)
}
