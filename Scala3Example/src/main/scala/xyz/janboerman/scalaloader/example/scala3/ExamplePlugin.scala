package xyz.janboerman.scalaloader.example.scala3

import xyz.janboerman.scalaloader.configurationserializable.runtime.{Codec, RuntimeConversions}
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}

@Scala(version = ScalaVersion.v3_6_2)
object ExamplePlugin extends ScalaPlugin {

    override def onEnable(): Unit =
        getLogger.info("Hello from Scala 3!")

        //CollectionTest.test()
        TupleTest.test()
        OptionTest.test()
        EitherTest.test()
        JavaCollectionTest.test()
        JavaMapTest.test()
        ScalaMapTest.test()

    def assertionsEnabled: Boolean =
        try
            assert(false)
            false
        catch
            case ae: AssertionError =>
                true

}
