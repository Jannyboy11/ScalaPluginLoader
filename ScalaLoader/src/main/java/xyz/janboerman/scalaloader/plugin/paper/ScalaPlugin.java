package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

public abstract class ScalaPlugin extends JavaPlugin implements IScalaPlugin {

    private final ScalaPluginDescription description;

    protected ScalaPlugin(ScalaPluginDescription description) {
        this.description = description;
    }

    protected ScalaPlugin() {
        //TODO get ScalaPlugin description from the ScalaPluginClassLoader.
        //TODO there must be either a paper-plugin.yml or plugin.yml in the plugin's jar file.
        this.description = null;
    }

    ScalaPluginDescription getScalaDescription() {
        return description;
    }

    public ScalaPluginClassLoader classLoader() {
        //TODO is this implementation correct? who knows!
        return (ScalaPluginClassLoader) super.getClassLoader();
    }

    @Override
    public EventBus getEventBus() {
        //TODO
        return null;
    }

    @Override
    public String getScalaVersion() {
        //TODO
        return null;
    }

    @Override
    public ScalaRelease getScalaRelease() {
        //TODO
        return null;
    }

    @Override
    public String getDeclaredScalaVersion() {
        //TODO
        return null;
    }

}
