package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.logging.Logger;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.entrypoint.classloader.group.PaperPluginClassLoaderStorage;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

public class ScalaPluginClassLoader extends PaperPluginClassLoader implements IScalaPluginClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final File pluginJarFile;
    private final ScalaPlugin plugin;

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

        try {
            Class<? extends ScalaPlugin> mainClass = (Class<? extends ScalaPlugin>) Class.forName(mainClassName, true, this);
            this.plugin = ScalaLoaderUtils.createScalaPluginInstance(mainClass);

        } catch (ClassNotFoundException e) {
            throw new ScalaPluginLoaderException("Could not find plugin's main class: " + mainClassName, e);
        }

        //TODO instantiate plugin!
        
    }

    public File getPluginJarFile() {
        return pluginJarFile;
    }

    //TODO loadClass

    //TODO findClass

    //TODO take bytecode transformations into account

    @Override
    public void init(JavaPlugin plugin) {
        assert plugin instanceof ScalaPlugin;

        // overriding this method just us the ability to do it *during* instantiation,
        // meaning that the bodies of ScalaPlugin subclasses' constructors are experiencing a fully initialised ScalaPlugin.
        super.init(plugin);

        //TODO anything else to do here? perhaps we want to 'set' the ScalaPluginDescription or something?

    }

    @Override
    public ScalaPlugin getPlugin() {
        return (ScalaPlugin) super.getPlugin();
    }

}
