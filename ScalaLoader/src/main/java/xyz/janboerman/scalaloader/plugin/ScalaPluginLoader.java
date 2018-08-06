package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.event.ScalaPluginDisableEvent;
import xyz.janboerman.scalaloader.event.ScalaPluginEnableEvent;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ScalaPluginLoader implements PluginLoader {

    private final Server server;
    private ScalaLoader lazyScalaLoader;
    private PluginLoader lazyJavaPluginLoader;

    private final Set<PluginScalaVersion> scalaVersions = new HashSet<>();
    private final Pattern[] pluginFileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    private final Map<String, ScalaLibraryClassLoader> scalaVersionParentLoaders = new HashMap<>();

    //TODO make it so that scala plugin's can access eachother's classes :)
    //Map<ScalaVersion, Map<PluginName, Class<?>>>
    private final Map<String, Map<String, Class<?>>> sharedScalaPluginClasses = new HashMap<>();

    private final Map<String, ScalaPlugin> scalaPlugins = new HashMap<>();
    private final Map<File, ScalaPlugin> scalaPluginsByFile = new HashMap<>();

    //TODO do I need to keep track of the loaders?
    //private final List<ScalaPluginClassLoader> loaders = new CopyOnWriteArrayList<>();

    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");
    }

    private ScalaLoader getScalaLoader() {
        return lazyScalaLoader == null ? lazyScalaLoader = JavaPlugin.getPlugin(ScalaLoader.class) : lazyScalaLoader;
    }

    private PluginLoader getJavaPluginLoader() {
        return lazyJavaPluginLoader == null ? lazyJavaPluginLoader = getScalaLoader().getPluginLoader() : lazyJavaPluginLoader;
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
        //filled optionals are smaller then empty optionals.
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(optional -> !optional.isPresent());
        //smaller package hierarchy = smaller string
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        //smaller element = better main class candidate!
        Comparator<DescriptionScanner> descriptionComparator = Comparator.nullsLast(Comparator /* get rid of null descriptors */
                .<DescriptionScanner, Optional<?>>comparing(DescriptionScanner::getMainClass, optionalComparator /* get rid of descriptions without a main class */)
                .thenComparing(DescriptionScanner::extendsScalaPlugin /* classes that extend ScalaPlugin directly are less likely to be the best candidate. */)
                .thenComparing(descriptionScanner -> descriptionScanner.getMainClass().get() /* never fails because empty optionals are larger anyway :) */, packageComparator)
                .thenComparing(descriptionScanner -> descriptionScanner.getMainClass().get() /* fallback - just compare the class strings */));

        try {
            DescriptionScanner mainClassCandidate = null;

            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entryEnumeration = jarFile.entries();
            while (entryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = entryEnumeration.nextElement();
                if (jarEntry.getName().endsWith(".class")) {

                    InputStream classBytesInputStream = jarFile.getInputStream(jarEntry);

                    DescriptionScanner descriptionScanner = new DescriptionScanner();
                    ClassReader classReader = new ClassReader(classBytesInputStream);
                    classReader.accept(descriptionScanner, 0);

                    //Emit a warning when the class does extend ScalaPlugin, but does not have de @Scala or @CustomScala annotation
                    if (descriptionScanner.extendsScalaPlugin() && !descriptionScanner.getScalaVersion().isPresent()) {
                        getScalaLoader().getLogger().warning("Class " + jarEntry.getName() + " extends ScalaPlugin but does not have the @Scala or @CustomScala annotation.");
                    }

                    //TODO in the future I could transform the class to use the the relocated scala library?

                    //The smallest element is the best candidate!
                    mainClassCandidate = BinaryOperator.minBy(descriptionComparator).apply(mainClassCandidate, descriptionScanner);

                } else if (jarEntry.getName().equals("plugin.yml")) {
                    getScalaLoader().getLogger().warning("Found plugin.yml in scala plugin " + file.getName() + ". Ignoring..");
                    //TODO should probably inspect the plugin yaml. if it contains a main class we should delegate to the JavaPluginLoader
                    //TODO if it doesn't contain a main class then we add the 'fields' of the plugin yaml to the ScalaPluginDescription.
                }
            } //end while - no more JarEntries

            if (mainClassCandidate == null || !mainClassCandidate.getMainClass().isPresent()) {
                getScalaLoader().getLogger().warning("Could not find main class in file " + file.getName() + ". Did you annotate your main class with @Scala?");
                getScalaLoader().getLogger().warning("Delegating to JavaPluginLoader...");
                return getJavaPluginLoader().getPluginDescription(file);
            }

            //null for unknown api version
            ApiVersion apiVersion = mainClassCandidate.getBukkitApiVersion().orElse(null);

            //TODO transform bytes if necessary based on apiVersion

            PluginScalaVersion scalaVersion = mainClassCandidate.getScalaVersion().get();

            try {
                //load scala version if not already present
                ScalaLibraryClassLoader scalaLibraryClassLoader = loadOrGetScalaVersion(scalaVersion);
                //create plugin classloader using the resolved scala classloader
                ScalaPluginClassLoader scalaPluginClassLoader = new ScalaPluginClassLoader(new URL[]{file.toURI().toURL()}, scalaLibraryClassLoader);

                //create our plugin
                final String mainClass = mainClassCandidate.getMainClass().get();
                Class<? extends ScalaPlugin> pluginMainClass = (Class<? extends ScalaPlugin>) Class.forName(mainClass, true, scalaPluginClassLoader);
                ScalaPlugin plugin = createPluginInstance(pluginMainClass);

                //api version and main class are detected from the annotation
                plugin.getScalaDescription().setApiVersion(apiVersion == null ? null : apiVersion.getVersionString());
                plugin.getScalaDescription().setMain(mainClass); //required per PluginDescriptionFile constructor - not actually used.

                //just init, don't load yet.
                plugin.init(this, server, new File(file.getParent(), plugin.getName()), file, scalaPluginClassLoader);
                if (scalaPlugins.putIfAbsent(plugin.getName().toLowerCase(), plugin) != null) {
                    throw new InvalidDescriptionException("Duplicate plugin names found: " + plugin.getName());
                }

                //be sure to cache the plugin - later in loadPlugin we just return the cached instance!
                scalaPluginsByFile.put(file, plugin);
                return plugin.getDescription();

            } catch (ClassNotFoundException e) {
                throw new InvalidDescriptionException(e, "Could find the class that was found the main class");
            } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
                throw new InvalidDescriptionException(e,
                        "Your plugin's constructor and/or initializers tried to access classes that were not yet loaded. " +
                                "Try to move stuff over to onLoad() and onEnable().");
            } catch (ScalaPluginLoaderException e) {
                throw new InvalidDescriptionException(e, "Failed to create scala library classloader");
            }

        } catch (IOException e) {
            throw new InvalidDescriptionException(e, "Could not read jar file " + file.getName());
        }
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
        ScalaPlugin plugin = scalaPluginsByFile.get(file);
        if (plugin == null) throw new InvalidPluginException("File " + file.getName() + " does not contain a ScalaPlugin");

        for (String dependency : plugin.getDescription().getDepend()) {
            boolean dependencyFound = server.getPluginManager().getPlugin(dependency) != null;
            if (!dependencyFound) {
                throw new UnknownDependencyException("Dependency " + dependency + " not found while loading plugin " + plugin.getName());
            }
        }

        plugin.getLogger().info("Loading " + plugin.getDescription().getFullName());
        plugin.onLoad();
        return plugin;
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
        if (plugin instanceof JavaPlugin) {
          getJavaPluginLoader().enablePlugin(plugin);
        } else if (plugin instanceof ScalaPlugin) {
            ScalaPlugin scalaPlugin = (ScalaPlugin) plugin;
            if (scalaPlugin.isEnabled()) return;

            ScalaPluginEnableEvent event = new ScalaPluginEnableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            plugin.getLogger().info("Enabling " + plugin.getDescription().getFullName());
            scalaPlugin.setEnabled(true);
            scalaPlugin.onEnable();
        } else {
            throw new IllegalArgumentException("ScalaPluginLoader can only enable " + ScalaPlugin.class.getSimpleName() + "s");
        }
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
        if (plugin instanceof JavaPlugin) {
            getJavaPluginLoader().disablePlugin(plugin);
        } else if (plugin instanceof ScalaPlugin) {
            ScalaPlugin scalaPlugin = (ScalaPlugin) plugin;
            if (!scalaPlugin.isEnabled()) return;

            ScalaPluginDisableEvent event = new ScalaPluginDisableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            plugin.getLogger().info("Disabling " + plugin.getDescription().getFullName());
            scalaPlugin.onDisable();
            scalaPlugin.setEnabled(false);
        }
    }

    /**
     * Returns a list of all filename filters expected by this PluginLoader
     *
     * @return The filters
     */
    @Override
    public Pattern[] getPluginFileFilters() {
        Pattern[] patterns = getScalaLoader().getJavaPluginLoaderPattners();
        if (patterns != null) return patterns;
        return pluginFileFilters.clone();
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
        return getJavaPluginLoader().createRegisteredListeners(listener, plugin);
    }

    /**
     * Tries to get the plugin instance from the scala plugin class.
     * This method is able to get the static instances from scala `object`s,
     * as well as it is able to create plugins using their public NoArgsConstructors.
     * @param clazz the plugin class
     * @param <P> the plugin type
     * @return the plugin's instance.
     * @throws ScalaPluginLoaderException
     */
    private <P extends ScalaPlugin> P createPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
        boolean weFoundAScalaSingletonObject = false;

        if (clazz.getName().endsWith("$")) {
            weFoundAScalaSingletonObject = true;

            //we found a scala singleton object.
            //the instance is already present in the MODULE$ field when this class is loaded.

            try {
                Field field = clazz.getField("MODULE$");
                Object pluginInstance = field.get(null);

                getScalaLoader().getLogger().info("DEBUG got plugin instance for class " + clazz.getName() + " from the static MODULE$ field.");

                return clazz.cast(pluginInstance);
            } catch (NoSuchFieldException e) {
                weFoundAScalaSingletonObject = false; //back paddle!
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Couldn't access MODULE$ field in class " + clazz.getName(), e);
            }
        }

        if (!weFoundAScalaSingletonObject) /*IntelliJ your code inspection is lying. this is not an else-if.*/ {
            //we found are a regular class.
            //it should have a NoArgsConstructor.

            try {
                Constructor ctr = clazz.getConstructor();
                Object pluginInstance = ctr.newInstance();

                getScalaLoader().getLogger().info("DEBUG got plugin instance for class " + clazz.getName() + " form the NoArgsConstructor.");

                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Could not access the NoArgsConstructor of " + clazz.getName() + ", please make it public", e);
            } catch (InvocationTargetException e) {
                throw new ScalaPluginLoaderException("Error instantiating class " + clazz.getName() + ", its constructor threw something at us", e);
            } catch (NoSuchMethodException e) {
                throw new ScalaPluginLoaderException("Could not find NoArgsConstructor in class " + clazz.getName(), e);
            } catch (InstantiationException e) {
                throw new ScalaPluginLoaderException("Could not instantiate class " + clazz.getName(), e);
            }
        }

        else return null;
    }


    private ScalaLibraryClassLoader loadOrGetScalaVersion(PluginScalaVersion scalaVersion) throws ScalaPluginLoaderException {
        //try to get from cache
        ScalaLibraryClassLoader scalaLibraryLoader = scalaVersionParentLoaders.get(scalaVersion.getScalaVersion());
        if (scalaLibraryLoader != null) return scalaLibraryLoader;

        if (!getScalaLoader().downloadScalaJarFiles()) {
            //load classes over the network
            getScalaLoader().getLogger().info("Loading scala libraries from over the network");
            try {
                scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), new URL[]{
                        new URL(scalaVersion.getScalaLibraryUrl()),
                        new URL(scalaVersion.getScalaReflectUrl())
                }, lazyScalaLoader.getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw new ScalaPluginLoaderException("Could not load scala libraries for version " + scalaVersion + " due to a malformed URL", e);
            }
        } else {
            //check if downloaded already (if not, do download)
            //then load classes from the downloaded jar

            File scalaLibsFolder = new File(getScalaLoader().getDataFolder(), "scalalibraries");
            File versionFolder = new File(scalaLibsFolder, scalaVersion.getScalaVersion());
            versionFolder.mkdirs();

            File[] jarFiles = versionFolder.listFiles((dir, name) -> name.endsWith(".jar"));

            if (jarFiles.length == 0) {
                //no jar files found - download dem files
                getScalaLoader().getLogger().info("Tried to load scala libraries from disk, but they were not present. Downloading...");
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
                    throw new ScalaPluginLoaderException("Invalid scala library url: " + scalaVersion.getScalaLibraryUrl(), e);
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

                try {
                    URL scalaReflectUrl = new URL(scalaVersion.getScalaReflectUrl());
                    rbc = Channels.newChannel(scalaReflectUrl.openStream());
                    fos = new FileOutputStream(scalaReflectFile);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Invalid scala reflect url: " + scalaVersion.getScalaReflectUrl(), e);
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

            getScalaLoader().getLogger().info("Loading scala libraries from disk");
            //load jar files.
            URL[] urls = new URL[jarFiles.length];
            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    urls[i] = jarFiles[i].toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Could not load scala libraries for version " + scalaVersion + " due to a malformed URL", e);
                }
            }

            scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), urls, lazyScalaLoader.getClass().getClassLoader());
        }

        //cache the resolved scala library classloader
        scalaVersionParentLoaders.put(scalaVersion.getScalaVersion(), scalaLibraryLoader);
        return scalaLibraryLoader;
    }

}

