package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import xyz.janboerman.scalaloader.ScalaLoader;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class ScalaPluginLoader implements PluginLoader {
    //TODO the pluginloader should only have one instance.
    //TODO the pluginclassloader has per-plugin instances.

    private final Server server;
    private final JavaPluginLoader javaPluginLoader;

    private final Set<PluginScalaVersion> scalaVersions = new HashSet<>();
    private final Pattern[] fileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    private final Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
    private final List<ScalaPluginClassLoader> loaders = new CopyOnWriteArrayList<>();

    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");
        this.javaPluginLoader = (JavaPluginLoader) JavaPlugin.getPlugin(ScalaLoader.class).getPluginLoader();
    }

    /**
     * Loads the plugin contained in the specified file
     *
     * @param file File to attempt to load
     * @return Plugin that was contained in the specified file, or null if
     * unsuccessful
     * @throws InvalidPluginException     Thrown when the specified file is not a
     *                                    plugin
     * @throws UnknownDependencyException If a required dependency could not
     *                                    be found
     */
    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        //TODO I want to be able to use both 'object extends ScalaPlugin' and 'class extends ScalaPlugin'
        //TODO even plugins written in java should be able to
        //TODO delegate to JavaPluginLoader if the jar file does not contain a scala plugin.
        return null;
    }

    /**
     * Loads a PluginDescriptionFile from the specified file
     *
     * @param file File to attempt to load from
     * @return A new PluginDescriptionFile loaded from the plugin.yml in the
     * specified file
     * @throws InvalidDescriptionException If the plugin description file
     *                                     could not be created
     */
    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        return null;
    }

    /**
     * Returns a list of all filename filters expected by this PluginLoader
     *
     * @return The filters
     */
    @Override
    public Pattern[] getPluginFileFilters() {
        return getPluginFileFilters().clone();
    }

    /**
     * Creates and returns registered listeners for the event classes used in
     * this listener
     *
     * @param listener The object that will handle the eventual call back
     * @param plugin   The plugin to use when creating registered listeners
     * @return The registered listeners.
     */
    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return javaPluginLoader.createRegisteredListeners(listener, plugin);
    }

    /**
     * Enables the specified plugin
     * <p>
     * Attempting to enable a plugin that is already enabled will have no
     * effect
     *
     * @param plugin Plugin to enable
     */
    @Override
    public void enablePlugin(Plugin plugin) {

    }

    /**
     * Disables the specified plugin
     * <p>
     * Attempting to disable a plugin that is not enabled will have no effect
     *
     * @param plugin Plugin to disable
     */
    @Override
    public void disablePlugin(Plugin plugin) {

    }
}
