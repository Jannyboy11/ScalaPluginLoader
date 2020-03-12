package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import xyz.janboerman.scalaloader.event.EventBus;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ScalaPlugin!
 * <br>
 * This is what your main class (can be an {@literal object} singleton) usually extends in your Scala plugin project.
 */
public abstract class ScalaPlugin implements Plugin {

    private final ScalaPluginDescription description;
    private PluginDescriptionFile lazyDescription;
    private ScalaPluginLogger lazyLogger; //regular PluginLogger forces evaluation of our PluginDescriptionFile
    private File lazyDataFolder;
    private File lazyConfigFile;

    private Server server;
    private ScalaPluginLoader pluginLoader;
    private File file;
    private ScalaPluginClassLoader classLoader;
    private boolean naggable = true;

    private FileConfiguration config;

    private boolean enabled;

    /**
     * This constructor should be used when your class is loaded by a {@link ScalaPluginClassLoader} - which is always the case in a server environment.
     * @param pluginDescription the plugin's configuration
     */
    protected ScalaPlugin(ScalaPluginDescription pluginDescription) {
        this.description = pluginDescription;
        this.description.setMain(getClass().getName());

        if (getClass().getClassLoader() instanceof ScalaPluginClassLoader) {
            this.classLoader = (ScalaPluginClassLoader) getClass().getClassLoader();
            this.server = classLoader.getServer();
            this.description.addYaml(classLoader.getExtraPluginYaml());
            this.description.setApiVersion(classLoader.getApiVersion().getVersionString());
            this.pluginLoader = classLoader.getPluginLoader();
            this.file = classLoader.getPluginJarFile();
        } else {
            getLogger().warning("ScalaPlugin got instantiated but was not loaded by a ScalaPluginClassLoader!");
            getLogger().warning("Many of ScalaPlugin's fields will remain uninitialised!");
        }
    }

    /**
     * An initializing constructor. For (unit) testing purposes only.
     * Many fields will remain uninitialised and most methods will give unexpected results.
     * @param pluginDescription the description
     * @param server the server
     * @param file the plugin's jar file
     */
    protected ScalaPlugin(ScalaPluginDescription pluginDescription, Server server, File file) {
        this.description = pluginDescription;
        this.description.setMain(getClass().getName());
        this.server = server;
        this.file = file;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Can only be used when the ScalaPlugin is loaded by the ScalaPluginLoader.
     * Otherwise use {@code scalaPlugin.getClass().getClassLoader()}.
     * @return the ScalaPluginClassLoader that loaded classes from this plugin
     */
    protected ScalaPluginClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Get the ScalaLoader's EventBus! This event bus allows you to call {@link xyz.janboerman.scalaloader.event.Event}s,
     * which will allow you to write less boilerplate.
     *
     * @return the event bus
     */
    protected final EventBus getEventBus() {
        return getClassLoader().getPluginLoader().getEventBus();
    }

    /**
     * Get the version of Scala this plugin.
     * @return the version of Scala, as per the {@link ScalaPluginClassLoader} used for this plugin
     */
    public final String getScalaVersion() {
        return getClassLoader().getScalaVersion();
    }

    /**
     * Get the name of the plugin.
     * @return the plugin's name
     */
    public String getName() {
        return description.getName();
    }

    /**
     * Get the data folder for of this ScalaPlugin
     * @return the data folder
     */
    @Override
    public File getDataFolder() {
        return lazyDataFolder == null ? lazyDataFolder = new File(file.getParent(), getName()) : lazyDataFolder;
    }

    /**
     * Get the description of this ScalaPlugin.
     * @return the description
     */
    ScalaPluginDescription getScalaDescription() {
        return description;
    }

    /**
     * Get the description of this ScalaPlugin.
     * @return the description
     */
    @Override
    public PluginDescriptionFile getDescription() {
        return lazyDescription == null ? lazyDescription = description.toPluginDescriptionFile() : lazyDescription;
    }

    /**
     * Get the configurations for this plugin.
     * @return the configurations
     */
    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    /**
     * Get a resource from the ScalaPlugin's jar file.
     * @param filename the file inside the jar file
     * @return an InputStream providing the contents of the resource, or null if the resource wasn't found
     */
    @Override
    public InputStream getResource(String filename) {
        Objects.requireNonNull(filename, "Filename cannot be null");

        URL url = getClassLoader().getResource(filename);
        if (url == null) return null;

        try {
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get the default configuration file used for this plugin.
     * @return the configuration file
     */
    public File getConfigFile() {
        return lazyConfigFile == null ? lazyConfigFile = new File(getDataFolder(), "config.yml") : lazyConfigFile;
    }

    /**
     * Saves the configuration to the configuration file.
     */
    @Override
    public void saveConfig() {
        try {
            getConfig().save(getConfigFile());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save config to " + getConfigFile(), e);
        }
    }

    /**
     * Save the default configuration file to the plugin's data folder.
     */
    @Override
    public void saveDefaultConfig() {
        if (!getConfigFile().exists()) {
            saveResource("config.yml", false);
        }
    }

    /**
     * Save a resource in the plugin's jar file to the plugin's data folder.
     * @param resourcePath the file inside the jar
     * @param replace whether to replace the file in the data folder if one exists already
     */
    @Override
    public void saveResource(String resourcePath, boolean replace) {
        //copy from the jar into this plugin's directory
        //blatantly copied from org.bukkit.plugin.java.JavaPlugin.java
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + file);
        }

        File outFile = new File(getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }

    }

    /**
     * Reads the configuration from the configuration file.
     */
    @Override
    public void reloadConfig() {
        //load from config file in plugin directory if present - otherwise load values from the default config (included in the jar)
        config = YamlConfiguration.loadConfiguration(getConfigFile());

        final InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream == null) {
            return;
        }

        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
    }

    /**
     * Gets the plugin loader.
     * @return a {@link ScalaPluginLoader}
     */
    @Override
    public ScalaPluginLoader getPluginLoader() {
        return pluginLoader;
    }

    /**
     * Get the server this plugin runs on
     * @return the server
     */
    @Override
    public Server getServer() {
        return server;
    }

    /**
     * Get whether this plugin is enabled.
     * @return true if the plugin is enabled, otherwise false
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Callback that is invoked when the plugin is disabled by the {@link ScalaPluginLoader}.
     * This method can be overridden by subclasses.
     */
    @Override
    public void onDisable() {
    }

    /**
     * Callback that is invoked when the plugin is loaded by the {@link ScalaPluginLoader}.
     * This method can be overridden by subclasses.
     */
    @Override
    public void onLoad() {
    }

    /**
     * Callback that is invoked when the plugin is enabled by the {@link ScalaPluginLoader}.
     * This method can be overridden by subclasses.
     */
    @Override
    public void onEnable() {
    }

    /**
     * Get whether you shouldn't use this plugin's logger.
     * @return true if you can use the logger without problems, otherwise false
     */
    @Override
    public boolean isNaggable() {
        return naggable;
    }

    /**
     * Set whether the plugin can be nagged.
     * @param canNag true if the plugin must be able to be nagged, otherwise false.
     */
    @Override
    public void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }

    /**
     * Get the default world's chunk generator
     * @param worldName the name of the world
     * @param id the id of the world
     * @return a chunkgenerator if present, or null when this plugin doesn't provide a chunk generator
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return null;
    }

    /**
     * Gets this plugin's logger.
     * @return the logger
     */
    @Override
    public Logger getLogger() {
        return lazyLogger == null ? lazyLogger = new ScalaPluginLogger(this) : lazyLogger;
    }

    /**
     * Get a command defined in the plugin.yml file or in the {@link ScalaPluginDescription}.
     * @param name the name of the command
     * @return the command if one with a corresponding name was found, otherwise null
     */
    protected final PluginCommand getCommand(String name) {
        return getServer().getPluginCommand(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    /**
     * Checks whether this plugin is equal to another plugin. In practice this only happens when this plugin is compared to itself.
     * @param other the other plugin
     * @return true if the plugins are equal, otherwise false
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (!(other instanceof Plugin)) return false;
        Plugin that = (Plugin) other;
        return Objects.equals(this.getName(), that.getName());
    }

    /**
     * Get the hash code of this plugin.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * Get a string representation of this plugin.
     * @return the string representation.
     */
    @Override
    public String toString() {
        return getName();
    }
}
