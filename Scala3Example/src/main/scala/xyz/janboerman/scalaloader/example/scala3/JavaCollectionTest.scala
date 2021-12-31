package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.inventory.InventoryType
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}

import java.io.File

object JavaCollectionTest {

    val saveFile = new File(ExamplePlugin.getDataFolder, "java-collection-test.yml")
    if !ExamplePlugin.getDataFolder.exists() then ExamplePlugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender

        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala java.util.Collection serialization-deserialization test")

        val treeSet = new java.util.TreeSet[Int]()
        treeSet.addAll(java.util.List.of[Int](1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
        val arrayList = new java.util.ArrayList[java.util.UUID]()
        arrayList.add(java.util.UUID.randomUUID())
        val enumSet = java.util.EnumSet.noneOf(classOf[InventoryType])
        enumSet.addAll(java.util.Set.of[InventoryType](InventoryType.PLAYER, InventoryType.LOOM))

        val testCase = JavaCollectionTest(treeSet, arrayList, enumSet)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("java-collection-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("java-collection-test")
        assert(testCase == actual, "JavaCollectionTest instances were not equal! :(")
        if ExamplePlugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    }

}


@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.FIELDS
))
case class JavaCollectionTest(treeSet: java.util.TreeSet[Int], arrayList: java.util.ArrayList[java.util.UUID], enumSet: java.util.EnumSet[InventoryType])
