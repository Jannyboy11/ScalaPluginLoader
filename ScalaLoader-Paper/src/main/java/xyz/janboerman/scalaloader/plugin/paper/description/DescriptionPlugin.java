package xyz.janboerman.scalaloader.plugin.paper.description;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.io.File;

//TODO use this at boot-strap time (make the plugin extend DescriptionPlugin) just so we can extract the description!
public class DescriptionPlugin extends JavaPlugin implements IScalaPlugin {

    private final ScalaPluginDescription description;

    protected DescriptionPlugin(ScalaPluginDescription description) {
        this.description = description;
    }

    protected DescriptionPlugin() {
        this.description = null;
    }

    public ScalaPluginDescription getScalaDescription() {
        return description;
    }

    @Override
    public File getConfigFile() {
        throw new UnsupportedOperationException("Not yet supported in constructor");
    }

    @Override
    public EventBus getEventBus() {
        throw new UnsupportedOperationException("Not yet supported in constructor");
    }

    @Override
    public String getScalaVersion() {
        throw new UnsupportedOperationException("Not yet supported in constructor");
    }

    @Override
    public ScalaRelease getScalaRelease() {
        throw new UnsupportedOperationException("Not yet supported in constructor");
    }

    @Override
    public String getDeclaredScalaVersion() {
        throw new UnsupportedOperationException("Not yet supported in constructor");
    }

    @Override
    public String getPrefix() {
        throw new UnsupportedOperationException("Not yet supported in constructor");
    }

}
