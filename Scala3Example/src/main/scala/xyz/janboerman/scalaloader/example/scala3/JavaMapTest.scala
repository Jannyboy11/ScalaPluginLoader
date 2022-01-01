package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}

import java.io.File

object JavaMapTest {

    val saveFile = new File(ExamplePlugin.getDataFolder, "java-map-test.yml")
    if !ExamplePlugin.getDataFolder.exists() then ExamplePlugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender

        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala java.util.Map serialization-deserialization test")

        val map1 = new java.util.TreeMap[Int, Byte]()
        map1.put(1, 1.asInstanceOf[Byte])
        val map2 = new java.util.LinkedHashMap[Byte, Int]()
        map2.put(1.asInstanceOf[Byte], 1)

        val testCase = JavaMapTest(map1, map2)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("java-map-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("java-map-test")
        assert(testCase == actual, "JavaMapTest instances were not equal! :(")
        if ExamplePlugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    }

}

@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.FIELDS
))
case class JavaMapTest(map1: java.util.Map[Int, Byte], map2: java.util.Map[Byte, Int])
