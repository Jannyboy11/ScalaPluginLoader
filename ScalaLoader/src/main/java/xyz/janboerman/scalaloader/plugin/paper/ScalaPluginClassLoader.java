package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.logging.Logger;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;

public class ScalaPluginClassLoader extends PaperPluginClassLoader implements IScalaPluginClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final File pluginJarFile;

    public ScalaPluginClassLoader(Logger logger,
                                  Path source,
                                  File pluginJarFile,
                                  PaperPluginMeta configuration,
                                  ClassLoader parent,
                                  URLClassLoader libraryLoader) throws IOException {
        super(logger, source, Compat.jarFile(pluginJarFile), configuration, parent, libraryLoader);

        this.pluginJarFile = pluginJarFile;
    }

    public File getPluginJarFile() {
        return pluginJarFile;
    }

    //TODO loadClass

    //TODO findClass

    //TODO take bytecode transformations into account

    //init(JavaPlugin plugin) called by JavaPlugin no-arg constructor!
    //do I want to override that method (and call super)? probably not I think.. what would that give us?




}
