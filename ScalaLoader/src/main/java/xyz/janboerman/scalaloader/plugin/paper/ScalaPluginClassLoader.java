package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;

public class ScalaPluginClassLoader extends PaperPluginClassLoader {

    static {
        registerAsParallelCapable();
    }

    public ScalaPluginClassLoader(Logger logger,
                                  Path source,
                                  JarFile file,
                                  PaperPluginMeta configuration,
                                  ClassLoader parentLoader,
                                  URLClassLoader libraryLoader) throws IOException {
        super(logger, source, file, configuration, parentLoader, libraryLoader);
    }

    public File getPluginJarFile() {
        //TODO
        return null;
    }

    //TODO loadClass

    //TODO findClass

    //TODO take bytecode transformations into account

}
