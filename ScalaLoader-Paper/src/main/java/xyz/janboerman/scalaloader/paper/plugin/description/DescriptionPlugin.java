package xyz.janboerman.scalaloader.paper.plugin.description;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLogger;

import java.io.File;
import java.util.logging.Logger;

/** Special plugin instance to obtain your plugin's description. Internal use only. */
public class DescriptionPlugin extends JavaPlugin implements IScalaPlugin {

    private final ScalaPluginDescription description;
    private final Logger logger;

    protected DescriptionPlugin(ScalaPluginDescription description) {
        this.description = description;
        this.logger = new ScalaPluginLogger(this);
    }

    protected DescriptionPlugin() {
        this.description = null;
        this.logger = new ScalaPluginLogger(this);
    }

    public ScalaPluginDescription getScalaDescription() {
        return description;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getConfigFile() {
        throw new UnsupportedOperationException("Not yet supported in constructor or initializer");
    }

    @Override
    public EventBus getEventBus() {
        return ScalaLoader.getInstance().getEventBus();
    }

    @Override
    public String getScalaVersion() {
        if (description == null)
            return ((DescriptionClassLoader) getClassLoader()).getScalaVersion();

        return description.getScalaVersion();
    }

    @Override
    public ScalaRelease getScalaRelease() {
        return ScalaRelease.fromScalaVersion(getDeclaredScalaVersion());
    }

    @Override
    public String getDeclaredScalaVersion() {
        if (description == null)
            return ((DescriptionClassLoader) getClassLoader()).getScalaVersion();

        return description.getScalaVersion();
    }

    @Override
    public String getPrefix() {
        if (description == null)
            return getClass().getSimpleName();

        return description.getPrefix();
    }

    public DescriptionClassLoader descriptionClassLoader() {
        return (DescriptionClassLoader) super.getClassLoader();
    }

}
