package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.jetbrains.annotations.NotNull;

public class ScalaPluginLoader implements PluginLoader {
    
    //TODO I am not really sure why I should need this class.

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        //TODO is this where we want to do the scanning? where are we called?

    }

    //TODO much like the xyz.janboerman.scalaloader.plugin.ScalaPluginLoader,
    //TODO this class is responsible for scanning the jar file for classes, collecting ScanResults,
    //TODO and detecting the main class (in case a plugin.yml or paper-plugin.yml is absent)
    //TODO additionally, the scala standard library must be appended to the plugin's classpath.

}
