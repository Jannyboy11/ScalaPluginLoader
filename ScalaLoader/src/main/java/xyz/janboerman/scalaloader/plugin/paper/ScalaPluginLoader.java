package xyz.janboerman.scalaloader.plugin.paper;

import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
import xyz.janboerman.scalaloader.event.EventBus;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;

public class ScalaPluginLoader implements PluginLoader, IScalaPluginLoader {

    private static final ScalaPluginLoader INSTANCE = new ScalaPluginLoader();

    private ScalaLoader scalaLoader;
    private EventBus eventBus;

    public static ScalaPluginLoader getInstance() {
        return INSTANCE;
    }
    
    //TODO I am not really sure why I should need this class.

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        //TODO is this where we want to do the scanning? where are we called?

    }

    //TODO much like the xyz.janboerman.scalaloader.plugin.ScalaPluginLoader,
    //TODO this class is responsible for scanning the jar file for classes, collecting ScanResults,
    //TODO and detecting the main class (in case a plugin.yml or paper-plugin.yml is absent)
    //TODO additionally, the scala standard library must be appended to the plugin's classpath.


    ScalaLoader getScalaLoader() {
        return scalaLoader == null ? scalaLoader = JavaPlugin.getPlugin(ScalaLoader.class) : scalaLoader;
    }


    public EventBus getEventBus() {
        return eventBus == null ? eventBus = new EventBus(Bukkit.getPluginManager()) : eventBus;
    }



}
