package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.library.ClassPathLibrary;
import io.papermc.paper.plugin.loader.library.PaperLibraryStore;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.dependency.LibraryClassLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ScalaPluginClasspathBuilder implements PluginClasspathBuilder {

    private final List<ClassPathLibrary> libraries = new ArrayList<>();
    private final ScalaPluginProviderContext context;

    public ScalaPluginClasspathBuilder(ScalaPluginProviderContext context) {
        this.context = context;
    }

    @Override
    public ScalaPluginProviderContext getContext() {
        return context;
    }

    @Override
    public ScalaPluginClasspathBuilder addLibrary(ClassPathLibrary library) {
        this.libraries.add(library);
        return this;
    }

    public ScalaPluginClassLoader buildClassLoader(Logger logger, ClassLoader parent, File pluginJarFile, TransformerRegistry transformerRegistry, ScalaPluginLoader pluginLoader, Map<String, Object> pluginYaml) throws IOException {
        //create library classloader
        var libraryStore = new PaperLibraryStore();
        for (var library : this.libraries) {
            library.register(libraryStore);
        }
        File[] libraryFiles = libraryStore.getPaths().stream().map(Path::toFile).toArray(File[]::new);
        LibraryClassLoader libraryClassLoader = new LibraryClassLoader(libraryFiles, parent, logger, null/*initialised via .setPlugin(..)*/, transformerRegistry);

        //create plugin classloader
        ScalaPluginClassLoader pluginClassLoader = new ScalaPluginClassLoader(logger, pluginJarFile, context.getConfiguration(), parent, libraryClassLoader, pluginLoader, pluginYaml, transformerRegistry, context.getDataDirectory());
        libraryClassLoader.setPlugin(pluginClassLoader); //important!!
        return pluginClassLoader;
    }

}
