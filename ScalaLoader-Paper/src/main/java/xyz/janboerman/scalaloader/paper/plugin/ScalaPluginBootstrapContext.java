package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.io.File;

public class ScalaPluginBootstrapContext extends ScalaPluginProviderContext implements BootstrapContext {
    public ScalaPluginBootstrapContext(File pluginJarFile, ScalaPluginDescription description) {
        super(pluginJarFile, description);
    }
}
