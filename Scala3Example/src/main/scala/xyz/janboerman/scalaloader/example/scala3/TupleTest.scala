package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}

import java.io.File

object TupleTest {

    val saveFile = new File(ExamplePlugin.getDataFolder, "tuple-test.yml")
    if !ExamplePlugin.getDataFolder.exists() then ExamplePlugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender

        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala tuple serialization-deserialization test")

        val t1 = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22)
        val t2 = (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23)

        val testCase = TupleTest(t1, t2)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("tuple-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("tuple-test")
        assert(testCase == actual, "TupleTest instances were not equal! :(")
        if ExamplePlugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    }

}



@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.FIELDS
))
case class TupleTest[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23](
    val tuple22: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22),
    val tupleXXL: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22, T23)
)
