package xyz.janboerman.scalaloader.example.scala3

import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}

@Scala(ScalaVersion.v3_0_0)
@Api(ApiVersion.v1_16)
object Scala3Plugin extends ScalaPlugin(ScalaPluginDescription("Scala3Example", "0.15.1-SNAPSHOT")):

    override def onEnable(): Unit =
        getLogger.info("Hello from Scala 3!")
