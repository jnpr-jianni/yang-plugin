package net.juniper.yang

import java.io.{FileWriter, FileFilter, File}

import scala.collection.mutable.ArrayBuffer

/**
 * Generate route class with all routing and a routing list will be used to generate proxy file for nginx/apache.
 * Created by juntao on 1/11/15.
 */
object RoutesGenerator {
  def generate(basePath: String, packName: String, name: String): Unit = {
    val packPath = packName.replaceAll("\\.", "/")
    val imports = generateImports(basePath + "/" + packPath + "/api")
    if(imports.size > 0) {
      val routePackageName = packName + ".api"
      val strBuilder = new StringBuilder
      strBuilder.append("package ").append(packName).append("\n")
      for(imp <- imports) {
        strBuilder.append("import " + routePackageName + "." + imp.dirName + "." + imp.className + "\n")
      }
      strBuilder.append("trait " + name + " extends AnyRef")
      for(imp <- imports) {
        strBuilder.append(" with " + imp.className)
      }
      strBuilder.append(" {\n")
      strBuilder.append("  val " + name.charAt(0).toLower + name.substring(1) + " = ")
      for(i <- 0 until imports.length) {
        if(i != 0)
          strBuilder.append(" ~ ")
        strBuilder.append(imports(i).routing)
      }
      strBuilder.append("\n")
      strBuilder.append("}")

      val routeOut = new FileWriter(basePath + "/" + packPath + "/" + name + ".scala")
      try
        routeOut.write(strBuilder.mkString)
      finally
        routeOut close

      val listBuiler = new StringBuilder
      for(imp <- imports) {
        listBuiler.append(imp.className.charAt(0).toLower + imp.className.substring(1)).append("\n")
      }
      val listOut = new FileWriter(basePath + "/" + packPath + "/routes.list")
      try
        listOut.write(listBuiler.mkString)
      finally
        listOut close

    }
  }

  def generateImports(basePath: String): Array[ImportInfo] = {
    val baseDir = new File(basePath)
    val imports = ArrayBuffer[ImportInfo]()
    if(baseDir.exists()) {
      val subDirs = baseDir.listFiles(new FileFilter() {
        def accept(file: File): Boolean = {
          file.isDirectory
        }
      })

      for(subDir <- subDirs) {
        val routesFiles = subDir.listFiles(new FileFilter {
          def accept(file: File): Boolean = {
            file.isFile && file.getName.endsWith("Routes.scala")
          }
        })
        for(routeFile <- routesFiles) {
          val routeClassName = routeFile.getName.substring(0, routeFile.getName.length - ".scala".length)
          var routePrefix = routeClassName.substring(0, routeClassName.length - "Routes".length)
          routePrefix = routePrefix.charAt(0).toLower + routePrefix.substring(1)
          val simpleRoutingName = routePrefix + "RestApiRouting"
          imports += new ImportInfo(subDir.getName, routeClassName, simpleRoutingName)
        }
      }
    }
    imports.toArray
  }

  class ImportInfo(val dirName: String, val className: String, val routing: String)
}
