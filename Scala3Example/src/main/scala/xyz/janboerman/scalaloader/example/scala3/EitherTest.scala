package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}
import xyz.janboerman.scalaloader.example.scala3.OptionTest.saveFile

import java.io.File

object EitherTest {

    val saveFile = new File(ExamplePlugin.getDataFolder, "either-test.yml")
    if !ExamplePlugin.getDataFolder.exists() then ExamplePlugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender

        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala Either serialization-deserialization test")

        val left: Either[Int, String] = Left(1)
        val right: Either[Int, String] = Right("Hello, World!")

        val testCase = EitherTest(left, right)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("either-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("either-test")
        assert(testCase == actual, "EitherTest instances were not equal! :(")
        if ExamplePlugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    }

}

@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.FIELDS
))
case class EitherTest(left: Either[Int, String], right: Either[Int, String])
