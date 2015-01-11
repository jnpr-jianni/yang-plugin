package net.juniper.yang

import java.io.File
import java.util.jar.JarFile
import sbt.Keys._
import sbt._
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object YangPlugin extends Plugin
{
    val yangPackageName = SettingKey[Option[String]]("yang-package-name")

    /**
     * Generate code from YANG via JNC
     */
    val Yang = config("yang")
    val yangGenerate = TaskKey[Seq[File]]("yang-generate")

    def yangGeneratorTask: Def.Initialize[Task[Seq[File]]] = Def.task {
        val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "yang", FilesInfo.lastModified, FilesInfo.exists) {
            in: Set[File] =>
                extractYangDependencies((managedClasspath in Runtime).value ++ (unmanagedClasspath in Runtime).value, streams.value.log)
                runJncGen(
                    srcFiles = in,
                    srcDir = (resourceDirectory in Yang).value,
                    targetBaseDir = (javaSource in Yang).value,
                    log = streams.value.log,
                    packageName = (yangPackageName in Yang).value,
                    dependencyModules = (projectDependencies in Compile).value,
                    moduleRoot = baseDirectory.value.getAbsolutePath
                )
        }
        cachedCompile(((resourceDirectory in Yang).value ** "*.yang").get.toSet).toSeq
    }

    def runJncGen( srcFiles: Set[File],
                   srcDir: File,
                   targetBaseDir: File,
                   log: Logger,
                   packageName: Option[String], dependencyModules: Seq[sbt.ModuleID], moduleRoot: String): Set[File] = {
        val targetDir = packageName.map {
            _.split('.').foldLeft(targetBaseDir) {
                _ / _
            }
        }.getOrElse(targetBaseDir)
        val baseArgs = Seq("-p", getYangDependencies(dependencyModules, moduleRoot), "-f", "jnc", "-d", targetDir.getAbsolutePath + "/mo", "--jnc-no-pkginfo", "--jnc-classpath-schema-loading")
        srcFiles map { file =>
            println("generating :" + file.toString)
            val args = baseArgs ++ Seq(file.toString)
            val exitCode = Process("pyang", args) ! log
            if (exitCode != 0) sys.error(s"pyang failed with exit code $exitCode")
        }

        val jrcArgs = Seq("-p", getYangDependencies(dependencyModules, moduleRoot), "-f", "jrc", "-d", targetDir.getAbsolutePath + "/api", "--jrc-no-pkginfo", "--jrc-classpath-schema-loading")
        srcFiles map { file =>
            println("generating :" + file.toString)
            val args = jrcArgs ++ Seq(file.toString)
            val exitCode = Process("pyang", args) ! log
            if (exitCode != 0) sys.error(s"pyang failed with exit code $exitCode")
        }
        (targetDir ** "*.java").get.toSet ++ (targetDir ** "*.scala").get.toSet
    }

    def extractYangDependencies(libs: Seq[Attributed[File]], log: Logger): Unit = {
        val yangDir = System.getProperty("user.home") + "/.yang"
        val args = Seq("-rf", yangDir + "/*")
        Process("rm", args) ! log
        new File(yangDir).mkdirs()

        libs.foreach { lib =>
            val file = lib.data
            if (file.exists) {
                if (!file.isDirectory && file.getName.endsWith(".jar")) {
                    val jarFile = new JarFile(file)
                    val yangEntry = jarFile.getEntry("yang")
                    if (yangEntry != null) {
                        val entries = jarFile.entries
                        for (entry <- entries) {
                            if(entry.getName.startsWith("yang/") && entry.getName.endsWith(".yang")) {
                                val targetFile = new File(yangDir + "/" + entry.getName.substring("yang/".length))
                                IO.write(targetFile, IO.readBytes(jarFile.getInputStream(entry)))
                            }
                        }
                    }
                }
            }
        }
    }

    def getYangDependencies(modules: Seq[sbt.ModuleID], moduleRoot: String): String = {
        val dArr = ArrayBuffer[String]()
        //add dependency path
        dArr += System.getProperty("user.home") + "/.yang"

        println("---------------------------------------------") + baseDirectory.toString
        //add dependency module paths
        for (module <- modules)
            dArr += baseDirectory.toString + "/" + module.name + "/src/main/resources/yang"

        //add current module path
        dArr += moduleRoot + "/src/main/resources/yang"

        dArr.mkString(":")
    }


    val yangSettings = inConfig(Yang)(Seq(
        resourceDirectory <<= (resourceDirectory in Compile) {_ / "yang"},
        javaSource <<= sourceManaged in Compile,
        yangGenerate <<= yangGeneratorTask,
        yangPackageName <<= yangPackageName in Yang
    )) ++ Seq(
        managedSourceDirectories in Compile <+= (javaSource in Yang),
        sourceGenerators in Compile <+= (yangGenerate in Yang),
        cleanFiles <+= (javaSource in Yang)
    )
}
