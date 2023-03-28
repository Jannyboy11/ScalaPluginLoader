package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.nio.file.Path;

public class ScalaPluginProviderContext implements PluginProviderContext {

    private final ScalaPluginMeta configuration;
    private ScalaPluginClassLoader pluginClassLoader;

    public ScalaPluginProviderContext(ScalaPluginDescription description) {
        this.configuration = new ScalaPluginMeta(description);
    }

    @Override
    public @NotNull ScalaPluginMeta getConfiguration() {
        return configuration;
    }

    @Override
    public @NotNull Path getDataDirectory() {
        return ScalaLoader.getInstance().getScalaPluginsFolder().toPath().resolve(getConfiguration().getName());
    }

    @Override
    public @NotNull ComponentLogger getLogger() {
        return ComponentLogger.logger(getConfiguration().getMainClass());
    }

    public void setPluginClassLoader(ScalaPluginClassLoader pluginClassLoader) {
        if (this.pluginClassLoader != null)
            throw new IllegalStateException("pluginClassLoader already set");

        this.pluginClassLoader = pluginClassLoader;
    }

    public ScalaPluginClassLoader getPluginClassLoader() {
        if (this.pluginClassLoader == null)
            throw new IllegalStateException("pluginClassLoader not yet set");

        return pluginClassLoader;
    }

}
