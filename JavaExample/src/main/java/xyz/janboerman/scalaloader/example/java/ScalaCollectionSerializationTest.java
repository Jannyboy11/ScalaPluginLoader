package xyz.janboerman.scalaloader.example.java;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import scala.collection.immutable.*;
import scala.reflect.ClassTag$;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import static xyz.janboerman.scalaloader.example.java.ExamplePlugin.assertionsEnabled;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public class ScalaCollectionSerializationTest {

    private final ExamplePlugin plugin;
    private final File saveFile;

    ScalaCollectionSerializationTest(ExamplePlugin plugin) {
        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), "basic-scala-collections-test.yml");
        try {
            if (!saveFile.exists()) saveFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not create basic-scala-collections-test.yml save file.", e);
        }
    }

    void test() {
        basicTest();
    }

    private void basicTest() {
        final var console = plugin.getServer().getConsoleSender();
        console.sendMessage(ChatColor.YELLOW + "Basic Scalacollection serialization-deserialization test");

        final var intBuilder = Seq$.MODULE$.newBuilder();
        intBuilder.addOne(0).addOne(1).addOne(2);
        final Seq<Integer> integers = (Seq<Integer>) intBuilder.result();

        final var boolBuilder = Set$.MODULE$.newBuilder();
        boolBuilder.addOne(false).addOne(true);
        final Set<Boolean> booleans = (Set<Boolean>) (Set) boolBuilder.result();

        final var doubleBuilder = IndexedSeq$.MODULE$.newBuilder();
        doubleBuilder.addOne(3.0D).addOne(4.0D);
        final IndexedSeq<Double> doubles = (IndexedSeq<Double>) doubleBuilder.result();

        final var stringBuilder = ArraySeq$.MODULE$.newBuilder(ClassTag$.MODULE$.apply(String.class));
        stringBuilder.addOne("Hello, ").addOne("World!");
        final ArraySeq<String> strings = (ArraySeq<String>) (ArraySeq) stringBuilder.result();

        final var testCase = new BasicScalaCollectionTest(integers, booleans, doubles, strings);

        var yamlConfig = YamlConfiguration.loadConfiguration(saveFile);
        yamlConfig.set("basic-test", testCase);
        try {
            yamlConfig.save(saveFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not save basic-scala-collections-test.yml save file.", e);
        }

        yamlConfig = YamlConfiguration.loadConfiguration(saveFile);
        final var actual = yamlConfig.get("basic-test");
        assert testCase.equals(actual) : "BasicTest instances were not equal! :(";
        if (assertionsEnabled()) {
            console.sendMessage(ChatColor.GREEN + "Test passed!");
        }

    }

}

@ConfigurationSerializable
class BasicScalaCollectionTest {

    private final Seq<Integer> integers;
    private final Set<Boolean> booleans;
    private final IndexedSeq<Double> doubles;
    private final ArraySeq<String> strings;

    public BasicScalaCollectionTest(Seq<Integer> integers,
                                    Set<Boolean> booleans,
                                    IndexedSeq<Double> doubles,
                                    ArraySeq<String> strings) {
        this.integers = integers;
        this.booleans = booleans;
        this.doubles = doubles;
        this.strings = strings;
    }

    @Override
    public int hashCode() {
        return Objects.hash(integers, booleans, doubles, strings);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BasicScalaCollectionTest that
                && Objects.equals(this.integers, that.integers)
                && Objects.equals(this.booleans, that.booleans)
                && Objects.equals(this.doubles, that.doubles)
                && Objects.equals(this.strings, that.strings);
    }
}
