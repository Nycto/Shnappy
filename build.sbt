name := "Shnappy"

organization := "com.roundeights"

version := "0.2"

scalaVersion := "2.11.2"

// append -deprecation to the options passed to the Scala compiler
scalacOptions ++= Seq("-deprecation", "-feature")

publishTo := Some("Spikemark" at "https://spikemark.herokuapp.com/maven/roundeights")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

resolvers ++= Seq("RoundEights" at "http://maven.spikemark.net/roundeights")

// Application dependencies
libraryDependencies ++= Seq(
    "com.roundeights" %% "attempt" % "0.1",
    "com.roundeights" %% "scalon" % "0.1",
    "com.roundeights" %% "foldout" % "0.1",
    "com.roundeights" %% "skene" % "0.1",
    "com.roundeights" %% "tubeutil" % "0.1",
    "com.roundeights" %% "vfunk" % "0.1",
    "com.roundeights" %% "hasher" % "1.0.0",
    "org.pegdown" % "pegdown" % "1.4.1" force,
    "com.github.jknack" % "handlebars" % "1.1.2",
    "net.databinder.dispatch" %% "dispatch-core" % "0.10.1",
    "org.slf4j" % "slf4j-simple" % "1.7.7",
    "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
    "org.specs2" %% "specs2" % "2.3.11" % "test"
)

