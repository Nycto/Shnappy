name := "Shnappy"

organization := "com.roundeights"

version := "0.1"

scalaVersion := "2.10.1"

// append -deprecation to the options passed to the Scala compiler
scalacOptions ++= Seq("-deprecation", "-feature")

// Application dependencies
libraryDependencies ++= Seq(
    "com.roundeights" %% "attempt" % "0.1",
    "com.roundeights" %% "scalon" % "0.1",
    "com.roundeights" %% "foldout" % "0.1",
    "com.roundeights" %% "skene" % "0.1",
    "com.roundeights" %% "tubeutil" % "0.1",
    "com.roundeights" %% "vfunk" % "0.1",
    "com.roundeights" %% "hasher" % "1.0.0",
    "org.pegdown" % "pegdown" % "1.3.0",
    "com.github.jknack" % "handlebars" % "1.0.0",
    "net.databinder.dispatch" %% "dispatch-core" % "0.10.1",
    "org.slf4j" % "slf4j-simple" % "1.7.2",
    "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"
)

