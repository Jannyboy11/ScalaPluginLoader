package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.PluginInitializerManager;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.nio.file.Path;

public class ScalaPluginProviderContext implements PluginProviderContext {

    private final ScalaPluginMeta configuration;

    public ScalaPluginProviderContext(ScalaPluginDescription description) {
        this.configuration = new ScalaPluginMeta(description);
    }

    @Override
    public @NotNull ScalaPluginMeta getConfiguration() {
        return configuration;
    }

    @Override
    public @NotNull Path getDataDirectory() {
        return PluginInitializerManager.instance().pluginDirectoryPath().resolve(getConfiguration().getDisplayName());
    }

    @Override
    public @NotNull ComponentLogger getLogger() {
        return ComponentLogger.logger(getConfiguration().getMainClass());
    }
}
