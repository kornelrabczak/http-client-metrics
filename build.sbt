name := "http-client-metrics"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "io.dropwizard.metrics" % "metrics-core" % "3.2.3"
libraryDependencies += "io.dropwizard.metrics" % "metrics-graphite" % "3.2.3"
libraryDependencies += "com.zaxxer" % "HikariCP" % "2.6.3"
libraryDependencies += "org.postgresql" % "postgresql" % "42.1.3"

