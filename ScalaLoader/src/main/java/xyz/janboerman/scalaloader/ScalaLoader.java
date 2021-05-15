package xyz.janboerman.scalaloader;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import xyz.janboerman.scalaloader.commands.DumpClass;
import xyz.janboerman.scalaloader.commands.ListScalaPlugins;
import xyz.janboerman.scalaloader.commands.ResetScalaUrls;
import xyz.janboerman.scalaloader.commands.SetDebug;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.NumericRange;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

/**
 * The ScalaLoader plugin's main class! ScalaLoader enables you to write plugins in Scala. Just depend on ScalaLoader,
 * extend {@link xyz.janboerman.scalaloader.plugin.ScalaPlugin}, and ScalaLoader will provide the Scala runtime classes!
 *
 * @note undocumented methods are unintended for use outside of this plugin.
 */
public final class ScalaLoader extends JavaPlugin {

    private final Map<String, ScalaLibraryClassLoader> scalaLibraryClassLoaders = new HashMap<>();
    private final DebugSettings debugSettings = new DebugSettings(this);

    private final boolean iActuallyManagedToOverrideTheDefaultJavaPluginLoader;
    private final File scalaPluginsFolder;
    private JavaPluginLoader weFoundTheJavaPluginLoader;
    private Map<Pattern, PluginLoader> pluginLoaderMap;
    private Pattern[] javaPluginLoaderPatterns;

    public ScalaLoader() {
        //setup scala plugins folder (can't do this in initializer yet because the super() constructor initializes the dataFolder)
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }

        //dirty hack to override the previous pattern.

        boolean myHackWorked; //try to get hold of the pattern. k thnx

        Server server = Bukkit.getServer();
        try {
            SimplePluginManager pluginManager = (SimplePluginManager) server.getPluginManager();
            Field fileAssociationsField = pluginManager.getClass().getDeclaredField("fileAssociations");
            fileAssociationsField.setAccessible(true);
            pluginLoaderMap = (Map) fileAssociationsField.get(pluginManager);
            Iterator<Map.Entry<Pattern, PluginLoader>> iterator = pluginLoaderMap.entrySet().iterator();

            ScalaPluginLoader scalaPluginLoader = new ScalaPluginLoader(this);

            while (iterator.hasNext()) {
                Map.Entry<Pattern, PluginLoader> entry = iterator.next();
                if (entry.getValue() instanceof JavaPluginLoader) {
                    weFoundTheJavaPluginLoader = (JavaPluginLoader) entry.getValue();
                    javaPluginLoaderPatterns = weFoundTheJavaPluginLoader.getPluginFileFilters();
                    entry.setValue(scalaPluginLoader);
                }
            }

            myHackWorked = true;
        } catch (Throwable iGiveUp) {
            myHackWorked = false;
            getLogger().log(Level.WARNING, "Error while trying to replace the standard JavaPluginLoader.", iGiveUp);
        }

        iActuallyManagedToOverrideTheDefaultJavaPluginLoader = myHackWorked;
        if (iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            getLogger().info("Managed to override the default .jar file association!");
        } else {
            getLogger().info("Did not manage to override the default .jar file association. Plugins may not load in the expected order.");
        }
    }

    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    public File getScalaPluginsFolder() {
        return scalaPluginsFolder;
    }

    public Pattern[] getJavaPluginLoaderPatterns() {
        return javaPluginLoaderPatterns;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad() {
        //setup config
        configure();

        //try to load scala plugins in the same plugin load phase as java plugins
        if (iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            getServer().getPluginManager().loadPlugins(scalaPluginsFolder);

            //don't re-register the JavaPluginLoader again.
            //doing so would break hot-reloading of ScalaPlugins
        } else {
            //couldn't replace the JavaPluginLoader - just register it 'normally' here.
            getServer().getPluginManager().registerInterface(ScalaPluginLoader.class);
            //if we would call getServer().getPluginManager().loadPlugins(scalaPluginsFolder); here then ScalaPlugins wouldn't ever be able to depend on JavaPlugins.
            //because the JavaPlugin in question might not have been loaded yet by the JavaPluginLoader.
        }
    }

    @Override
    public void onEnable() {
        if (!iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            //if the injection didn't work, load scala plugins in onEnable.
            //this violates the JavaDocs of Plugin#onLoad, but we have no other option sadly.
            //Plugin#onLoad states that onLoad of all plugins is called before onEnable is called of any other plugin.
            //..which is false in this case because ScalaLoader's onEnable is called before the onLoads of all ScalaPlugins.

            Plugin[] plugins = getServer().getPluginManager().loadPlugins(scalaPluginsFolder);
            //now while we are at it, let's enable them too.
            for (Plugin plugin : plugins) {
                getServer().getPluginManager().enablePlugin(plugin);
            }
        }

        //initialize bStats
        final int pluginId = 9150;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new DrilldownPie("declared_scala_version", () -> {
            Map<String /*compat-release version*/, Map<String /*actual version*/, Integer /*amount*/>> stats = new HashMap<>();

            for (ScalaPlugin scalaPlugin : ScalaPluginLoader.getInstance().getScalaPlugins()) {
                String scalaVersion = scalaPlugin.getDeclaredScalaVersion();
                String compatVersion = ScalaRelease.fromScalaVersion(scalaVersion).getCompatVersion();

                stats.computeIfAbsent(compatVersion, k -> new HashMap<>())
                        .compute(scalaVersion, (v, amount) -> amount == null ? 1 : amount + 1);
            }

            return stats;
        }));
        //TODO track used features of the ScalaLoader plugin -> ConfigurationSerializable api?, Event api? (could make these drilldowns!)
        //TODO track popular third-party libraries (once we include a third-party library loading api) (using advanced pie!)

        //ScalaLoader commands
        getCommand("resetScalaUrls").setExecutor(new ResetScalaUrls(this));
        getCommand("dumpClass").setExecutor(new DumpClass(this));
        getCommand("setDebug").setExecutor(new SetDebug(getDebugSettings()));
        getCommand("listScalaPlugins").setExecutor(new ListScalaPlugins());

    }

    @Override
    public void onDisable() {
        //Do we want to disable the scala plugins? I don't think so
    }

    public void runInMainThread(Runnable runnable) {
        Server server = getServer();

        if (server.isPrimaryThread()) {
            runnable.run();
        } else {
            server.getScheduler().runTask(this, runnable);
        }
    }

    private void configure() {
        //ScalaLoader config stuff
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(PluginScalaVersion.class, "ScalaVersion");
        FileConfiguration config = getConfig();
        if (!config.isList("scala-versions")) {
            getConfig().set("scala-versions", Arrays.stream(ScalaVersion.values()).map(PluginScalaVersion::fromScalaVersion).collect(Collectors.toList()));
            saveConfig();
        }

        //ScalaPlugin config stuff
        NumericRange.registerWithConfigurationSerialization();
    }

    private boolean downloadScalaJarFiles() {
        return getConfig().getBoolean("load-libraries-from-disk", true);
    }

    /**
     * Get a (fresh or cached) {@link ScalaLibraryClassLoader} that loads standard library classes from a specific Scala version.
     * The classloader can either load classes from over the network directly, or use downloaded library archives (jar files).
     * @param scalaVersion the scala version
     * @return the class loader
     * @throws ScalaPluginLoaderException if a url is malformed.
     */
    public ScalaLibraryClassLoader loadOrGetScalaVersion(PluginScalaVersion scalaVersion) throws ScalaPluginLoaderException {
        //try to get from cache
        ScalaLibraryClassLoader scalaLibraryLoader = scalaLibraryClassLoaders.get(scalaVersion.getScalaVersion());
        if (scalaLibraryLoader != null) return scalaLibraryLoader;

        if (!downloadScalaJarFiles()) {
            //load classes over the network
            getLogger().info("Loading Scala " + scalaVersion + " libraries from over the network");
            try {
                Map<String, String> urlMap = scalaVersion.getUrls();
                URL[] urls = new URL[urlMap.size()];
                int i = 0;
                for (String url : urlMap.values()) {
                    urls[i++] = new URL(url);
                }
                scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), urls, getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw new ScalaPluginLoaderException("Could not load scala libraries for version " + scalaVersion + " due to a malformed URL", e);
            }
        } else {
            //check if downloaded already (if not, do download)
            //then load classes from the downloaded jar

            File scalaLibsFolder = new File(getDataFolder(), "scalalibraries");
            File versionFolder = new File(scalaLibsFolder, scalaVersion.getScalaVersion());
            versionFolder.mkdirs();

            File[] jarFiles = versionFolder.listFiles((dir, name) -> name.endsWith(".jar"));

            if (jarFiles.length == 0) {
                //no jar files found - download dem files
                getLogger().info("Tried to load Scala " + scalaVersion + " libraries from disk, but they were not present. Downloading...");

                Map<String, String> urlMap = scalaVersion.getUrls();
                jarFiles = new File[urlMap.size()];
                int i = 0;
                for (Map.Entry<String, String> entry : urlMap.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    String fileName;
                    if (key.endsWith("-url")) {
                        fileName = key.substring(0, key.length() - 3) + scalaVersion.getScalaVersion() + ".jar";
                    } else if (key.endsWith(".jar")) {
                        fileName = key;
                    } else {
                        fileName = key + "-" + scalaVersion.getScalaVersion() + ".jar";
                    }

                    File scalaRuntimeJarFile = new File(versionFolder, fileName);

                    try {
                        scalaRuntimeJarFile.createNewFile();
                    } catch (IOException e) {
                        throw new ScalaPluginLoaderException("Could not create new jar file", e);
                    }

                    ReadableByteChannel rbc = null;
                    FileOutputStream fos = null;

                    try {
                        URL url = new URL(value);
                        downloadFile(url, scalaRuntimeJarFile);
                    } catch (MalformedURLException e) {
                        throw new ScalaPluginLoaderException("Invalid url for key: " + key, e);
                    } catch (IOException e) {
                        throw new ScalaPluginLoaderException("Could not open or close channel", e);
                    }

                    jarFiles[i++] = scalaRuntimeJarFile;
                }
            }

            getLogger().info("Loading Scala " + scalaVersion.getScalaVersion() + " libraries from disk");
            //load jar files.
            URL[] urls = new URL[jarFiles.length];
            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    urls[i] = jarFiles[i].toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Could not load Scala libraries for version " + scalaVersion.getScalaVersion() + " due to a malformed URL", e);
                }
            }

            scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), urls, getClass().getClassLoader());
        }

        //cache the resolved scala library classloader
        scalaLibraryClassLoaders.put(scalaVersion.getScalaVersion(), scalaLibraryLoader);
        return scalaLibraryLoader;
    }

    private static void downloadFile(URL inputResourceLocation, File outputFile) throws IOException {
        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        try {
            rbc = Channels.newChannel(inputResourceLocation.openStream());
            fos = new FileOutputStream(outputFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } finally {
            if (rbc != null) try { rbc.close(); } catch (IOException e) { e.printStackTrace(); }
            if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }


    /**
     * Add new versions of Scala to ScalaLoader's config.
     * @param versions the scala versions
     * @return whether a new version was added to the config
     */
    public boolean saveScalaVersionsToConfig(PluginScalaVersion... versions) {
        FileConfiguration config = getConfig();
        Set<PluginScalaVersion> scalaVersions = new LinkedHashSet<>(Compat.listOf(versions));
        boolean wasAdded = scalaVersions.addAll((List<PluginScalaVersion>) config.getList("scala-versions", Compat.emptyList()));
        config.set("scala-versions", Compat.listCopy(scalaVersions));
        saveConfig();
        return wasAdded;
    }

}
