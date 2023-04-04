package xyz.janboerman.scalaloader.paper.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.description.Scala;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionClassLoader;

import java.io.File;

/**
 * Representation of a ScalaPlugin when running on Paper. ScalaLoader's bytecode transformer will ensure that subclasses of xyz.janboerman.scalaloader.plugin.ScalaPlugin
 * will be subclasses of xyz.janboerman.scalalaoder.paper.plugin.ScalaPlugin at runtime when ScalaLoader runs on Paper.
 */
public abstract class ScalaPlugin extends JavaPlugin implements IScalaPlugin {

    private final ScalaPluginDescription description;
    private File configFile;

    /**
     * Use this super constructor if you don't want to describe your plugin using a Yaml file.
     * You can *just* provide the description directly as an argument.
     * @param description your plugin's description
     */
    protected ScalaPlugin(ScalaPluginDescription description) {
        this.description = description;
    }

    /**
     * Use this super constructor if you have a plugin.yml or paper-plugin.yml.
     */
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

    /** {@inheritDoc} */
    @Override
    public String getPrefix() {
        return getScalaDescription().getPrefix();
    }

    /** {@inheritDoc} */
    @Override
    public ScalaPluginClassLoader classLoader() {
        return (ScalaPluginClassLoader) super.getClassLoader();
    }

    @Override
    public ScalaPluginLoader pluginLoader() {
        return (ScalaPluginLoader) classLoader().getPluginLoader();
    }

    /** {@inheritDoc} */
    @Override
    public File getConfigFile() {
        return configFile == null ? configFile = new File(classLoader().getDataDirectory().toFile(), "config.yml") : configFile;
    }

    /** {@inheritDoc} */
    @Override
    public EventBus getEventBus() {
        return ScalaLoader.getInstance().getEventBus();
    }

    /** {@inheritDoc} */
    @Override
    public String getScalaVersion() {
        return classLoader().getScalaVersion();
    }

    /** {@inheritDoc} */
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
