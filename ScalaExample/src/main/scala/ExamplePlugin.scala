import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.scala.Scala
import xyz.janboerman.scalaloader.scala.ScalaVersion

@Scala(version = ScalaVersion.v2_12_6)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription(
        ScalaVersion.v2_12_6.getName,
        "ScalaPlugin",
        "0.1-SNAPSHOT")) {

    override def onEnable(): Unit = {
        getLogger.info("Hello, World!");
    }
}
