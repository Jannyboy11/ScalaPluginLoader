package xyz.janboerman.scalaloader.compat;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import xyz.janboerman.scalaloader.DebugSettings;

import java.io.File;

public interface IScalaLoader extends Plugin {

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

}
