package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import java.util.jar.JarFile;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;

public class ScalaPluginClassLoader extends PaperPluginClassLoader implements IScalaPluginClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final ScalaPluginLoader pluginLoader;
    private final File pluginJarFile;
    private final JarFile jarFile;
    private final String mainClassName;
    private final Map<String, Object> pluginYaml;

    public ScalaPluginClassLoader(Logger logger,
                                  File pluginJarFile,
                                  ScalaPluginMeta configuration,
                                  ClassLoader parent,
                                  URLClassLoader libraryLoader,

                                  ScalaPluginLoader pluginLoader,
                                  Map<String, Object> pluginYaml) throws IOException {
        super(logger, pluginJarFile.toPath(), Compat.jarFile(pluginJarFile), configuration, parent, libraryLoader);

        this.pluginJarFile = pluginJarFile;
        this.jarFile = Compat.jarFile(pluginJarFile);
        this.mainClassName = configuration.getMainClass();
        this.pluginLoader = pluginLoader;
        this.pluginYaml = pluginYaml;
    }

    @Override
    public void init(JavaPlugin plugin) {
        assert plugin instanceof ScalaPlugin : "Used ScalaPluginClassLoader to initialise a plugin that is not a ScalaPlugin: " + plugin;
        // overriding this method just us the ability to do it *during* instantiation,
        // meaning that the bodies of ScalaPlugin subclasses' constructors are experiencing a fully initialised ScalaPlugin.
        super.init(plugin);
    }

    public File getPluginJarFile() {
        return pluginJarFile;
    }

    @Override
    public ScalaPluginMeta getConfiguration() {
        return (ScalaPluginMeta) super.getConfiguration();
    }

    @Override
    public String getMainClassName() {
        return mainClassName;
    }

    @Override
    public ApiVersion getApiVersion() {
        return ApiVersion.byVersion(getConfiguration().getAPIVersion());
    }

    public Map<String, Object> getExtraPluginYaml() {
        return Collections.unmodifiableMap(pluginYaml);
    }

    @Override
    public Server getServer() {
        return ScalaLoader.getInstance().getServer();
    }

    public String getScalaVersion() {
        return getConfiguration().getScalaVersion();
    }

    @Override
    public ScalaPlugin getPlugin() {
        return (ScalaPlugin) super.getPlugin();
    }

    @Override
    public ScalaPluginLoader getPluginLoader() {
        return pluginLoader;
    }

    //TODO loadClass

    //TODO findClass

    //TODO take bytecode transformations into account

    @Override
    public ClassDefineResult getOrDefineClass(String className, ClassGenerator classGenerator, boolean persist) {
        //TODO
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
