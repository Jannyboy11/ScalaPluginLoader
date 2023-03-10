package xyz.janboerman.scalaloader.plugin.paper;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.commands.DumpClass;
import xyz.janboerman.scalaloader.commands.ListScalaPlugins;
import xyz.janboerman.scalaloader.commands.ResetScalaUrls;
import xyz.janboerman.scalaloader.commands.SetDebug;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
        initConfigurations();
        loadScalaPlugins();
    }

    @Override
    public void onEnable() {
        initCommands();

        //TODO enable ScalaPlugins

        initBStats();
    }

    private void initConfigurations() {
        //TODO
    }

    private void initCommands() {
        getCommand("resetScalaUrls").setExecutor(new ResetScalaUrls(this));
        getCommand("dumpClass").setExecutor(new DumpClass(this));
        getCommand("setDebug").setExecutor(new SetDebug(getDebugSettings()));
        getCommand("listScalaPlugins").setExecutor(new ListScalaPlugins());
    }

    private void initBStats() {
        final int pluginId = 9150;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new DrilldownPie("declared_scala_version", () -> {
            Map<String /*compat-release version*/, Map<String /*actual version*/, Integer /*amount*/>> stats = new HashMap<>();

            for (IScalaPlugin scalaPlugin : getScalaPlugins()) {
                String scalaVersion = scalaPlugin.getDeclaredScalaVersion();
                String compatVersion = ScalaRelease.fromScalaVersion(scalaVersion).getCompatVersion();

                stats.computeIfAbsent(compatVersion, k -> new HashMap<>())
                        .compute(scalaVersion, (v, amount) -> amount == null ? 1 : amount + 1);
            }

            return stats;
        }));
    }

    private void loadScalaPlugins() {
        //TODO might need to load them 'all at once' because of the new paper plugin update
        for (File file : scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar"))) {
            //loadScalaPlugin(file);
        }

    }
}
