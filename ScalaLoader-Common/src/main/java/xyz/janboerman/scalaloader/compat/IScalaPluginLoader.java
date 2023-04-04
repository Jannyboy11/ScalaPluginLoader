package xyz.janboerman.scalaloader.compat;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.DebugSettings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Represents the plugin loader of a ScalaPlugin.
 * <p>
 * On CraftBukkit and Spigot, this is a single instance for all ScalaPlugins,
 * while on Paper, this is a loader instance per ScalaPlugin.
 */
public interface IScalaPluginLoader {

    /** Internal use only */
    public DebugSettings debugSettings();

    /**
     * Make the classes of a ScalaPlugin accessible to a JavaPlugin.
     * @param scalaPlugin the ScalaPlugin
     * @param javaPlugin the JavaPlugin
     */
    public static void openUpToJavaPlugin(IScalaPlugin scalaPlugin, JavaPlugin javaPlugin) {
        if (!IScalaLoader.getInstance().isPaperPlugin()) {
            //IScalaPluginLoader instance == xyz.janboerman.scalaloader.plugin.ScalaPluginLoader.getInstance();
            try {
                Class<?> scalaPluginLoaderClass = Class.forName("xyz.janboerman.scalaloader.plugin.ScalaPluginLoader");
                Method getInstanceMethod = scalaPluginLoaderClass.getDeclaredMethod("getInstance");
                Object scalaPluginLoader = getInstanceMethod.invoke(null);

                Class<?> scalaPluginClass = Class.forName("xyz.janboerman.scalaloader.plugin.ScalaPlugin");
                Method openUpToJavaPluginMethod = scalaPluginLoaderClass.getDeclaredMethod("openUpToJavaPlugin", scalaPluginClass, JavaPlugin.class);
                openUpToJavaPluginMethod.invoke(scalaPluginLoader, scalaPlugin, javaPlugin);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException notPossible) {
                IScalaLoader.getInstance().getLogger().log(Level.SEVERE, "Not a Paper plugin, but couldn't find ScalaLoader-Bukkit classes or methods", notPossible);
            }
        }
        //else: IScalaPlugin is a xyz.janboerman.scalaloader.paper.plugin.ScalaPlugin (Paper plugin), which extends JavaPlugin, so we can just no-op in that case.
    }

}