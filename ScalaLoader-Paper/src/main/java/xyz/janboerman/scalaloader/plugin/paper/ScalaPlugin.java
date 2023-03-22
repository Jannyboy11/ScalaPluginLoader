package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.description.Scala;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;

import java.io.File;

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

    @Override
    public String getPrefix() {
        return getScalaDescription().getPrefix();
    }

    @Override
    public ScalaPluginClassLoader classLoader() {
        return (ScalaPluginClassLoader) super.getClassLoader();
    }

    @Override
    public File getConfigFile() {
        //TODO implement this.
        //TODO might want to respect Paper's PluginProviderContext.
        return null;
    }

    @Override
    public EventBus getEventBus() {
        return ScalaLoader.getInstance().getEventBus();
    }

    @Override
    public String getScalaVersion() {
        return classLoader().getScalaVersion();
    }

    @Override
    public final String getDeclaredScalaVersion() {
        Class<?> mainClass = getClass();

        Scala scala = mainClass.getDeclaredAnnotation(Scala.class);
        if (scala != null) {
            return scala.version().getVersion();
        }

        CustomScala customScala = mainClass.getDeclaredAnnotation(CustomScala.class);
        if (customScala != null) {
            return customScala.value().value();
        }

        Object yamlDefinedScalaVersion = classLoader().getExtraPluginYaml().get("scala-version");
        if (yamlDefinedScalaVersion != null) {
            return yamlDefinedScalaVersion.toString();
        }

        return getScalaVersion(); //fallback - to make this more robust in production
    }

    @Override
    public String toString() {
        return getName();
    }

}
