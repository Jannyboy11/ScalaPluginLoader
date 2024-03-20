package xyz.janboerman.scalaloader.example.java;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
//import org.bukkit.configuration.serialization.ConfigurationSerializable;
//import org.bukkit.configuration.serialization.ConfigurationSerialization;
//import org.bukkit.configuration.serialization.SerializableAs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import static xyz.janboerman.scalaloader.example.java.ExamplePlugin.assertionsEnabled;

class ListSerializationTest {

    private final File saveFile;
    private final Logger logger;
    private final ExamplePlugin plugin;

    ListSerializationTest(ExamplePlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        saveFile = new File(dataFolder, "list-serialization-test.yml");
        if (!saveFile.exists()) {
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.logger = plugin.getLogger();
    }

    void test() {
        plugin.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "Test " + ChatColor.RESET + "deserialize(serialize(listserializable)).equals(listserializable)");
        //ConfigurationSerialization.registerClass(ListSerializable.class, "ListSerializable");

        ListSerializable listSerializable = new ListSerializable(List.of(4L, 5L));
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("listserializable", listSerializable);

        try {
            yamlConfiguration.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        yamlConfiguration = YamlConfiguration.loadConfiguration(saveFile);
        assert listSerializable.equals(yamlConfiguration.get("listserializable")) : "original listserializable does not equal deserialized listserializable";
        if (assertionsEnabled()) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Test passed!");
        }
    }
}

//@SerializableAs("ListSerializable")
@ConfigurationSerializable(as = "ListSerializable")
public class ListSerializable /*implements ConfigurationSerializable*/ {

    private final List<Long> longs;
    private final List<List<Float>[]> listOfArrayOfListOfFloat = List.of();

    ListSerializable(List<Long> longs) {
        this.longs = longs;
    }

    //generated!
//    @Override
//    public Map<String, Object> serialize() {
//        ...
//    }

    //generated!
//    public static ListSerializable deserialize(Map<String, Object> map) {
//        ...
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListSerializable that)) return false;

        return Objects.equals(longs, that.longs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(longs);
    }
}
