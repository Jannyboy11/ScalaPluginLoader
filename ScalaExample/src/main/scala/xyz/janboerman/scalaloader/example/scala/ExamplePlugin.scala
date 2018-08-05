package xyz.janboerman.scalaloader.example.scala

import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_12_6)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription(
        "ScalaPlugin",
        "0.1-SNAPSHOT")) {

    override def onEnable(): Unit = {
        getLogger.info("Hello, World!");
    }

}

