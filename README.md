# zio-metrics-micrometer
An effort to produce an equivalent of [zio-metrics](https://github.com/zio/zio-metrics) that uses [Micrometer](https://micrometer.io/) instead of directly supporting a specific metrics backend (eg Prometheus, Dropwizard, etc.).

The API is based on the prometheus2 package of zio-metrics but has been modified somewhat. The initial aim is to get some feedback on the API before adding support for all [Micrometer](https://micrometer.io/) metric types and adding full test coverage. Micrometer's aim is provide a common interface for multiple metric backends analagous to how [Slf4J](https://www.slf4j.org/) works for logging backends.

Micrometer supports many metric backends (eg Prometheus, Dropwizard, StatsD, etc.). See Micrometer's [own documentation](https://micrometer.io/docs) for details.

There are snapshot releases available at https://oss.sonatype.org/content/repositories/snapshots.

```scala
libraryDependencies += "com.github.pjfanning" %% "zio-metrics-micrometer" % "0.20.5"
```

| Release |Branch|Description|
|--------|---|---|
| 0.1.4  |zio1|ZIO 1 support. Still a prototype.|
| 0.20.2.1 |zio2-rc5|ZIO 2.0.0-RC5 support. Still a prototype.|
| 0.20.5 |zio2|ZIO 2.0.0-RC6 support. Still a prototype.|

## Safe vs Unsafe
* the 'unsafe' API returns ZIO effects that can fail
* the 'safe' API aims to return ZIO effects that do not fail but instead will log issues and return stub instances that will provide basic metric support without interacting with the real metric backend (because the real metric backend is not accessible, for instance).

## Labels/Tags
The current API uses the terms `labelled` and `unlabelled` based on zio-metrics naming conventions. Micrometer uses the term `tags`. The tag concept is described [here](https://micrometer.io/docs/concepts#_naming_meters).

## Metrics
* [Counters](https://micrometer.io/docs/concepts#_counters) are used to count the number of events.
* [Gauges](https://micrometer.io/docs/concepts#_gauges) are used to track values that can increase and decrease. zio-metrics-micrometer supports meters where the user gets to set, increment or decrement the values explicitly. It also supports wrapping function calls to existing functions that already have the values you want to track (e.g. you might have a connection pool instance that already has a function that returns the active connection count).
* [Distribution Summaries](https://micrometer.io/docs/concepts#_distribution_summaries) are used to track value distributions. You can define percentiles or histogram buckets.
* [Timers](https://micrometer.io/docs/concepts#_timers) are similar to Distribution Summaries but are specialised to cater for timing events. zio-metrics-micrometer allows you to choose [Long Task Timers](https://micrometer.io/docs/concepts#_long_task_timers) as well.
* [Time Gauges](https://micrometer.io/docs/concepts#_timegauge) are like Gauges but specialised for timing events.

## Example

[zio-http-example](https://github.com/pjfanning/zio-http-example) has a demo of how counter metrics can be maintained and also exposed as `metrics` endpoint.

```scala
  private val registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
  private val metricEnv = Clock.live ++ Registry.makeWith(registry)

  private def recordCount(method: String, path: String) = {
    for {
      c <- Counter.labelled("http", Some("HTTP counts"), Seq("method", "path"))
      result <- c(Seq(method, path)).inc()
    } yield result
  }
```

```scala
    case Method.GET -> !! / "text" => {
      ZIO.succeed(Response.text("Hello World!")).zipPar(
        recordCount("get", "text").provideLayer(metricEnv))
    }
```    

```scala
    case Method.GET -> !! / "metrics" => {
      ZIO.succeed(Response.text(registry.scrape()))
    }
```

## API

Counter (package `com.github.pjfanning.zio.micrometer.unsafe`)
```scala
  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): ZIO[Registry, Throwable, Seq[String] => Counter]

  def unlabelled(
    name: String,
    help: Option[String] = None,
  ): ZIO[Registry, Throwable, Counter]
```

Counter (package `com.github.pjfanning.zio.micrometer.safe`)
```scala
  def labelled(
    name: String,
    help: Option[String] = None,
    labelNames: Seq[String] = Seq.empty
  ): URIO[Registry, Seq[String] => Counter]

  def unlabelled(
    name: String,
    help: Option[String] = None
  ): URIO[Registry, Counter]
```


