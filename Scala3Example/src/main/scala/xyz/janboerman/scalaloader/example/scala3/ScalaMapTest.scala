package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}

import java.io.File

object ScalaMapTest {

    val saveFile = new File(ExamplePlugin.getDataFolder, "scala-map-test.yml")
    if !ExamplePlugin.getDataFolder.exists() then ExamplePlugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender

        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala scala.collection.Map serialization-deserialization test")

        val map1 = Map("key" -> "value")
        val map5 = Map("k1" -> "v1", "k2" -> "v2", "k3" -> "v3", "k4" -> "v4", "k5" -> "v5")
        val mutableMap = new scala.collection.mutable.HashMap[String, String]()

        val testCase = ScalaMapTest(map1, map5, mutableMap)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("scala-map-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("scala-map-test")
        assert(testCase == actual, "ScalaMapTest instances were not equal! :(")
        if ExamplePlugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    }

}

@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.FIELDS
))
case class ScalaMapTest(map1: Map[String, String], map5: Map[String, String], mut: scala.collection.mutable.Map[String, String])