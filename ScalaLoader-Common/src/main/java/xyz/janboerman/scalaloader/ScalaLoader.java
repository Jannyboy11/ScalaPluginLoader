package xyz.janboerman.scalaloader;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

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

import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassFile;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

/**
 * The ScalaLoader plugin's main class! ScalaLoader enables you to write plugins in Scala. Just depend on ScalaLoader,
 * extend {@link xyz.janboerman.scalaloader.plugin.ScalaPlugin}, and ScalaLoader will provide the Scala runtime classes!
 *
 * @note undocumented methods are unintended for use outside of this plugin.
 */
public final class ScalaLoader extends JavaPlugin implements IScalaLoader {

    private final Map<String, ScalaLibraryClassLoader> scalaLibraryClassLoaders = new HashMap<>();
    private final DebugSettings debugSettings = new DebugSettings(this);

    private final boolean iActuallyManagedToOverrideTheDefaultJavaPluginLoader;
    private final File scalaPluginsFolder;
    private JavaPluginLoader weFoundTheJavaPluginLoader;
    private Map<Pattern, PluginLoader> pluginLoaderMap;
    private Pattern[] javaPluginLoaderPatterns;
    private final Map<File, UnknownDependencyException> scalaPluginsWaitingOnJavaPlugins = new HashMap<>();

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

    @Override
    public boolean isPaperPlugin() {
        return false;
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
        return ScalaPluginLoader.getInstance().getScalaPlugins();
    }

    public Pattern[] getJavaPluginLoaderPatterns() {
        return javaPluginLoaderPatterns;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad() {
        //setup config
        ScalaLoaderUtils.initConfiguration(this);

        //try to load scala plugins in the same plugin load phase as java plugins
        if (iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            //don't call getServer().getPluginManager().loadPlugins(scalaPluginsFolder);
            //because at this point some javaplugins that are dependencies of scalaplugins may not have been loaded yet.

            for (File file : scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar"))) {
                try {
                    getServer().getPluginManager().loadPlugin(file);    //will now use our own ScalaPluginLoader to load the plugin
                    //TODO Problematic on Paper-405 and later (the "PaperPlugin" update: https://github.com/PaperMC/Paper/pull/8108)
                    //TODO This is an intentional breaking change by PaperMC: https://github.com/Jannyboy11/ScalaPluginLoader/issues/20#issuecomment-1462187112
                } catch (UnknownDependencyException ude) {
                    ScalaPluginLoader.getInstance().loadWhenDependenciesComeAvailable(file);
                    scalaPluginsWaitingOnJavaPlugins.put(file, ude);
                } catch (InvalidPluginException | InvalidDescriptionException e) {
                    getLogger().log(Level.SEVERE, "Could not load plugin from file: " + file.getAbsolutePath(), e);
                }
            }

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
        //ScalaLoader commands
        ScalaLoaderUtils.initCommands(this);

        //if the ".jar"-pluginloader is overridden, then check for unloaded plugins. otherwise just enable the scalaplugins now.
        if (iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            //at this point all JavaPlugins have been at least loaded (not enabled). all ScalaPlugins are loaded as well.
            //if there are still scalaplugins whose dependencies have not yet been loaded, then throw the UnknownDependencyException
            for (File file : ScalaPluginLoader.getInstance().getPluginsWaitingForDependencies()) {
                UnknownDependencyException ude = scalaPluginsWaitingOnJavaPlugins.get(file);
                assert ude != null : "found plugin file that didn't load, but hasn't got any missing dependencies: " + file.getAbsolutePath();
                throw ude;
            }

            //don't leak memory.
            scalaPluginsWaitingOnJavaPlugins.clear();
            ScalaPluginLoader.getInstance().clearPluginsWaitingForDependencies();
        } else {
            //if the injection didn't work, load scala plugins in onEnable.
            //this violates the JavaDocs of Plugin#onLoad(), but we have no other option sadly.
            //Plugin#onLoad states that onLoad of all plugins is called before onEnable is called of any other plugin.
            //..which is false in this case because ScalaLoader's onEnable is called before the onLoads of all ScalaPlugins.

            Plugin[] plugins = getServer().getPluginManager().loadPlugins(scalaPluginsFolder);
            //now while we are at it, let's enable them too.
            for (Plugin plugin : plugins) {
                getServer().getPluginManager().enablePlugin(plugin);
            }
        }

        ScalaLoaderUtils.initBStats(this);
    }

    @Override
    public void onDisable() {
        //Do we want to disable the scala plugins? I don't think so
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

}
