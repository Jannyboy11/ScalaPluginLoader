package xyz.janboerman.scalaloader.compat;

import org.bukkit.plugin.Plugin;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.event.EventBus;

import java.io.File;

/**
 * ScalaPlugin abstraction. Not meant to be implemented directly.
 * <p>
 * When you implement your own ScalaPlugin, please inherit from xyz.janboerman.scalaloader.plugin.ScalaPlugin.
 * Alternatively, if your plugin is only meant to run on Paper, you can inherit from xyz.janboerman.scalaloader.paper.plugin.ScalaPlugin instead`.
 */
public interface IScalaPlugin extends Plugin {

    /**
     * Get the configuration file of this ScalaPlugin.
     * @return the configuration file
     */
    public File getConfigFile();

    /**
     * Get the Server's event bus.
     * @return the event bus
     */
    public EventBus getEventBus();

    /**
     * Get the version of Scala used by this plugin.
     * @return the scala version
     */
    public String getScalaVersion();

    /**
     * Get the Scala Release cycle associated with this plugin
     * @return the scala release
     */
    public default ScalaRelease getScalaRelease() {
        return ScalaRelease.fromScalaVersion(getScalaVersion());
    }

    /**
     * Get the version of Scala declared by this plugin.
     * @return the declared scala version
     */
    public String getDeclaredScalaVersion();

    /**
     * Get the name of the ScalaPlugin.
     * @return this plugin's name
     */
    public String getName();

    /**
     * Get the {@link ClassLoader} of this plugin.
     * @return the class loader
     */
    @Called
    public default IScalaPluginClassLoader classLoader() {
        return (IScalaPluginClassLoader) getClass().getClassLoader();
    }

    /**
     * Get the {@link org.bukkit.plugin.PluginLoader} of this plugin.
     * @return the plugin loader
     */
    public default IScalaPluginLoader pluginLoader() {
        return classLoader().getPluginLoader();
    }

    /**
     * Get the prefix of this plugin, used for logging.
     * @return the prefix
     */
    public String getPrefix();

}
