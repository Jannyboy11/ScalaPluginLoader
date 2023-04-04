package xyz.janboerman.scalaloader.compat;

import org.bukkit.Server;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;

import java.io.File;

/**
 * ClassLoader that loads {@link IScalaPlugin} instances.
 * Implementations of this interface must guarantee that they inherit from {@link java.lang.ClassLoader}.
 */
public interface IScalaPluginClassLoader {

    /**
     * Get the jar file of the ScalaPlugin.
     * @return the jar file
     */
    public File getPluginJarFile();

    /**
     * Get the name of the main class of the plugin.
     * @return the main class name
     */
    public String getMainClassName();

    /**
     * Get the bukkit api version.
     * @return the bukkit api version
     */
    public ApiVersion getApiVersion();

    /**
     * Gets or defines a class using this ClassLoader.
     * @param className the name of the class
     * @param classGenerator the generator of the class' bytecode
     * @param persist whether this class should be loaded again by the plugin after a server restart
     * @return the ClassDefineResult.
     */
    public ClassDefineResult getOrDefineClass(String className, ClassGenerator classGenerator, boolean persist);

    /**
     * Get the Server the associated plugin runs on.
     * @return the server
     */
    public Server getServer();

    /**
     * Get the ScalaPlugin instance loaded by this ClassLoader.
     * @apiNote can return null if this classloader is not yet initialised.
     * @return the ScalaPlugin
     */
    public IScalaPlugin getPlugin();

    /**
     * Get the PluginLoader that constructed this ClassLoader.
     * @return the PluginLoader.
     */
    public IScalaPluginLoader getPluginLoader();
}
