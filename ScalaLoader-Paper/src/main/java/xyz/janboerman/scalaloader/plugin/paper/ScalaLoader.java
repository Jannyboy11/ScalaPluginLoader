package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.util.Collection;

public class ScalaLoader extends JavaPlugin implements IScalaLoader {

    private DebugSettings debugSettings = new DebugSettings(this);
    private File scalaPluginsFolder;

    public ScalaLoader() {
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }
    }

    @Override
    public boolean isPaperPlugin() {
        return true;
    }

    @Override
    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    @Override
    public File getScalaPluginsFolder() {
        return scalaPluginsFolder;
    }

    @Override
    public Collection<ScalaPlugin> getScalaPlugins() {
        //TODO
        return null;
    }

    @Override
    public void onLoad() {
        ScalaLoaderUtils.initConfiguration(this);
        loadScalaPlugins();
    }

    @Override
    public void onEnable() {
        ScalaLoaderUtils.initCommands(this);
        enableScalaPlugins();
        ScalaLoaderUtils.initBStats(this);
    }

    private void loadScalaPlugins() {
        //TODO might need to load them 'all at once' because of the new paper plugin update
        for (File file : scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar"))) {
            //loadScalaPlugin(file);
        }
    }

    private void enableScalaPlugins() {
        //TODO
    }

    @Override
    public ScalaPluginLoader getScalaPluginLoader() {
        return ScalaPluginLoader.getInstance();
    }
}
