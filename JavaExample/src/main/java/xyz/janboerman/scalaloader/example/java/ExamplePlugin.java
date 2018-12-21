package xyz.janboerman.scalaloader.example.java;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginDescriptionFile;
import scala.Option;
import scala.Some;
import xyz.janboerman.scalaloader.example.scala.ExamplePlugin$;
import xyz.janboerman.scalaloader.example.scala.Home;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.description.Version;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CustomScala(@Version(value = "2.12.6",
        scalaLibraryUrl = "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.12.6%2Fscala-reflect-2.12.6.jar",
        scalaReflectUrl = "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.12.6%2Fscala-library-2.12.6.jar"))
public class ExamplePlugin extends ScalaPlugin {

    public ExamplePlugin() {
        super(new ScalaPluginDescription("JavaExample", "0.9-SNAPSHOT").addHardDepend("ScalaExample"));
    }

    @Override
    public void onEnable() {
        Option<String> some = new Some<>("Hello, World!");
        Option<String> none = Option.apply(null);

        getServer().broadcastMessage("Some = " + some);
        getServer().broadcastMessage("None = " + none);

        PluginDescriptionFile pluginDescriptionFile = getDescription();
        getLogger().info("commands from PluginDescriptionFile = " + pluginDescriptionFile.getCommands());
        getLogger().info("permissions from PluginDescriptionFile = " + pluginDescriptionFile.getPermissions().stream().map(Permission::getName).collect(Collectors.toList()));

        getLogger().info("Got " + ExamplePlugin$.MODULE$.getInt() + " from the Scala example plugin :)");

        //this works because 2.12.6 is binary compatible with 2.12.7
        Home home = Home.apply(UUID.randomUUID(), "home", getServer().getWorlds().get(0).getSpawnLocation());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Executed bar command!");

        //verify that permissions actually worked
        List<Permission> permissions = getDescription().getPermissions();
        sender.sendMessage("Permissions = " + permissions);

        return true;
    }

}
