package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.jar.JarFile;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.entrypoint.classloader.group.PaperPluginClassLoaderStorage;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

public class ScalaPluginClassLoader extends PaperPluginClassLoader implements IScalaPluginClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final File pluginJarFile;
    private final JarFile jarFile;
    private final String mainClassName;

    public ScalaPluginClassLoader(Logger logger,
                                  Path source,
                                  File pluginJarFile,
                                  PaperPluginMeta configuration,
                                  ClassLoader parent,
                                  URLClassLoader libraryLoader,

                                  String mainClassName) throws IOException, ScalaPluginLoaderException {
        super(logger, source, Compat.jarFile(pluginJarFile), configuration, parent, libraryLoader);

        //TODO this is what PaperPluginClassLaoder does if the PaperPluginmeta has an open classloader.
        //TODO so we can omit this call if we accept a PaperPluginMeta instead of ScalaPluginMeta (or we make ScalaPluginmeta a subclass of PaperPluginMeta)
        //PaperPluginClassLoaderStorage.instance().registerOpenGroup(this);

        this.pluginJarFile = pluginJarFile;
        this.jarFile = Compat.jarFile(pluginJarFile);
        this.mainClassName = mainClassName;

        try {
            Class<? extends ScalaPlugin> mainClass = (Class<? extends ScalaPlugin>) Class.forName(mainClassName, true, this);
            ScalaLoaderUtils.createScalaPluginInstance(mainClass); //sets the loadedPlugin

        } catch (ClassNotFoundException e) {
            throw new ScalaPluginLoaderException("Could not find plugin's main class: " + mainClassName, e);
        }

        //TODO instantiate plugin!
        
    }

    public File getPluginJarFile() {
        return pluginJarFile;
    }

    @Override
    public String getMainClassName() {
        return mainClassName;
    }

    @Override
    public ApiVersion getApiVersion() {
        //TODO dependency-inject
        return null;
    }

    @Override
    public ClassDefineResult getOrDefineClass(String className, ClassGenerator classGenerator, boolean persist) {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Server getServer() {
        //TODO dependency-inject
        return null;
    }

    //TODO
    public String getScalaVersion() {
        //TODO dependency-inject
        return null;
    }

    //TODO
    public ScalaRelease getScalaRelease() {
        //TODO dependency-inject (or compute from getScalaVersion())
        return null;
    }

    @Override
    public ScalaPlugin getPlugin() {
        return (ScalaPlugin) super.getPlugin();
    }

    @Override
    public ScalaPluginLoader getPluginLoader() {
        //TODO dependency-inject
        return null;
    }

    //TODO loadClass

    //TODO findClass

    //TODO take bytecode transformations into account

    @Override
    public void init(JavaPlugin plugin) {
        assert plugin instanceof ScalaPlugin : "Used ScalaPluginClassLoader to initialise a plugin that is not a ScalaPlugin: " + plugin;

        // overriding this method just us the ability to do it *during* instantiation,
        // meaning that the bodies of ScalaPlugin subclasses' constructors are experiencing a fully initialised ScalaPlugin.
        super.init(plugin);

        //TODO anything else to do here? perhaps we want to 'set' the ScalaPluginDescription or something?

    }

}
