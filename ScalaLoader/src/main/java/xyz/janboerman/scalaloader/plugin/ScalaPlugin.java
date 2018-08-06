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
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ScalaPlugin implements Plugin, Comparable<Plugin> {

    private final ScalaPluginDescription description;
    private PluginDescriptionFile lazyDescription;
    private ScalaPluginLogger lazyLogger; //regular PluginLogger forces evaluation of our PluginDescriptionFile

    private Server server;
    private ScalaPluginLoader pluginLoader;
    private File dataFolder;
    private File file;
    private ScalaPluginClassLoader classLoader;
    private boolean naggable = true;

    private File configFile;
    private FileConfiguration config;

    private boolean enabled;

    protected ScalaPlugin(ScalaPluginDescription pluginDescription) {
        this.description = pluginDescription;
        this.description.setMain(getClass().getName());
        //TODO ideally - we want to initialize our stuff here.
        //TODO I could probably use ScalaPluginClassLoader to do that :)
    }

    //intentionally package protected
    final void init(ScalaPluginLoader pluginLoader, Server server, File dataFolder, File file, ScalaPluginClassLoader classLoader) {
        //at this point, the description is set already :)
        this.server = server;
        this.pluginLoader = pluginLoader;
        this.file = file;
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        this.configFile = new File(dataFolder, "config.yml");
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    protected ScalaPluginClassLoader getClassLoader() {
        return classLoader;
    }

    public final String getScalaVersion() {
        return getClassLoader().getScalaVersion();
    }

    public String getName() {
        return description.getName();
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    ScalaPluginDescription getScalaDescription() {
        return description;
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return lazyDescription == null ? lazyDescription = description.toPluginDescriptionFile() : lazyDescription;
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

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

    @Override
    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

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

    @Override
    public void reloadConfig() {
        //TODO load from config file in plugin directory if present - otherwise load values from the default config (included in the jar)
        config = YamlConfiguration.loadConfiguration(configFile);

        final InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream == null) {
            return;
        }

        config.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
    }

    @Override
    public ScalaPluginLoader getPluginLoader() {
        return pluginLoader;
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public boolean isNaggable() {
        return naggable;
    }

    @Override
    public void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }


    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return null;
    }

    @Override
    public Logger getLogger() {
        return lazyLogger == null ? lazyLogger = new ScalaPluginLogger(this) : lazyLogger;
    }

    protected final PluginCommand getCommand(String name) {
        return getServer().getPluginCommand(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    @Override
    public int compareTo(Plugin other) {
        if (other instanceof JavaPlugin) return 1; //java plugins are smaller.

        return getName().compareTo(other.getName());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (!(other instanceof Plugin)) return false;
        Plugin that = (Plugin) other;
        return Objects.equals(this.getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }
}
