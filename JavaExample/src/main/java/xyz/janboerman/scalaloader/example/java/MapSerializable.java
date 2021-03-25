package xyz.janboerman.scalaloader.example.java;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import static xyz.janboerman.scalaloader.example.java.ExamplePlugin.assertionsEnabled;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@ConfigurationSerializable(as = "MapSerializable")
public class MapSerializable {

    // don't register, use RuntimeConversions!
    public enum HelloWorld {
        Yolo,
        Swag;
    }

    private final Map<HelloWorld, HelloWorld> map = new EnumMap<>(HelloWorld.class);

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MapSerializable that)) return false;

        return Objects.equals(this.map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.map);
    }


    public static void test(ExamplePlugin plugin) {
        //setup
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        File saveFile = new File(dataFolder, "map-serialization-test.yml");
        if (!saveFile.exists()) {
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        CommandSender console = plugin.getServer().getConsoleSender();
        console.sendMessage(ChatColor.YELLOW + "Test" + ChatColor.RESET + " mapSerializable.equals(deserialize(serialize(mapSerializable)))");
        MapSerializable mapSerializable = new MapSerializable();
        mapSerializable.map.putAll(Map.ofEntries(
                Map.entry(HelloWorld.Yolo, HelloWorld.Swag),
                Map.entry(HelloWorld.Swag, HelloWorld.Yolo))
        );

        YamlConfiguration config = new YamlConfiguration();
        config.set("mapSerializable", mapSerializable);
        try {
            config.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        config = YamlConfiguration.loadConfiguration(saveFile);
        assert mapSerializable.equals(config.get("mapSerializable"));
        if (assertionsEnabled()) {
            console.sendMessage(ChatColor.GREEN + "Test passed!");
        }
    }

}


