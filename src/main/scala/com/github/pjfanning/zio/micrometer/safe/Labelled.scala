package com.github.pjfanning.zio.micrometer.safe

/**
 * Helper to create strongly typed Micrometer labelled metrics.
 *
 * Metrics are defined with a list of labels whose length is statically known.
 * Operations on the metric (increment a counter for instance), require to pass a list of label
 * values with the same length.
 */
trait LabelledMetric[R, M]