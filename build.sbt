scalaVersion := "2.13.10"

scalacOptions ++= Seq(
  "-feature",
  "-language:reflectiveCalls",
)

// Chisel 3.5
addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.6" cross CrossVersion.full)
libraryDependencies += "edu.berkeley.cs" %% "chisel3" % "3.5.6"
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.5.6"
libraryDependencies += "edu.berkeley.cs" % "ip-contributions" % "0.5.4"

libraryDependencies += "net.fornwall" % "jelf" % "0.9.0"

// because I am reusing a.out -- should go with a change in compilation
Test / parallelExecution := false

// library name
name := "wildcat"
