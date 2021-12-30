package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}
import xyz.janboerman.scalaloader.example.scala3.TupleTest.saveFile

import java.io.File

object OptionTest {

    val saveFile = new File(ExamplePlugin.getDataFolder, "option-test.yml")
    if !ExamplePlugin.getDataFolder.exists() then ExamplePlugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender

        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala Option serialization-deserialization test")

        val o1: Option[String] = Some("Hello, World!")
        val o2: Option[Int] = None

        val testCase = OptionTest(o1, o2)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("option-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("option-test")
        assert(testCase == actual, "OptionTest instances were not equal! :(")
        if ExamplePlugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    }

}

@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.FIELDS
))
case class OptionTest(a: Option[String], b: Option[Int])
