import sbt._
import Keys._

import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._
import sbtunidoc.Plugin._

object MesosaurusBuild extends Build {

//////////////////////////////////////////////////////////////////////////////
// PROJECT INFO
//////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION    = "io.mesosphere"
  val PROJECT_NAME    = "mesosaurus"
  val PROJECT_VERSION = "0.1.0"
  val SCALA_VERSION   = "2.10.4"


  //////////////////////////////////////////////////////////////////////////////
  // DEPENDENCY VERSIONS
  //////////////////////////////////////////////////////////////////////////////

  val ARGPARSE4J_VERSION      = "0.4.3"
  val JODA_CONVERT_VERSION    = "1.6"
  val JODA_TIME_VERSION       = "2.3"
  val LOGBACK_VERSION         = "1.0.9"
  val MESOS_VERSION           = "0.19.1"
  val SCALATEST_VERSION       = "2.1.5"
  val SLF4J_VERSION           = "1.7.2"
  val TISCAF_VERSION          = "0.8"
  val TYPESAFE_CONFIG_VERSION = "1.0.2"
  val UNFILTERED_VERSION      = "0.7.1"


//////////////////////////////////////////////////////////////////////////////
// NATIVE LIBRARY PATHS
//////////////////////////////////////////////////////////////////////////////

  val pathToMesosLibs = "/usr/local/lib"

//////////////////////////////////////////////////////////////////////////////
// ROOT PROJECT
//////////////////////////////////////////////////////////////////////////////

  lazy val root = Project(
    id = PROJECT_NAME,
    base = file("."),
    settings = commonSettings
  )


//////////////////////////////////////////////////////////////////////////////
// SHARED SETTINGS
//////////////////////////////////////////////////////////////////////////////

  lazy val commonSettings = Project.defaultSettings ++
                            basicSettings ++
                            formatSettings ++
                            net.virtualvoid.sbt.graph.Plugin.graphSettings

  lazy val basicSettings = Seq(
    version := PROJECT_VERSION,
    organization := ORGANIZATION,
    scalaVersion := SCALA_VERSION,

    libraryDependencies ++= Seq(
      "net.databinder"              %% "unfiltered-filter" % UNFILTERED_VERSION,
      "net.databinder"              %% "unfiltered-jetty"  % UNFILTERED_VERSION,
      "org.apache.mesos"             % "mesos"             % MESOS_VERSION,
      "com.typesafe"                 % "config"            % TYPESAFE_CONFIG_VERSION,
      "org.slf4j"                    % "slf4j-api"         % SLF4J_VERSION,
      "ch.qos.logback"               % "logback-classic"   % LOGBACK_VERSION   % "runtime",
      "joda-time"                    % "joda-time"         % JODA_TIME_VERSION,
      "org.joda"                     % "joda-convert"      % JODA_CONVERT_VERSION,
      "net.sourceforge.argparse4j"   % "argparse4j"        % ARGPARSE4J_VERSION,
      "junit"                        % "junit"             % "4.11"    % "test",
      "org.scalatest"               %% "scalatest"         % SCALATEST_VERSION % "test",
      "org.gnieh"                   %% "tiscaf"            % TISCAF_VERSION
    ),

    scalacOptions in Compile ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    javaOptions += "-Djava.library.path=%s:%s".format(
      sys.props("java.library.path"),
      pathToMesosLibs
    ),

    fork in run := true,

    fork in Test := true,

    parallelExecution in Test := false
  )

  lazy val formatSettings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := FormattingPreferences()
      .setPreference(IndentWithTabs, false)
      .setPreference(IndentSpaces, 2)
      .setPreference(AlignParameters, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
      .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(PreserveSpaceBeforeArguments, true)
      .setPreference(SpaceBeforeColon, false)
      .setPreference(SpaceInsideBrackets, false)
      .setPreference(SpaceInsideParentheses, false)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(FormatXml, true)
    )


}