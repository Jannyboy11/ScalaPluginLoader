package xyz.janboerman.scalaloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.plugin.java.JavaPluginLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

public final class ScalaLoader extends JavaPlugin {

    private final boolean iActuallyManagedToOverrideTheDefaultJavaPluginLoader;
    private File scalaPluginsFolder;
    private JavaPluginLoader weFoundTheJavaPluginLoader;
    private Map<Pattern, PluginLoader> pluginLoaderMap;

    public ScalaLoader() {
        //dirty hack to override the previous pattern.

        boolean myHackWorked; //try to get hold of the pattern. k thnx

        Server server = Bukkit.getServer();
        try {
            SimplePluginManager pluginManager = (SimplePluginManager) server.getPluginManager();
            Field fileAssociationsField = pluginManager.getClass().getDeclaredField("fileAssociations");
            fileAssociationsField.setAccessible(true);
            pluginLoaderMap = (Map) fileAssociationsField.get(pluginManager);
            Iterator<Map.Entry<Pattern, PluginLoader>> iterator = pluginLoaderMap.entrySet().iterator();

            ScalaPluginLoader scalaPluginLoader = new ScalaPluginLoader(server);

            while (iterator.hasNext()) {
                Map.Entry<Pattern, PluginLoader> entry = iterator.next();
                if (entry.getValue() instanceof JavaPluginLoader) {
                    weFoundTheJavaPluginLoader = (JavaPluginLoader) entry.getValue();
                    entry.setValue(scalaPluginLoader);
                }
            }

            myHackWorked = true;
        } catch (Throwable iGiveUp) {
            myHackWorked = false;
        }

        iActuallyManagedToOverrideTheDefaultJavaPluginLoader = myHackWorked;

    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad() {
        //setup config
        configure();

        //setup scala plugins folder
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }

        //try to load scala plugins in the same plugin load phase as java plugins
        if (iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            getServer().getPluginManager().loadPlugins(scalaPluginsFolder);

            //re-register the JavaPluginLoader again.
            //this should avoid the ScalaPluginLoader trying to load JavaPlugins
            for (Pattern pattern : weFoundTheJavaPluginLoader.getPluginFileFilters()) {
                pluginLoaderMap.put(pattern, weFoundTheJavaPluginLoader);
            }
        } else {
            //couldn't replace the JavaPluginLoader - just register it here.
            getServer().getPluginManager().registerInterface(ScalaPluginLoader.class);
        }
    }

    @Override
    public void onEnable() {
        if (!iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            //if the injection didn't work, load scala plugins in onEnable.
            //this violates the JavaDocs of Plugin#onLoad, but we have no other option sadly.
            //Plugin#onLoad states that onLoad of all plugins is called before onEnable is called of any other plugin.

            Plugin[] plugins = getServer().getPluginManager().loadPlugins(scalaPluginsFolder);
            //now while we are at it, let's enable them too.
            for (Plugin plugin : plugins) {
                getServer().getPluginManager().enablePlugin(plugin);
            }
        }
    }

    @Override
    public void onDisable() {
        //Do we want to disable the scala plugins? I don't think so
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
