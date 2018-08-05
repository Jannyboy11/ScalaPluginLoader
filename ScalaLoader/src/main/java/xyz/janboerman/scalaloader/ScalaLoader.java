package xyz.janboerman.scalaloader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.scala.ScalaVersion;

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
            config.set("scala-versions", Arrays.stream(ScalaVersion.values()).map(PluginScalaVersion::fromScalaVersion).collect(Collectors.toList()));
        }
    }

    public boolean downloadScalaJarFiles() {
        return getConfig().getBoolean("load-libraries-from-disk", false);
    }

    public boolean saveScalaVersionToConfig(PluginScalaVersion scalaVersion) {
        FileConfiguration config = getConfig();
        Set<PluginScalaVersion> scalaVersions = new LinkedHashSet<>((List<PluginScalaVersion>) config.getList("scala-versions", Collections.emptyList()));
        boolean wasAdded = scalaVersions.add(scalaVersion);
        config.set("scala-versions", new ArrayList<>(scalaVersions));
        saveConfig();
        return wasAdded;
    }

}
