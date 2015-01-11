sbtPlugin := true

name := "yang-plugin"

organization := "net.juniper"

version := "0.1.1"

publishMavenStyle := false

publishTo := Some(Resolver.file("file",  new File(System.getProperty("user.home") + "/mavenrepo/sbt"))(Resolver.ivyStylePatterns))