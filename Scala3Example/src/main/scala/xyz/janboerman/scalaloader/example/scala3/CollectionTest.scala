package xyz.janboerman.scalaloader.example.scala3

import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}

import java.lang.annotation.Annotation
import scala.collection.immutable.{ArraySeq, SortedSet}

object CollectionTest:

    val saveFile = new File(Scala3Plugin.getDataFolder, "collection-test.yml")
    if !Scala3Plugin.getDataFolder.exists() then Scala3Plugin.getDataFolder.mkdirs()
    if !saveFile.exists() then saveFile.createNewFile()

    def test(): Unit =
        val console = Scala3Plugin.getServer.getConsoleSender
        console.sendMessage(s"${ChatColor.YELLOW}Basic Scala collection serialization-deserialization test")

        val ints = Seq(0, 1, 2)
        val booleans = Set(false, true)
        val floats = IndexedSeq(3.0F, 4.0F)
        val strings = ArraySeq("Hello, ", "World!")
        val chars = SortedSet('A', 'B', 'C', 'D', 'E', 'F')

        val testCase = CollectionTest(ints, booleans, floats, strings, chars)

        var yamlConfig = YamlConfiguration()
        yamlConfig.set("collection-test", testCase)
        yamlConfig.save(saveFile)

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile)
        val actual = yamlConfig.get("collection-test")
        assert(testCase == actual, "CollectionTest instances were not equal! :(")
        if Scala3Plugin.assertionsEnabled then console.sendMessage(s"${ChatColor.GREEN}Test passed!")
    end test

@ConfigurationSerializable(scan = Scan(
    value = Scan.Type.GETTER_SETTER_METHODS
))
class CollectionTest(@Scan.IncludeProperty var integers: Seq[Int],
                     @Scan.IncludeProperty var booleans: Set[Boolean],
                     @Scan.IncludeProperty var floats: IndexedSeq[Float],
                     @Scan.IncludeProperty var strings: ArraySeq[String],
                     @Scan.IncludeProperty var chars: SortedSet[Char]):

    override def equals(obj: Any): Boolean = obj match {
        case that: CollectionTest =>
            this.integers == that.integers &&
            this.booleans == that.booleans &&
            this.floats == that.floats &&
            this.strings == that.strings &&
            this.chars == that.chars
        case _ =>
            false
    }

    override def hashCode(): Int = java.util.Objects.hash(integers, booleans, floats, strings, chars)

    override def toString: String =
        s"CollectionTest($integers, $booleans, $floats, $strings, $chars)"
