package xyz.janboerman.scalaloader.example.scala

import java.io.File

import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.Scan.IncludeProperty
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, Scan}

object SerializationTest {

    private val dataFolder = ExamplePlugin.getDataFolder;
    dataFolder.mkdirs();
    private val saveFile = new File(dataFolder, "serialization-test.yml");

    if (!saveFile.exists()) saveFile.createNewFile()

    def test(): Unit = {
        val writeConfiguration = new YamlConfiguration();
        writeConfiguration.set("case", CaseSerializationTest("case", 1337))
        writeConfiguration.set("methods", new MethodsSerializationTest("methods", 1338))
        writeConfiguration.set("fields", new FieldsSerializationTest("fields", 1339))
        writeConfiguration.save(saveFile)

        val readConfiguration = YamlConfiguration.loadConfiguration(saveFile)
        val caseSerializationTest = readConfiguration.get("case")
        val methodsSerializationTest = readConfiguration.get("methods")
        val fieldsSerializationTest = readConfiguration.get("fields")

        ExamplePlugin.getLogger.info(s"deserialized case = $caseSerializationTest")
        ExamplePlugin.getLogger.info(s"deserialized methods = $methodsSerializationTest")
        ExamplePlugin.getLogger.info(s"deserialized fields = $fieldsSerializationTest")
    }
}

@ConfigurationSerializable(as = "CaseSerializationTest", scan = new Scan(value = Scan.Type.CASE_CLASS))
case class CaseSerializationTest(name: String, count: Int)

@ConfigurationSerializable(as = "MethodsSerializationTest", scan = new Scan(value = Scan.Type.GETTER_SETTER_METHODS))
class MethodsSerializationTest(var name: String, var count: Int) {

    def getName(): String = name
    def setName(name: String): Unit = this.name = name

    @IncludeProperty("amount") def getCount(): Int = count
    @IncludeProperty("amount") def setCount(count: Int): Unit = this.count = count

    override def toString(): String = s"MethodsSerializableTest($name,$count)"

}

@ConfigurationSerializable(as = "FieldsSerializationTest", scan = new Scan(value = Scan.Type.FIELDS))
class FieldsSerializationTest private(private var name: String) {

    @IncludeProperty("amount") var count: Int = 0

    def this(name: String, count: Int) = {
        this(name)
        this.count = count
    }

    override def toString(): String = s"FieldsSerializationTest($name, $count)"
}
