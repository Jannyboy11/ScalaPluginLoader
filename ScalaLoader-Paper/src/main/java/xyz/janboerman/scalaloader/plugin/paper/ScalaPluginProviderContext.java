package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ScalaPluginProviderContext implements PluginProviderContext {

    @Override
    public @NotNull ScalaPluginMeta getConfiguration() {
        return null;
    }

    @Override
    public @NotNull Path getDataDirectory() {
        return null;
    }

    @Override
    public @NotNull ComponentLogger getLogger() {
        return ComponentLogger.logger(getConfiguration().getMainClass());
    }
}
