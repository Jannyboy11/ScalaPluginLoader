package xyz.janboerman.scalaloader.example.scala3

import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}

@Scala(ScalaVersion.v3_0_0)
@Api(ApiVersion.v1_16)
object Scala3Plugin extends ScalaPlugin(ScalaPluginDescription("Scala3Example", "0.16.0-SNAPSHOT")):

    override def onEnable(): Unit =
        getLogger.info("Hello from Scala 3!")
//        CollectionTest.test()

    def assertionsEnabled: Boolean =
        try
            assert(false)
            false
        catch
            case ae: AssertionError =>
                true



