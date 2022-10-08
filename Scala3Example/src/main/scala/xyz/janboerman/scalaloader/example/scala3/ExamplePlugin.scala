package xyz.janboerman.scalaloader.example.scala3

import xyz.janboerman.scalaloader.configurationserializable.runtime.{Codec, RuntimeConversions}
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import zio.ZIO
import zio.Console

@Scala(ScalaVersion.v3_2_0)
object ExamplePlugin extends ScalaPlugin {

    var syncRuntime = new BukkitRuntime(this).syncRuntime

    override def onEnable(): Unit =
        getLogger.info("Hello from Scala 3!")

        //CollectionTest.test()
        TupleTest.test()
        OptionTest.test()
        EitherTest.test()
        JavaCollectionTest.test()
        JavaMapTest.test()
        ScalaMapTest.test()

        val fortyTwo: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
        val program = for
            name <- fortyTwo.map(number => s"Jannyboy${number}")
            _ <- Console.printLine(s"Hello $name, welcome to ZIO!")
        yield ()
        syncRuntime.unsafeRun(program)


    def assertionsEnabled: Boolean =
        try
            assert(false)
            false
        catch
            case ae: AssertionError =>
                true

}
