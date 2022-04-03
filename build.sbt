import sbtghactions.JavaSpec.Distribution.Zulu

organization := "com.github.pjfanning"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.8", "3.1.1")

val micrometerVersion = "1.8.4"
val zioVersion        = "1.0.13"
val zioLoggingVersion = "0.5.9"

lazy val root = (project in file("."))
  .settings(
    name := "zio-metrics-micrometer",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-java8-compat"             % "1.0.2",
      "dev.zio"                %% "zio"                            % zioVersion,
      "dev.zio"                %% "zio-logging"                    % zioLoggingVersion,
      "dev.zio"                %% "zio-test"                       % zioVersion % Test,
      "dev.zio"                %% "zio-test-sbt"                   % zioVersion % Test,
      "io.micrometer"          %  "micrometer-core"                % micrometerVersion,
      "io.micrometer"          %  "micrometer-registry-prometheus" % micrometerVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

Test / parallelExecution := false

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

Test / publishArtifact := false

pomIncludeRepository := { _ => false }

homepage := Some(url("https://github.com/pjfanning/zio-metrics-micrometer"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

pomExtra := (
  <developers>
    <developer>
      <id>pjfanning</id>
      <name>PJ Fanning</name>
      <url>https://github.com/pjfanning</url>
    </developer>
  </developers>
  )

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Zulu, "8"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("zio1")),
  RefPredicate.Equals(Ref.Branch("zio2")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("ci-release"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.CI_DEPLOY_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.CI_DEPLOY_USERNAME }}",
      "CI_SNAPSHOT_RELEASE" -> "+publishSigned"
    )
  )
)

