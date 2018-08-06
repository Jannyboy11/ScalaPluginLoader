package xyz.janboerman.scalaloader.example.scala

import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_12_6)
object ExamplePlugin extends ScalaPlugin(new ScalaPluginDescription(
        "ScalaExample",
        "0.1-SNAPSHOT")) {

    getLogger().info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")
    }

    override def onDisable(): Unit = {
        getLogger.info("ScalaExample - I am disabled!")
    }

}

