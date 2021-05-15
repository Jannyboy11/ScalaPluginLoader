package xyz.janboerman.scalaloader.example.scala3

import xyz.janboerman.scalaloader.plugin.description.Version.ScalaLibrary
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, CustomScala, Version}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}

@CustomScala(Version(value = "3.0.0",
    scalaLibraryUrl = "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-library/2.13.5/scala-library-2.13.5.jar",
    scalaReflectUrl = "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-reflect/2.13.5/scala-reflect-2.13.5.jar",
    scalaLibs = Array(
        //extra additions to the standard library from scala 3
        ScalaLibrary(
            name = "scala3-library",
            url = "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala3-library_3.0.0-nonbootstrapped/3.0.0/scala3-library_3.0.0-nonbootstrapped-3.0.0.jar"
        ),
        //enable metaprogramming
        ScalaLibrary(
            name = "tasty-core",
            url = "https://search.maven.org/remotecontent?filepath=org/scala-lang/tasty-core_3.0.0-nonbootstrapped/3.0.0/tasty-core_3.0.0-nonbootstrapped-3.0.0.jar"
        )
    )
))
@Api(ApiVersion.v1_16)
object Scala3Plugin extends ScalaPlugin(ScalaPluginDescription("Scala3Example", "0.15.0-SNAPSHOT")):

    override def onEnable(): Unit =
        getLogger.info("Hello from Scala 3!")
