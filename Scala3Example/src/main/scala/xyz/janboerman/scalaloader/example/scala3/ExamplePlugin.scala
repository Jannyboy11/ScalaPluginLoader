package xyz.janboerman.scalaloader.example.scala3

import xyz.janboerman.scalaloader.configurationserializable.runtime.{Codec, RuntimeConversions}
import xyz.janboerman.scalaloader.plugin.description.{Api, ApiVersion, Scala, ScalaVersion}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import zio.ZIO
import zio.console.*

@Scala(ScalaVersion.v3_1_0)
object ExamplePlugin extends ScalaPlugin {

    val syncRuntime = new BukkitRuntime(this).syncRuntime

    override def onEnable(): Unit =
        getLogger.info("Hello from Scala 3!")

        //use the identity codec for unknown Object and String objects to make the framework stop complaining :)
        RuntimeConversions.registerCodec[Object](getClassLoader(), classOf[Object], Codec.of[Object, Object](identity, identity))
        RuntimeConversions.registerCodec[String](getClassLoader(), classOf[String], Codec.of[String, String](identity, identity))
        //CollectionTest.test()
        TupleTest.test()
        OptionTest.test()
        EitherTest.test()

        val fourtyTwo: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
        val program = for
            name <- fourtyTwo.map(number => s"Jannyboy${number}")
            _ <- putStrLn(s"Hello $name, welcome to ZIO!")
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
