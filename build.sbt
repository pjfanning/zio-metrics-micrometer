ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.8", "3.1.1")

val micrometerVersion = "1.8.3"
val zioVersion        = "1.0.12"

lazy val root = (project in file("."))
  .settings(
    name := "zio-metrics-micrometer",
    libraryDependencies ++= Seq(
      "dev.zio"          %% "zio"                            % zioVersion,
      "io.micrometer"    %  "micrometer-core"                % micrometerVersion,
      "io.micrometer"    %  "micrometer-registry-prometheus" % micrometerVersion % Test
    )
  )
