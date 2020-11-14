package xyz.janboerman.scalaloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bstats.bukkit.Metrics;

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

import xyz.janboerman.scalaloader.compat.Compat;
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

    private final boolean iActuallyManagedToOverrideTheDefaultJavaPluginLoader;
    private File scalaPluginsFolder;
    private JavaPluginLoader weFoundTheJavaPluginLoader;
    private Map<Pattern, PluginLoader> pluginLoaderMap;
    private Pattern[] javaPluginLoaderPatterns;

    public ScalaLoader() {
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

    public Pattern[] getJavaPluginLoaderPatterns() {
        return javaPluginLoaderPatterns;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLoad() {
        //setup config
        configure();

        //setup scala plugins folder
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }

        //try to load scala plugins in the same plugin load phase as java plugins
        if (iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            getServer().getPluginManager().loadPlugins(scalaPluginsFolder);

            //re-register the JavaPluginLoader again.
            //this should avoid the ScalaPluginLoader trying to load JavaPlugins
            for (Pattern pattern : weFoundTheJavaPluginLoader.getPluginFileFilters()) {
                pluginLoaderMap.put(pattern, weFoundTheJavaPluginLoader);
            }
        } else {
            //couldn't replace the JavaPluginLoader - just register it 'normally' here.
            getServer().getPluginManager().registerInterface(ScalaPluginLoader.class);

            //TODO why not call getServer().getPluginManager().loadPlugins(scalaPluginsFolder); here?
            //TODO store the returned Plugin[] in a field, which can then be used in our onEnable()
        }
    }

    @Override
    public void onEnable() {
        if (!iActuallyManagedToOverrideTheDefaultJavaPluginLoader) {
            //if the injection didn't work, load scala plugins in onEnable.
            //this violates the JavaDocs of Plugin#onLoad, but we have no other option sadly.
            //Plugin#onLoad states that onLoad of all plugins is called before onEnable is called of any other plugin.
            //..which is false in this case because ScalaLoader's onEnable is called before the onLoads of all ScalaPlugins.

            //TODO get from a field?
            Plugin[] plugins = getServer().getPluginManager().loadPlugins(scalaPluginsFolder);
            //now while we are at it, let's enable them too.
            for (Plugin plugin : plugins) {
                getServer().getPluginManager().enablePlugin(plugin);
            }

            //don't re-register the JavaPluginLoader because we cannot know for sure we got loaded by it.
            //we delegate all JavaPlugin-related stuff to the loader of ScalaLoader anyway.
            //TODO why can't I do this? --> getServer().getPluginManager().registerInterface(getPluginLoader().getClass());
            //TODO (also note that I still need to override the ".jar" file association with the instance)
        }


        //initialize bStats
        final int pluginId = 9150;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.DrilldownPie("declared_scala_version", () -> {
            Map<String /*compat-release version*/, Map<String /*actual version*/, Integer /*amount*/>> stats = new HashMap<>();

            for (ScalaPlugin scalaPlugin : ScalaPluginLoader.getInstance().getScalaPlugins()) {
                String scalaVersion = scalaPlugin.getDeclaredScalaVersion();
                String compatVersion;

                int lastDot = scalaVersion.lastIndexOf('.');
                if (lastDot != -1) {
                    compatVersion = "Scala " + scalaVersion.substring(0, lastDot);
                } else {
                    compatVersion = "unknown";
                }

                stats.computeIfAbsent(compatVersion, k -> new HashMap<>())
                        .compute(scalaVersion, (v, amount) -> amount == null ? 1 : amount + 1);
            }

            return stats;
        }));
        //TODO track used features of the ScalaLoader plugin -> ConfigurationSerializable api?, Event api? (could make these drilldowns!)
        //TODO track popular third-party libraries (once we include a third-party library loading api) (using advanced pie!)
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
        saveDefaultConfig();
        ConfigurationSerialization.registerClass(PluginScalaVersion.class, "ScalaVersion");
        FileConfiguration config = getConfig();
        if (!config.isList("scala-versions")) {
            getConfig().set("scala-versions", Arrays.stream(ScalaVersion.values()).map(PluginScalaVersion::fromScalaVersion).collect(Collectors.toList()));
            saveConfig();
        }
    }

    //TODO how will I inject scala library classes into the javaplugin's PluginClassLoaders?
    //TODO maybe I should relocate them after all ;o
    //TODO I think I will relocate them - but just for the javaplugins (I don't actually have to rewrite the scala library class bytes)
    //TODO I just need to change the 'name' of the class as the key in the map. I hope.
    //TODO JavaPlugins that get loaded by the ScalaPluginLoader will need actual class bytes transformations though.
    //TODO Or - I just use a custom JavaPluginLoader which has access to the ScalaLibraryClassLoaders, and can use those as parent classloaders
    //TODO Just calling addURLs using reflection won't work, I think
    //TODO What if I create a 'fake' PluginClassLoader and add it to the JavaPluginLoader that uses the ScalaPluginClassLoader as a parent? :)
    //TODO I can make use of JavaPlugin's initializing constructor that takes a non-PluginClassLoader class loader.

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
                scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), new URL[]{
                        new URL(scalaVersion.getScalaLibraryUrl()),
                        new URL(scalaVersion.getScalaReflectUrl())
                }, getClass().getClassLoader());
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
                File scalaLibraryFile = new File(versionFolder, "scala-library-" + scalaVersion + ".jar");
                File scalaReflectFile = new File(versionFolder, "scala-reflect-" + scalaVersion + ".jar");

                try {
                    scalaLibraryFile.createNewFile();
                    scalaReflectFile.createNewFile();
                } catch (IOException e) {
                    throw new ScalaPluginLoaderException("Could not create new jar files", e);
                }

                ReadableByteChannel rbc = null;
                FileOutputStream fos = null;

                try {
                    URL scalaLibraryUrl = new URL(scalaVersion.getScalaLibraryUrl());
                    rbc = Channels.newChannel(scalaLibraryUrl.openStream());
                    fos = new FileOutputStream(scalaLibraryFile);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Invalid Scala library url: " + scalaVersion.getScalaLibraryUrl(), e);
                } catch (IOException e) {
                    throw new ScalaPluginLoaderException("Could not open or close channel", e);
                } finally {
                    if (fos != null) {
                        try {
                            fos.flush();
                            fos.close();
                        } catch (IOException ignored) {}
                    }
                    if (rbc != null) {
                        try {
                            rbc.close();
                        } catch (IOException ignored) {}
                    }
                }

                //Duplicate code but Java doesn't allow nested methods. So I'm not going to bother.
                try {
                    URL scalaReflectUrl = new URL(scalaVersion.getScalaReflectUrl());
                    rbc = Channels.newChannel(scalaReflectUrl.openStream());
                    fos = new FileOutputStream(scalaReflectFile);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Invalid Scala reflect url: " + scalaVersion.getScalaReflectUrl(), e);
                } catch (IOException e) {
                    throw new ScalaPluginLoaderException("Could not open or close channel", e);
                } finally {
                    if (fos != null) {
                        try {
                            fos.flush();
                            fos.close();
                        } catch (IOException ignored) {}
                    }
                    if (rbc != null) {
                        try {
                            rbc.close();
                        } catch (IOException ignored) {}
                    }
                }

                jarFiles = new File[] {scalaLibraryFile, scalaReflectFile};
            }

            getLogger().info("Loading Scala " + scalaVersion + " libraries from disk");
            //load jar files.
            URL[] urls = new URL[jarFiles.length];
            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    urls[i] = jarFiles[i].toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Could not load Scala libraries for version " + scalaVersion + " due to a malformed URL", e);
                }
            }

            scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), urls, getClass().getClassLoader());
        }

        //cache the resolved scala library classloader
        scalaLibraryClassLoaders.put(scalaVersion.getScalaVersion(), scalaLibraryLoader);
        return scalaLibraryLoader;
    }

    /**
     * Add new versions of Scala to ScalaLoader's config.
     * @param versions the scala versions
     * @return whether a new version was added to the config
     */
    public boolean saveScalaVersionsToConfig(PluginScalaVersion... versions) {
        FileConfiguration config = getConfig();
        Set<PluginScalaVersion> scalaVersions = new LinkedHashSet<>(Arrays.asList(versions));
        boolean wasAdded = scalaVersions.addAll((List<PluginScalaVersion>) config.getList("scala-versions", Collections.emptyList()));
        config.set("scala-versions", Compat.listCopy(scalaVersions));
        saveConfig();
        return wasAdded;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "resetScalaUrl":
                if (args.length == 0) return false;
                String version = args[0];
                ScalaVersion scalaVersion;
                if ("all".equals(version)) {
                    saveScalaVersionsToConfig(Arrays.stream(ScalaVersion.values())
                            .map(PluginScalaVersion::fromScalaVersion)
                            .toArray(PluginScalaVersion[]::new));
                    sender.sendMessage(ChatColor.GREEN + "All URLs for built-in scala versions were reset!");
                } else if ((scalaVersion = ScalaVersion.fromVersionString(version)) != null) {
                    saveScalaVersionsToConfig(PluginScalaVersion.fromScalaVersion(scalaVersion));
                    sender.sendMessage(ChatColor.GREEN + "URLs for scala version " + scalaVersion.getVersion() + " were reset.");
                } else {
                    sender.sendMessage(new String[] {
                            ChatColor.RED + "Unrecognized scala version: " + version + ".",
                            ChatColor.RED + "Please use one of the following: " + onTabComplete(sender, command, label, new String[0]) + "."
                    });
                }

                return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "resetScalaUrl":
                if (args.length == 0) {
                    LinkedList<String> scalaVersions = Arrays.stream(ScalaVersion.values())
                            .map(ScalaVersion::getVersion)
                            .collect(Collectors.toCollection(LinkedList::new));

                    scalaVersions.addFirst("all");
                    return scalaVersions;
                }

                return Collections.emptyList();
        }

        return null;
    }

}
