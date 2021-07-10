package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.description.Scala;
import static xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.Command;
import static xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.Permission;

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
            this.description.addYaml(pluginYaml);
            this.description.setMain(getClass().getName());
            this.description.setApiVersion(classLoader.getApiVersion().getVersionString());
            this.description.description((String) pluginYaml.get("description"));
            String author = (String) pluginYaml.get("author");
            if (author != null)
                this.description.addAuthor(author);
            Iterable authors = (Iterable) pluginYaml.get("authors");
            if (authors != null)
                for (Object auth : authors)
                    if (auth != null)
                        this.description.addAuthor(auth.toString());
            Iterable contributors = (Iterable) pluginYaml.get("contributors");
            if (contributors != null)
                for (Object contrib : contributors)
                    if (contrib != null)
                        this.description.addContributor(contrib.toString());
            this.description.website((String) pluginYaml.get("website"));
            this.description.prefix((String) pluginYaml.get("prefix"));
            String load = (String) pluginYaml.get("load");
            if (load != null)
                this.description.loadOrder(PluginLoadOrder.valueOf(load));
            String defaultPermissionDefault = (String) pluginYaml.get("default-permission");
            if (defaultPermissionDefault != null)
                this.description.permissionDefault(PermissionDefault.getByName(defaultPermissionDefault));
            List<String> depend = (List<String>) pluginYaml.get("depend");
            if (depend != null)
                for (String dep : depend)
                    if (dep != null)
                        this.description.addHardDepend(dep);
            List<String> softDepend = (List<String>) pluginYaml.get("softdepend");
            if (softDepend != null)
                for (String softDep : softDepend)
                    if (softDep != null)
                        this.description.addSoftDepend(softDep);
            List<String> inverseDepend = (List<String>) pluginYaml.get("loadbefore");
            if (inverseDepend != null)
                for (String inverseDep : inverseDepend)
                    if (inverseDep != null)
                        this.description.addLoadBefore(inverseDep);
            List<String> provides = (List<String>) pluginYaml.get("provides");
            if (provides != null)
                this.description.provides(provides.toArray(new String[0]));
            Map<String, Map<String, Object>> commands = (Map) pluginYaml.get("commands");
            if (commands != null) {
                for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                    String cmdName = entry.getKey();
                    Map<String, Object> value = entry.getValue();
                    Command cmd = new Command(cmdName);
                    cmd.description((String) value.get("description"));
                    cmd.usage((String) value.get("usage"));
                    cmd.permission((String) value.get("permission"));
                    cmd.permissionMessage((String) value.get("permission-message"));
                    Iterable aliases = (Iterable) value.get("aliases");
                    for (Object alias : aliases) cmd.addAlias(alias.toString());
                    this.description.addCommand(cmd);
                }
            }
            Map<String, Map<String, Object>> permissions = (Map) pluginYaml.get("permissions");
            if (permissions != null) {
                for (Map.Entry<String, Map<String, Object>> entry : permissions.entrySet()) {
                    String permissionName = entry.getKey();
                    Map<String, Object> properties = entry.getValue();
                    Permission perm = makePermission(permissionName, properties);
                    this.description.addPermission(perm);
                }
            }

        } else {
            throw new IllegalStateException("ScalaPlugin nullary constructor can only be used when loaded by a " + ScalaPluginClassLoader.class.getSimpleName() + ".");
        }
    }

    private static Permission makePermission(String name, Map<String, Object> properties) {
        Permission perm = new Permission(name);

        perm.description((String) properties.get("description"));
        String def = (String) properties.get("default");
        if (def != null) perm.permissionDefault(PermissionDefault.getByName(def));
        Object children = properties.get("children");
        if (children instanceof List) {
            List kids = (List) children;
            for (Object kid : kids) perm.addChild(new Permission(kid.toString()));
        } else if (children instanceof Map) {
            Map<String, Map<String, Object>> kids = (Map) children;
            for (Map.Entry<String, Map<String, Object>> kid : kids.entrySet()) {
                perm.addChild(makePermission(kid.getKey(), kid.getValue()));
            }
        }

        return perm;
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
    public ScalaPluginClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Get the ScalaLoader's EventBus! This event bus allows you to call {@link xyz.janboerman.scalaloader.event.Event}s,
     * which will allow you to write less boilerplate.
     *
     * @return the event bus
     */
    public final EventBus getEventBus() {
        return getPluginLoader().getEventBus();
    }

    /**
     * Get the version of Scala this plugin.
     * @return the version of Scala, as per the {@link ScalaPluginClassLoader} used for this plugin
     */
    public final String getScalaVersion() {
        return getClassLoader().getScalaVersion();
    }

    /**
     * Get the compatibility-release version of Scala used by this plugin.
     * @return the compatibility release
     */
    public final ScalaRelease getScalaRelease() {
        return getClassLoader().getScalaRelease();
    }

    /**
     * Get the version of Scala that this plugin depends on.
     * At runtime a newer compatible version of Scala could be used instead.
     * @return the defined scala version
     */
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

        assert false : "ScalaPlugin defined its Scala version, but not via the @Scala or @CustomScala annotation";

        return getScalaVersion(); //fallback - to make this more robust in production
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
    protected final org.bukkit.command.PluginCommand getCommand(String name) {
        return getServer().getPluginCommand(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
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
