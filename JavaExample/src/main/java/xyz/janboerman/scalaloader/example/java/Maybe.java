package xyz.janboerman.scalaloader.example.java;


import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import xyz.janboerman.scalaloader.configurationserializable.DelegateSerialization;
import static xyz.janboerman.scalaloader.example.java.ExamplePlugin.assertionsEnabled;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

@DelegateSerialization
public sealed interface Maybe<T> permits Maybe.Just, Maybe.Nothing {

    public boolean isPresent();
    public T get();

    public static <T> Maybe<T> just(T value) { return new Just<>(value); }
    public static <T> Maybe<T> nothing() { return Nothing.getInstance(); }

    public static void test(ExamplePlugin plugin) {
        //setup
        File dataFolder = plugin.getDataFolder();
        dataFolder.mkdirs();
        File saveFile = new File(dataFolder, "maybe-serialization-test.yml");
        if (!saveFile.exists()) {
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ConfigurationSerialization.registerClass(Nothing.class);
        CommandSender console = plugin.getServer().getConsoleSender();
        Maybe<Integer> maybe;
        YamlConfiguration config = new YamlConfiguration();

        //test just
        console.sendMessage(ChatColor.YELLOW + "Test" + ChatColor.RESET + " just(1).equals(deserialize(serialize(just(1))))");
        maybe = Maybe.just(1);
        config.set("justOne", maybe);
        try {
            config.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = YamlConfiguration.loadConfiguration(saveFile);
        assert maybe.equals(config.get("justOne"));
        if (assertionsEnabled()) {
            console.sendMessage(ChatColor.GREEN + "Test passed!");
        }

        //test nothing
        console.sendMessage(ChatColor.YELLOW + "Test" + ChatColor.RESET + " nothing.equals(deserialize(serialize(nothing)))");
        maybe = Maybe.nothing();
        config.set("nothing", maybe);
        try {
            config.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = YamlConfiguration.loadConfiguration(saveFile);
        assert maybe.equals(config.get("nothing"));
        if (assertionsEnabled()) {
            console.sendMessage(ChatColor.GREEN + "Test passed!");
        }

        //test just != nothing
        console.sendMessage(ChatColor.YELLOW + "Test"+ ChatColor.RESET + " !just(1).equals(nothing)");
        assert !config.get("justOne").equals(config.get("nothing"));
        if (assertionsEnabled()) {
            console.sendMessage(ChatColor.GREEN + "Test passed!");
        }
    }


    @ConfigurationSerializable
    public static record Just<T>(T value) implements Maybe<T> {

        @Override public boolean isPresent() { return true; }
        @Override public T get() { return value; }

        //already has equals, hashCode and toString! :)

    }

    //implement ConfigurationSerializable explicitly because Scan.Type.SINGLETON_OBJECT only works for Scala objects!
    //I might add more scan types in the future, but I am not going to care for now.
    //the whole point of using ScalaLoader is that you are writing Scala anyway!
    public static final class Nothing<T> implements Maybe<T>, org.bukkit.configuration.serialization.ConfigurationSerializable {

        private static final Nothing<?> INSTANCE = new Nothing<>();

        private Nothing() {}

        public static <T> Nothing<T> getInstance() {
            return (Nothing<T>) INSTANCE;
        }

        @Override public boolean isPresent() { return false; }
        @Override public T get() { throw new NoSuchElementException("Nothing"); }

        @Override
        public Map<String, Object> serialize() {
            return Map.of();
        }

        public static <T> Nothing<T> deserialize(Map<String, Object> map) {
            return getInstance();
        }

        //no need to implement equals and hashCode because there is only one instance ever
        //and so using the identity equality from java.lang.Object is just fine!

        @Override
        public String toString() {
            return "Nothing";
        }
    }
}


