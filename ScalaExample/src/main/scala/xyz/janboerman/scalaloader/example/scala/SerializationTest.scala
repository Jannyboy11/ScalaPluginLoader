package xyz.janboerman.scalaloader.example.scala

import org.bukkit.ChatColor

import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import xyz.janboerman.scalaloader.configurationserializable.Scan.IncludeProperty
import xyz.janboerman.scalaloader.configurationserializable.{ConfigurationSerializable, DelegateSerialization, Scan}

object SerializationTest {

    private val dataFolder = ExamplePlugin.getDataFolder;
    dataFolder.mkdirs();
    private val saveFile = new File(dataFolder, "serialization-test.yml");

    if (!saveFile.exists()) saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender
        console.sendMessage(s"${ChatColor.YELLOW}Test ${ChatColor.RESET}case class/methods/fields value.equals(deserialize(serialize(value))")

        val writeConfiguration = new YamlConfiguration();

        val caseTest = CaseSerializationTest("case", 1337)
        val methodTest = new MethodsSerializationTest("methods", 1338)
        val fieldTest = new FieldsSerializationTest("fields", 1339)

        writeConfiguration.set("case", caseTest)
        writeConfiguration.set("methods", methodTest)
        writeConfiguration.set("fields", fieldTest)
        writeConfiguration.save(saveFile)

        val readConfiguration = YamlConfiguration.loadConfiguration(saveFile)
        val caseSerializationTest = readConfiguration.get("case")
        val methodsSerializationTest = readConfiguration.get("methods")
        val fieldsSerializationTest = readConfiguration.get("fields")

        assert(caseTest == caseSerializationTest);
        assert(methodTest == methodsSerializationTest)
        assert(fieldTest == fieldsSerializationTest)
        if (ExamplePlugin.assertionsEnabled()) {
            console.sendMessage(s"${ChatColor.GREEN}Test passed!")
        }

        Maybe.test()
    }
}

@ConfigurationSerializable(as = "CaseSerializationTest", scan = new Scan(value = Scan.Type.CASE_CLASS))
case class CaseSerializationTest(name: String, count: Int)

@ConfigurationSerializable(as = "MethodsSerializationTest", scan = new Scan(value = Scan.Type.GETTER_SETTER_METHODS))
class MethodsSerializationTest(private var name: String, private var count: Int) {

    @IncludeProperty def getName(): String = name
    @IncludeProperty def setName(name: String): Unit = this.name = name

    @IncludeProperty("amount") def getCount(): Int = count
    @IncludeProperty("amount") def setCount(count: Int): Unit = this.count = count

    override def toString(): String = s"MethodsSerializableTest($name,$count)"

    override def equals(obj: Any): Boolean = {
        obj match {
            case mst: MethodsSerializationTest => this.getName() == mst.getName() && this.getCount() == mst.getCount()
            case _ => false
        }
    }

    override def hashCode(): Int = java.util.Objects.hash(getName(), getCount())

}

@ConfigurationSerializable(as = "FieldsSerializationTest", scan = new Scan(value = Scan.Type.FIELDS))
class FieldsSerializationTest private(private var name: String) {

    @IncludeProperty("amount") var count: Int = 0

    def this(name: String, count: Int) = {
        this(name)
        this.count = count
    }

    override def toString(): String = s"FieldsSerializationTest($name, $count)"

    override def equals(obj: Any): Boolean = {
        obj match {
            case fst: FieldsSerializationTest => this.name == fst.name && this.count == fst.count
            case _ => false
        }
    }

    override def hashCode(): Int = java.util.Objects.hash(name, count)
}



object Maybe {
    private val saveFile = new File(ExamplePlugin.getDataFolder, "maybe.yml")
    ExamplePlugin.getDataFolder.mkdirs()
    if (!saveFile.exists()) saveFile.createNewFile()

    def test(): Unit = {
        val console = ExamplePlugin.getServer.getConsoleSender
        console.sendMessage(s"${ChatColor.YELLOW}Test ${ChatColor.RESET}maybe.equals(deserialize(serialize(maybe))")

        val justHello = Maybe("Hello")
        val nothing = Maybe(null)

        var config = new YamlConfiguration()
        config.set("justHello", justHello)
        config.set("nothing", nothing)
        config.save(saveFile)

        config = YamlConfiguration.loadConfiguration(saveFile)
        assert(justHello == config.get("justHello"))
        assert(nothing == config.get("nothing"))
        if (ExamplePlugin.assertionsEnabled()) {
            console.sendMessage(s"${ChatColor.GREEN}Test passed!")
        }
    }

    def apply[T](value: T): Maybe[T] = if (value == null) NoValue else Just(value)


//    //generated by the classloader! (as a static method on Maybe)
//    def deserialize(map: java.util.Map[String, AnyRef]): Maybe[_] = {
//        import org.bukkit.configuration.serialization.ConfigurationSerialization
//
//        ConfigurationSerialization.deserializeObject(
//            map,
//            ConfigurationSerialization.getClassByAlias(map.get("$variant").asInstanceOf[String])
//        ).asInstanceOf[Maybe[_]]
//    }
}

//TODO should 'as' be allowed in DelegateSerialization?
//TODO should it distribute amongst the children?
//TODO or should it just not be present in the @DelegateSerialization thing?

@DelegateSerialization(value = Array(classOf[Just[_]], classOf[NoValue.type]), as = "Maybe")
sealed trait Maybe[+T] {
   //if we would define serialize() here, it wouldn't ever by called anyway because subclasses override it!
   //so, in the serialize methods of Just and Nothing, we need to inject an extra map.put!
}

object Just {
//    //generated by the classloader! (as a static method on Just!)
//    def deseralize(map: java.util.Map[String, AnyRef]): Just[_] = {
//        apply(map.get("value"))
//    }
}
@ConfigurationSerializable(scan = new Scan(Scan.Type.CASE_CLASS), as = "Just")
case class Just[+T](value: T) extends Maybe[T] {
//    //generated by the classloader (2nd pass, as serialize())
//    def serialize(): java.util.Map[String, AnyRef] = {
//        val map = new java.util.HashMap[String, AnyRef]()
//        map.putAll($serialize());
//        map.put("$variant", "Just" /*or the class name in case there was no alias*/)
//        map
//    }
//
//    //generated by the classloader! (1st pass, as serialize(), renamed in second pass to $serialize())
//    def $serialize(): java.util.Map[String, AnyRef] = {
//        val map = new java.util.HashMap[String, AnyRef]()
//        map.put("value", value.asInstanceOf[AnyRef])
//        map
//    }
}

@ConfigurationSerializable(scan = new Scan(Scan.Type.SINGLETON_OBJECT), as = "Nothing")
case object NoValue extends Maybe[scala.Nothing] {
//    //generated by the classloader (2nd pass, as serialize())
//    def serialize(): java.util.Map[String, AnyRef] = {
//        val map = new java.util.HashMap[String, AnyRef]()
//        map.putAll($serialize())
//        map.put("$variant", "Nothing" /*or or the class name in case there was no alias*/)
//        return map;
//    }
//
//    //generated by the classloader (but static)!
//    def deserialize(map: java.util.Map[String, AnyRef]): Nothing.type = {
//        Nothing
//    }
//
//    //generated by the classloader! (1st pass, as serialize(), renamed in second pass to serialize$()
//    def $serialize(): java.util.Map[String, AnyRef] = {
//        val map = new java.util.HashMap[String, AnyRef]()
//        map
//    }
}
