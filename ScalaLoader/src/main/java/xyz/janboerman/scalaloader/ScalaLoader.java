package xyz.janboerman.scalaloader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.version.ScalaVersion;

public final class ScalaLoader extends JavaPlugin {

    private final Map<String, Plugin> pluginsByScalaVersion = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public ScalaLoader() {
        getServer().getPluginManager().registerInterface(ScalaPluginLoader.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad() {
        configure();

        // TODO save all versions to the default config.

        // TODO load scala plugins (load multiple instances
        // TODO load scala standard library (download if necessary)
        // TODO check if it exists already.
        // TODO use ASM to relocate all scala standard (and reflection) library classes

    }

    @Override
    public void onEnable() {
        // TODO enable all scala plugins
    }

    @Override
    public void onDisable() {
        //TODO disable all scala plugins
    }

    private void configure() {
        ConfigurationSerialization.registerClass(PluginScalaVersion.class, "ScalaVersion");
        FileConfiguration config = getConfig();
        if (!config.isList("scala-versions")) {
            config.set("scala-versions", Arrays.stream(ScalaVersion.values()).map(PluginScalaVersion::fromScalaVersion).collect(Collectors.toList()));
        }
    }

    public void saveScalaVersionToConfig(PluginScalaVersion scalaVersion) {
        FileConfiguration config = getConfig();
        List<PluginScalaVersion> scalaVersions = new ArrayList<>((List<PluginScalaVersion>) config.getList("scala-versions", Collections.emptyList()));
        scalaVersions.add(scalaVersion);
        config.set("scala-versions", scalaVersions);
        saveConfig();
    }

}
