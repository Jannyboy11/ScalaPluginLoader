package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.description.Scala;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.paper.description.DescriptionClassLoader;

import java.io.File;

public abstract class ScalaPlugin extends JavaPlugin implements IScalaPlugin {

    private final ScalaPluginDescription description;
    private File configFile;

    protected ScalaPlugin(ScalaPluginDescription description) {
        this.description = description;
    }

    protected ScalaPlugin() {
        if (getClass().getClassLoader() instanceof ScalaPluginClassLoader classLoader) {
            this.description = classLoader.getConfiguration().description;
        } else {
            if (!(getClass().getClassLoader() instanceof DescriptionClassLoader)) {
                ScalaLoader.getInstance().getLogger().warning("ScalaPlugin nullary constructor called without ScalaPluginLoader. This will likely result in unexpected behaviour!");
            }
            this.description = null;
        }
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
        return configFile == null ? configFile = new File(classLoader().getDataDirectory().toFile(), "config.yml") : configFile;
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
