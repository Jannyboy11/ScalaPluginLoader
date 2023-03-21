package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jannyboy11
 */
public class ScalaLoader extends JavaPlugin implements IScalaLoader {

    private EventBus eventBus;
    private DebugSettings debugSettings = new DebugSettings(this);
    private File scalaPluginsFolder;
    private Set<ScalaPlugin> scalaPlugins = new HashSet<>();

    public ScalaLoader() {
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }
    }

    static ScalaLoader getInstance() {
        return JavaPlugin.getPlugin(ScalaLoader.class);
    }

    @Override
    public boolean isPaperPlugin() {
        return true;
    }

    @Override
    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    public EventBus getEventBus() {
        return eventBus == null ? eventBus = new EventBus(getServer().getPluginManager()) : eventBus;
    }

    @Override
    public File getScalaPluginsFolder() {
        return scalaPluginsFolder;
    }

    @Override
    public Collection<ScalaPlugin> getScalaPlugins() {
        return scalaPlugins;
    }

    protected void addScalaPlugin(ScalaPlugin plugin) {
        scalaPlugins.add(plugin);
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
        loadScalaPlugins(scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar")));
    }

    private void loadScalaPlugins(File[] files) {
        if (files == null) return;

        //TODO for each file, do the scanning.
        //TODO for each plugin, record the declared scala version
        //TODO then, calculate the newest compatible version for each plugin
        //TODO use those for loading!

        //TODO make sure to register the plugins with Paper's PluginInstanceManager.
        //TODO make sure that plugin dependencies are registered correctly.
        //TODO call PaperPluginInstanceManager#loadPlugin(Plugin provided) - it does both of the above!

        //TODO for every ScalaPlugin, make sure to call Plugin.onLoad().
    }

    private void enableScalaPlugins() {
        //TODO for every ScalaPlugin, call Plugin.onEnable();
    }

}
