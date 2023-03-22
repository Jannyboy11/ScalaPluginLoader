package xyz.janboerman.scalaloader.compat;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface IScalaLoader extends Plugin {

    public static IScalaLoader getInstance() {
        JavaPlugin whoLoadedThis = JavaPlugin.getProvidingPlugin(IScalaLoader.class);
        return (IScalaLoader) whoLoadedThis;
    }

    public boolean isPaperPlugin();

    public DebugSettings getDebugSettings();

    public File getScalaPluginsFolder();

    /**
     * Runs a task on the server's main thread.
     *
     * @deprecated This method is only used by deprecated methods, and thus is no longer needed.
     *             Should you need equivalent functionality, then use the following snippet:
     *             <pre>
     *                 <code>
     *                     if (Bukkit.isPrimaryThread()) {
     *                         runnable.run();
     *                     } else {
     *                         Bukkit.getScheduler().runTask(scalaLoader, runnable);
     *                     }
     *                 </code>
     *             </pre>
     *
     * @param runnable the task to run on the main thread
     */
    @Deprecated
    public default void runInMainThread(Runnable runnable) {
        Server server = getServer();

        if (server.isPrimaryThread()) {
            runnable.run();
        } else {
            server.getScheduler().runTask(this, runnable);
        }
    }

    public Collection<? extends IScalaPlugin> getScalaPlugins();

    /**
     * Add new versions of Scala to ScalaLoader's config.
     * @param versions the scala versions
     * @return whether a new version was added to the config
     */
    public default boolean saveScalaVersionsToConfig(PluginScalaVersion... versions) {
        FileConfiguration config = getConfig();
        Set<PluginScalaVersion> scalaVersions = new LinkedHashSet<>(Compat.listOf(versions));
        boolean wasAdded = scalaVersions.addAll((List<PluginScalaVersion>) config.getList("scala-versions", Compat.emptyList()));
        config.set("scala-versions", Compat.listCopy(scalaVersions));
        saveConfig();
        return wasAdded;
    }

}
