package xyz.janboerman.scalaloader;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

public final class ScalaLoader extends JavaPlugin {

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad() {
        //setup config
        configure();

        //setup scala plugins folder
        File scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }

        //load scala plugins
        //getServer().getPluginManager().registerInterface(ScalaPluginLoader.class);
        //getServer().getPluginManager().loadPlugins(scalaPluginsFolder);

        //TODO temporary! used for testing!
        ScalaPluginLoader pluginLoader = new ScalaPluginLoader(getServer());
        try {
            File scalaExampleFile = new File(scalaPluginsFolder, "ScalaExample-0.1-SNAPSHOT.jar");
            File javaExampleFile = new File(scalaPluginsFolder, "JavaExample-0.1-SNAPSHOT.jar");
            pluginLoader.getPluginDescription(scalaExampleFile);
            pluginLoader.getPluginDescription(javaExampleFile);
        } catch (InvalidDescriptionException e) {
            e.printStackTrace();
        }

        //re-register the previous PluginLoader for .jar files to the pluginmanager (which is likely a JavaPluginLoader)
        //this should avoid the ScalaPluginLoader trying to load JavaPlugins
        getServer().getPluginManager().registerInterface(getPluginLoader().getClass());
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    private void configure() {
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(PluginScalaVersion.class, "ScalaVersion");
        FileConfiguration config = getConfig();
        if (!config.isList("scala-versions")) {
            getConfig().set("scala-versions", Arrays.stream(ScalaVersion.values()).map(PluginScalaVersion::fromScalaVersion).collect(Collectors.toList()));
            saveConfig();
        }
    }

    public boolean downloadScalaJarFiles() {
        return getConfig().getBoolean("load-libraries-from-disk", false);
    }

    public boolean saveScalaVersionsToConfig(PluginScalaVersion... versions) {
        FileConfiguration config = getConfig();
        Set<PluginScalaVersion> scalaVersions = new LinkedHashSet<>(Arrays.asList(versions));
        boolean wasAdded = scalaVersions.addAll((List<PluginScalaVersion>) config.getList("scala-versions", Collections.emptyList()));
        config.set("scala-versions", new ArrayList<>(scalaVersions));
        saveConfig();
        return wasAdded;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "resetScalaUrl":
                if (args.length == 0) return false;
                String version = args[0];
                ScalaVersion scalaVersion;
                if ("all".equals(version)) {
                    saveScalaVersionsToConfig(Arrays.stream(ScalaVersion.values())
                            .map(PluginScalaVersion::fromScalaVersion)
                            .toArray(PluginScalaVersion[]::new));
                    sender.sendMessage(ChatColor.GREEN + "All URLs for built-in scala versions were reset!");
                } else if ((scalaVersion = ScalaVersion.fromVersionString(version)) != null) {
                    saveScalaVersionsToConfig(PluginScalaVersion.fromScalaVersion(scalaVersion));
                    sender.sendMessage(ChatColor.GREEN + "URLs for scala version " + scalaVersion.getVersion() + " were reset.");
                } else {
                    sender.sendMessage(new String[] {
                            ChatColor.RED + "Unrecognized scala version: " + version + ".",
                            ChatColor.RED + "Please use one of the following: " + onTabComplete(sender, command, label, new String[0]) + "."
                    });
                }

                return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "resetScalaUrl":
                if (args.length == 0) {
                    LinkedList<String> scalaVersions = Arrays.stream(ScalaVersion.values())
                            .map(ScalaVersion::getVersion)
                            .collect(Collectors.toCollection(LinkedList::new));

                    scalaVersions.addFirst("all");
                    return scalaVersions;
                }

                return Collections.emptyList();
        }

        return null;
    }

}
