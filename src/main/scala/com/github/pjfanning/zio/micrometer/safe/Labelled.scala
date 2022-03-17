package com.github.pjfanning.zio.micrometer.safe

import com.github.pjfanning.zio.micrometer.LabelList
import zio.URIO

/**
 * Helper to create strongly typed Micrometer labelled metrics.
 *
 * Metrics are defined with a list of labels whose length is statically known.
 * Operations on the metric (increment a counter for instance), require to pass a list of label
 * values with the same length.
 */
trait LabelledMetric[R, M] {
  def labelled(
    name: String,
    help: Option[String],
    labelNames: Seq[String]
  ): URIO[R, Seq[String] => M]

  def apply(name: String, help: Option[String]): URIO[R, M] =
    labelled(name, help, Nil).map(f => f(Nil))
  def apply[L <: LabelList](name: String, help: Option[String], labels: L): URIO[R, Labelled[L]] =
    labelled(name, help, labels.toList).map(f => (l: L) => f(l.toList))

  type Labelled[L <: LabelList] = L => M
}
