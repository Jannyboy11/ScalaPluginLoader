package xyz.janboerman.scalaloader.example.java;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import scala.Option;
import scala.Some;
import xyz.janboerman.scalaloader.example.scala.ExamplePlugin$;
import xyz.janboerman.scalaloader.example.scala.Home;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.description.Api;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.description.Version;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

@CustomScala(@Version(value = "2.13.0",
        scalaLibraryUrl = "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.13.0%2Fscala-reflect-2.13.0.jar",
        scalaReflectUrl = "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.13.0%2Fscala-library-2.13.0.jar"))
@Api(ApiVersion.v1_16)
public class ExamplePlugin extends ScalaPlugin {

    private final Random random = new Random();

    public ExamplePlugin() {
        super(new ScalaPluginDescription("JavaExample", "0.14.3-SNAPSHOT").addHardDepend("ScalaExample"));
    }

    public static ExamplePlugin getInstance() {
        return ScalaPlugin.getPlugin(ExamplePlugin.class);
    }

    @Override
    public void onEnable() {
        Option<String> some = new Some<>("Hello, World!");
        Option<String> none = Option.apply(null);

        getLogger().info("Some = " + some);
        getLogger().info("None = " + none);

        PluginDescriptionFile pluginDescriptionFile = getDescription();
        getLogger().info("commands from PluginDescriptionFile = " + pluginDescriptionFile.getCommands());
        getLogger().info("permissions from PluginDescriptionFile = " + pluginDescriptionFile.getPermissions().stream().map(Permission::getName).collect(Collectors.toList()));

        //IntelliJ is too dumb to understand ScalaExample's automatic module, so it complains ExamplePlugin$ is not visible, but actually it is.
        getLogger().info("Got " + ExamplePlugin$.MODULE$.getInt() + " from the Scala example plugin :)");

        //this works because 2.13.0 is binary compatible with 2.13.5 (and we are experiencing the same IntelliJ bug again!)
        Home home = Home.apply(UUID.randomUUID(), "home", getServer().getWorlds().get(0).getSpawnLocation());

        new Money(this).test();                 //checks whether basic bytecode transformations worked correctly
        new ArraySerializationTest(this).test();    //checks whether arrays get converted properly into lists and back
        new ListSerializationTest(this).test();     //check whether list elements get converted properly
        Maybe.test(this);                           //checks whether @DelegateSerializtion works correctly
        MapSerializable.test(this);                 //checks whether map elements get converted properly
        //testDeserializedTypes();    //check which types we get back after one round trip of live->serialized->live
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Executed bar command!");

        //flip a coin
        int randomInt = random.nextInt(2);
        var message = switch(randomInt) {
            case 0 -> "You got heads!";
            case 1 -> "You got tails!";
            default -> throw new RuntimeException("java.util.Random is broken if this occurs");
        };
        sender.sendMessage(message);

        //verify that permissions actually worked
        var permissions = getDescription().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toList());
        sender.sendMessage("Permissions = " + permissions);

        return true;
    }

    private void testDeserializedTypes() {
        getLogger().info("debugging automatically (de)serializable classes...");

        ConfigurationSerialization.registerClass(MultiBox.class);

        File configFile = new File(getDataFolder(), "largebox-deserialization-test.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("multibox", new MultiBox());
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "error saving config deserialization test config file", e);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        MultiBox mb = (MultiBox) config.get("multibox");
    }

    static boolean assertionsEnabled() {
        try {
            assert false;
            return false;
        } catch (AssertionError ae) {
            return true;
        }
    }
}
