ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.8", "3.1.1")

val micrometerVersion = "1.8.3"
val zioVersion        = "1.0.13"

lazy val root = (project in file("."))
  .settings(
    name := "zio-metrics-micrometer",
    libraryDependencies ++= Seq(
      "dev.zio"          %% "zio"                            % zioVersion,
      "dev.zio"          %% "zio-test"                       % zioVersion % Test,
      "dev.zio"          %% "zio-test-sbt"                   % zioVersion % Test,
      "io.micrometer"    %  "micrometer-core"                % micrometerVersion,
      "io.micrometer"    %  "micrometer-registry-prometheus" % micrometerVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
