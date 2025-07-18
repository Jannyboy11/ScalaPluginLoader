package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.bytecode.Replaced;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.description.Api;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.description.Scala;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * A ScalaPlugin!
 * <br>
 * This is what your main class (can be an {@literal object} singleton) usually extends in your Scala plugin project.
 * <br>
 * Example:
 * <pre>
 * <code>
 * import xyz.janboerman.scalaloader.plugin.ScalaPlugin
 * import xyz.janboerman.scalaloader.plugin.description.*
 *
 * {@literal @}Scala(ScalaVersion.v3_1_1)
 * object MyPlugin extends ScalaPlugin:
 *     override def onEnable(): Unit =
 *         getLogger().info("Hello, World!")
 * </code>
 * </pre>
 * Optionally you can pass a {@link ScalaPluginDescription} to the ScalaPlugin constructor which allows you to not have to provide a plugin.yml plugin description file.
 *
 * <p>
 * For ScalaLoader's Event api, see {@link EventBus}, {@link xyz.janboerman.scalaloader.event.Event} and {@link xyz.janboerman.scalaloader.event.Cancellable}.
 */
@Replaced //Paper
public abstract class ScalaPlugin implements IScalaPlugin {

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
            this.description.setScalaVersion(getDeclaredScalaVersion());
            this.pluginLoader = classLoader.getPluginLoader();
            this.file = classLoader.getPluginJarFile();
        } else {
            getLogger().warning("ScalaPlugin got instantiated but was not loaded by a ScalaPluginClassLoader!");
            getLogger().warning("Many of ScalaPlugin's fields will remain uninitialised!");
        }
    }

    /**
     * This constructor can be used if you use a plugin.yml to define your plugin description.
     */
    protected ScalaPlugin() {
        if (getClass().getClassLoader() instanceof ScalaPluginClassLoader) {
            this.classLoader = (ScalaPluginClassLoader) getClass().getClassLoader();
            this.server = classLoader.getServer();
            this.pluginLoader = classLoader.getPluginLoader();
            this.file = classLoader.getPluginJarFile();

            Map<String, Object> pluginYaml = classLoader.getExtraPluginYaml();
            String name = Objects.requireNonNull(pluginYaml.get("name"), "name unspecified in plugin.yml").toString();
            String version = Objects.requireNonNull(pluginYaml.get("version"), "version unspecified in plugin.yml").toString();
            this.description = new ScalaPluginDescription(name, version);
            this.description.setMain(getClass().getName());
            this.description.setApiVersion(classLoader.getApiVersion().getVersionString());
            this.description.setScalaVersion(getDeclaredScalaVersion());
            this.description.readFromPluginYamlData(pluginYaml);
        } else {
            throw new IllegalStateException("ScalaPlugin nullary constructor can only be used when loaded by a " + ScalaPluginClassLoader.class.getSimpleName() + ".");
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
        this.description.setApiVersion(getDeclaredApiVersion());
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
     *
     * @deprecated This method *WILL NOT EXIST* at runtime when running your plugin on Paper. Use {@link #classLoader()} instead.
     */
    @Deprecated //TODO at the next deprecation clean up spree, make this method protected and final.
    // TODO consider also using MainClassCallerMigrator in bytecode transformations in ScalaLoader-Bukkit.
    public ScalaPluginClassLoader getClassLoader() {
        return classLoader();
    }

    /**
     * Can only be used when the ScalaPlugin is loaded by the ScalaPluginLoader.
     * Otherwise use {@code scalaPlugin.getClass().getClassLoader()}.
     * @return the ScalaPluginClassLoader that loaded classes from this plugin
     */
    @Override
    public final ScalaPluginClassLoader classLoader() {
        return classLoader;
    }

    /**
     * Get the ScalaLoader's EventBus! This event bus allows you to call {@link xyz.janboerman.scalaloader.event.Event}s,
     * which will allow you to write less boilerplate.
     *
     * @return the event bus
     */
    @Override
    public final EventBus getEventBus() {
        return getPluginLoader().getEventBus();
    }

    /**
     * Get the version of Scala this plugin.
     * @return the version of Scala, as per the {@link ScalaPluginClassLoader} used for this plugin
     */
    @Override
    public final String getScalaVersion() {
        return getClassLoader().getScalaVersion();
    }

    /**
     * Get the compatibility-release version of Scala used by this plugin.
     * @return the compatibility release
     */
    @Override
    public final ScalaRelease getScalaRelease() {
        return getClassLoader().getScalaRelease();
    }

    /**
     * Get the version of Scala that this plugin depends on.
     * At runtime a newer compatible version of Scala could be used instead.
     * @return the defined scala version
     */
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

        Object yamlDefinedScalaVersion = getClassLoader().getExtraPluginYaml().get("scala-version");
        if (yamlDefinedScalaVersion != null) {
            return yamlDefinedScalaVersion.toString();
        }

        assert false : "ScalaPlugin defined its Scala version, but not via the @Scala or @CustomScala annotation, or in plugin.yml";

        return getScalaVersion(); //fallback - to make this more robust in production
    }

    /**
     * Get the api-version that was declared by this plugin.
     * @return the bukkit api version
     */
    public final String getDeclaredApiVersion() {
        Class<?> mainClass = getClass();

        Api api = mainClass.getDeclaredAnnotation(Api.class);
        if (api != null) {
            return api.value().getVersionString();
        }

        Object yamlDefinedApi = getClassLoader().getExtraPluginYaml().get("api-version");
        if (yamlDefinedApi != null) {
            return yamlDefinedApi.toString();
        }

        return ApiVersion.latestVersionString();
    }

    /**
     * Get the name of the plugin.
     * @return the plugin's name
     */
    @Override
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
     * Get the log prefix of this ScalaPlugin.
     * @return the prefix
     */
    @Override
    public String getPrefix() {
        return getScalaDescription().getPrefix();
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
    @Override
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
     * @deprecated use {@link #pluginLoader()} instead.
     */
    @Deprecated //TODO at the next deprecation clean up spree, make this method final.
    @Override
    public ScalaPluginLoader getPluginLoader() {
        return pluginLoader();
    }

    /**
     * Get the plugin loader.
     * @return a {@link ScalaPluginLoader}
     */
    @Override
    public ScalaPluginLoader pluginLoader() {
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
     * Gets a ChunkGenerator for use in a default world, as specified in the server configuration.
     * @param worldName the name of the world that the generator will be applied to
     * @param id Unique ID, if any, that was specified to indicate which generated was requested
     * @return a chunk generator if present, or null when this plugin doesn't provide a chunk generator
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return null;
    }

    /**
     * Gets a BiomeProvider for use in a default world, as specified in the server configuration.
     * @param worldName the name of the world that the biome provider will be applied to
     * @param id Unique ID, if any, that was specified to indicate which biome provider was requested
     * @return a biome provider if present, or null when this plugin doesn't provide a biome provider
     */
    @Override
    public BiomeProvider getDefaultBiomeProvider(String worldName, String id) {
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

    /**
     * Get the ScalaPlugin's instance given its class.
     * @param pluginClass the class of the ScalaPlugin
     * @param <P> the type of the plugin
     * @return the ScalaPlugin
     * @throws IllegalArgumentException if the class is not a subtype of {@link ScalaPlugin}
     * @throws ClassCastException if the plugin's instance is not of type pluginClass
     * @throws IllegalStateException when called from a plugin's constructor or initializer
     */
    protected static <P extends ScalaPlugin> P getPlugin(Class<P> pluginClass) {
        ClassLoader classLoader = pluginClass.getClassLoader();
        if (classLoader instanceof ScalaPluginClassLoader) {
            ScalaPlugin plugin = ((ScalaPluginClassLoader) classLoader).getPlugin();
            if (plugin != null) {
                return pluginClass.cast(plugin);
            } else {
                throw new IllegalStateException("Can't call " + ScalaPlugin.class.getName() + ".getPlugin(java.lang.Class) from your plugin's constructor or initializer.");
            }
        } else {
            throw new IllegalArgumentException(pluginClass.getName() + " is not loaded by a " + ScalaPluginClassLoader.class.getName() + ". Is it even a ScalaPlugin?");
        }
    }
}
