package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions;
import xyz.janboerman.scalaloader.configurationserializable.transform.AddVariantTransformer;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanResult;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanner;
import xyz.janboerman.scalaloader.configurationserializable.transform.PluginTransformer;
import xyz.janboerman.scalaloader.dependency.PluginYamlLibraryLoader;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.event.plugin.ScalaPluginDisableEvent;
import xyz.janboerman.scalaloader.event.plugin.ScalaPluginEnableEvent;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ScalaPluginLoader implements PluginLoader {

    private static ScalaPluginLoader INSTANCE;

    private final Server server;
    private ScalaLoader lazyScalaLoader;
    private PluginLoader lazyJavaPluginLoader;

    private static final Pattern[] pluginFileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    private final ConcurrentMap<ScalaRelease, ConcurrentMap<String, Class<?>>> sharedScalaPluginClasses = new ConcurrentHashMap<>();
    private final ConcurrentMap<ScalaRelease, CopyOnWriteArrayList<ScalaPluginClassLoader>> sharedScalaPluginClassLoaders = new ConcurrentHashMap<>();
    private final ScalaCompatMap scalaCompatMap = new ScalaCompatMap();
    private final Map<File, PluginJarScanResult> preScannedPluginJars = new HashMap<>();

    private final Map<String, ScalaPlugin> scalaPlugins = new HashMap<>();
    private final Map<Path, ScalaPlugin> scalaPluginsByAbsolutePath = new HashMap<>();
    private final Collection<ScalaPlugin> scalaPluginsView = Collections.unmodifiableCollection(scalaPlugins.values());

    private EventBus eventBus;
    private PluginYamlLibraryLoader pluginYamlLibraryLoader;

    private static final Comparator<DescriptionScanner> descriptionComparator;
    static {
        //filled optionals are smaller then empty optionals.
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(optional -> !optional.isPresent());
        //smaller package hierarchy = smaller string
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        //smaller element = better main class candidate!
        descriptionComparator = Comparator.nullsLast(Comparator /* get rid of null descriptors */
                .<DescriptionScanner, Optional<?>>comparing(DescriptionScanner::getMainClass, optionalComparator /* get rid of descriptions without a main class */)
                .thenComparing(DescriptionScanner::extendsScalaPlugin /* classes that extend ScalaPlugin directly are less likely to be the best candidate. */)
                .thenComparing(DescriptionScanner::getClassName, packageComparator /* less deeply nested class = better candidate*/)
                .thenComparing(DescriptionScanner::getClassName /* fallback - just compare the class name strings */));
    }

    public ScalaPluginLoader(ScalaLoader scalaLoader) {
        this.lazyScalaLoader = scalaLoader;
        this.server = scalaLoader.getServer();
        init();
    }

    /**
     * Per PluginLoader API, the constructor has only one parameter: the Server.
     * @param server the server.
     */
    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");
        init();
    }


    @SuppressWarnings("deprecation")
    private void init() {
        //Static abuse but I cannot find a more elegant way to do this.
        if (INSTANCE == null) {
            INSTANCE = this;
        } else {
            throw new IllegalStateException("The ScalaPluginLoader can only be instantiated once!");
        }

        //set fields
        ScalaLoader scalaLoader = getScalaLoader();
        this.eventBus = new EventBus(server.getPluginManager());
        this.pluginYamlLibraryLoader = new PluginYamlLibraryLoader(scalaLoader.getLogger(), new File(scalaLoader.getDataFolder(), "libraries"));

        //pre-scan plugins so that scala-versions can be detected BEFORE the main classes are instantiated!
        //this is just a best-effort thing because the PluginManager may load plugins at arbitrary points in time.
        //TODO I don't think this is necessary anymore when plugins aren't instantiated anymore when their 'description' is read.

        File pluginsFolder = scalaLoader.getScalaPluginsFolder();
        if (pluginsFolder.exists()) {
            File[] pluginJarFiles = pluginsFolder.listFiles((dir, name) -> Arrays.stream(getPluginFileFilters())
                    .anyMatch(pattern -> pattern.matcher(name).find()));
            if (pluginJarFiles != null) {
                for (File pluginJarFile : pluginJarFiles) {
                    try {
                        PluginJarScanResult scanResult = scanJar(pluginJarFile);
                        if (!scanResult.isJavaPluginExplicitly) {
                            preScannedPluginJars.put(pluginJarFile, scanResult);
                            scanResult.mainClassCandidate.getScalaVersion().ifPresent(scalaCompatMap::add);
                        }
                    } catch (IOException e) {
                        getScalaLoader().getLogger().log(Level.SEVERE, "Could not read plugin jar file: " + pluginJarFile.getName(), e);
                        //not much else we can do here, throwing an exception would be inappropriate.
                    }
                }
            }
        }
    }

    /**
     * Get the instance that was created when this ScalaPluginLoader was constructed.
     * @apiNote if you call this method from your own plugin, your plugin must have a dependency on {@link ScalaLoader}.
     * @return the instance that was created either by bukkit's {@link PluginManager} or by {@link ScalaLoader},
     *         or null if no ScalaPluginLoader was constructed yet.
     */
    public static ScalaPluginLoader getInstance() {
        return INSTANCE;
    }

    ScalaLoader getScalaLoader() {
        return lazyScalaLoader == null ? lazyScalaLoader = JavaPlugin.getPlugin(ScalaLoader.class) : lazyScalaLoader;
    }

    PluginLoader getJavaPluginLoader() {
        return lazyJavaPluginLoader == null ? lazyJavaPluginLoader = getScalaLoader().getPluginLoader() : lazyJavaPluginLoader;
    }

    /**
     * Get the event bus.
     *
     * @apiNote This method is provided for JavaPlugins. ScalaPlugin's can use {@link ScalaPlugin#getEventBus()} instead.
     * @return the event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Get the ScalaPlugins.
     *
     * @return an unmodifiable collection containing all ScalaPlugins.
     */
    public Collection<ScalaPlugin> getScalaPlugins() {
        return scalaPluginsView;
    }

    /**
     * Get a set class names to debug-print when they are loaded.
     *
     * @return the set of class names
     */
    Set<String> debugClassNames() {
        return getScalaLoader().getDebugSettings().debugClassLoads();
    }


    private static PluginJarScanResult scanJar(File file) throws IOException {
        PluginJarScanResult result = new PluginJarScanResult();

        Logger logger = getInstance().getScalaLoader().getLogger();
        logger.info("Reading ScalaPlugin file: " + file.getName() + "..");

        Map<String, Object> pluginYamlData = null;

        JarFile jarFile = Compat.jarFile(file);


        {   //short-circuit: check whether the Plugin extends JavaPlugin
            JarEntry pluginYmlEntry = jarFile.getJarEntry("plugin.yml");
            //If it contains a main class and it doesn't extend ScalaPlugin directly we should try to delegate to the JavaPluginLoader
            //If it doesn't contain a main class then we add the 'fields' of the plugin yaml to the ScalaPluginDescription.

            if (pluginYmlEntry != null) {
                Yaml yaml = new Yaml();
                InputStream pluginYamlInputStream = jarFile.getInputStream(pluginYmlEntry);
                pluginYamlData = (Map<String, Object>) yaml.loadAs(pluginYamlInputStream, Map.class);

                if (pluginYamlData.containsKey("main")) {
                    String yamlDefinedMainClassName = pluginYamlData.get("main").toString();
                    String mainClassEntry = yamlDefinedMainClassName + ".class";
                    JarEntry pluginYamlDefinedMainJarEntry = jarFile.getJarEntry(mainClassEntry);

                    if (pluginYamlDefinedMainJarEntry != null) {
                        InputStream classBytesInputStream = jarFile.getInputStream(pluginYamlDefinedMainJarEntry);
                        DescriptionScanner yamlMainScanner = new DescriptionScanner(classBytesInputStream); //use a less powerful scanner implementation here?

                        if (yamlMainScanner.extendsJavaPlugin()) {
                            result.isJavaPluginExplicitly = true;
                        }
                    }
                }
            }
        }

        if (!result.isJavaPluginExplicitly) {
            TransformerRegistry transformerRegistry = new TransformerRegistry();
            DescriptionScanner mainClassCandidate = null;
            if (pluginYamlData == null) pluginYamlData = new HashMap<>();

            Enumeration<JarEntry> entryEnumeration = jarFile.entries();
            while (entryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = entryEnumeration.nextElement();
                if (jarEntry.getName().endsWith(".class")) {

                    InputStream classBytesInputStream = jarFile.getInputStream(jarEntry);
                    byte[] classBytes = Compat.readAllBytes(classBytesInputStream);

                    //scan class to see if this class is the best candidate for the main class
                    DescriptionScanner descriptionScanner = new DescriptionScanner(classBytes);

                    //Emit a warning when the class does extend ScalaPlugin, but does not have de @Scala or @CustomScala annotation
                    if (descriptionScanner.extendsScalaPlugin() && !descriptionScanner.getScalaVersion().isPresent()) {
                        logger.warning("Class " + jarEntry.getName() + " extends ScalaPlugin but does not have the @Scala or @CustomScala annotation.");
                        //this is just a soft warning and not a hard error because this class itself may be subclassed by the actual main class
                    }

                    //The smallest element is the best candidate!
                    mainClassCandidate = BinaryOperator.minBy(descriptionComparator).apply(mainClassCandidate, descriptionScanner);

                    //scan class to see if this class is configurationserializable and wants to register a plugin transformer:
                    final GlobalScanResult configSerResult = new GlobalScanner().scan(new ClassReader(classBytes));
                    PluginTransformer.addTo(transformerRegistry, configSerResult);
                    AddVariantTransformer.addTo(transformerRegistry, configSerResult);
                }
            }

            result.mainClassCandidate = mainClassCandidate;
            result.transformerRegistry = transformerRegistry;
            result.pluginYaml = pluginYamlData;
        }

        return result;
    }


    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        final Path path = file.toPath().toAbsolutePath();
        final ScalaPlugin alreadyPresent = scalaPluginsByAbsolutePath.get(path);
        if (alreadyPresent != null) return alreadyPresent.getDescription();

        PluginJarScanResult jarScanResult = preScannedPluginJars.get(file);
        if (jarScanResult == null) {
            try {
                jarScanResult = scanJar(file);
                if (jarScanResult.isJavaPluginExplicitly) {
                    return getJavaPluginLoader().getPluginDescription(file);
                }
            } catch (IOException e) {
                throw new InvalidDescriptionException(e, "Could not read jar file " + file.getName());
            }
        }

        DescriptionScanner mainClassCandidate = jarScanResult.mainClassCandidate;
        Map<String, Object> pluginYamlData = jarScanResult.pluginYaml;
        TransformerRegistry transformerRegistry = jarScanResult.transformerRegistry;

        if (mainClassCandidate == null || !mainClassCandidate.getMainClass().isPresent()) {
            getScalaLoader().getLogger().warning("Could not find main class in file " + file.getName() + ". Did you annotate your main class with @Scala and is it public?");
            getScalaLoader().getLogger().warning("Delegating to JavaPluginLoader...");
            return getJavaPluginLoader().getPluginDescription(file);
        }

        assert mainClassCandidate.getScalaVersion().isPresent() : "Plugin main class is present without a PluginScalaVersion o.0";

        //assume latest if unspecified
        ApiVersion apiVersion = mainClassCandidate.getBukkitApiVersion().orElseGet(ApiVersion::latest);

        PluginScalaVersion scalaVersion = mainClassCandidate.getScalaVersion().get();

        try {
            final String mainClass = mainClassCandidate.getMainClass().get();

            //get the latest compatible scala version - best effort
            scalaVersion = scalaCompatMap.getLatestVersion(scalaVersion);
            //load scala version if not already present
            ScalaLibraryClassLoader scalaLibraryClassLoader = getScalaLoader().loadOrGetScalaVersion(scalaVersion);

            //download or get the maven dependencies defined in the plugin.yml
            Collection<File> dependencies = pluginYamlLibraryLoader.getJarFiles(pluginYamlData);
            //TODO scan dependencies?

            //create plugin classloader using the resolved scala classloader
            ScalaPluginClassLoader scalaPluginClassLoader =
                    new ScalaPluginClassLoader(this, new URL[] { file.toURI().toURL() }, scalaLibraryClassLoader,
                            server, pluginYamlData, file, apiVersion, mainClass, transformerRegistry, dependencies);
            sharedScalaPluginClassLoaders.computeIfAbsent(scalaVersion.getCompatRelease(), v -> new CopyOnWriteArrayList<>()).add(scalaPluginClassLoader);

            //get the ScalaPlugin from the class loader!
            ScalaPlugin plugin = scalaPluginClassLoader.getPlugin();

            if (scalaPlugins.putIfAbsent(plugin.getName().toLowerCase(), plugin) != null) {
                throw new InvalidDescriptionException("Duplicate plugin names found: " + plugin.getName());
            }

            //be sure to cache the plugin - later in #loadPlugin(File) we just return the cached instance!
            //TODO this is actually a work-around that should not be needed anymore once plugin-loading is refactored
            scalaPluginsByAbsolutePath.put(path.toAbsolutePath(), plugin);

            //it is so stupid bukkit doesn't let me extend PluginDescriptionFile.
            return plugin.getDescription();

            //Ideally I would use ScalaPluginDescription.toPluginDescriptionFile().
            //but the ScalaPluginDescription requires a live ScalaPlugin instance (for now)
            //So I need to give up on this idea, or analyze the bytecode (that is going to be hard o.0)
            //TODO when I implement the library loading api, implement plugin-loading in such a way that a ScalaPlugin instance does not have to be created
            //TODO for getting the ScalaPluginDescription.
            //TODO make sure that currently-compiled plugins can still load (possibly with the help of some extra bytecode transformations!)

        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            throw new InvalidDescriptionException(e,
                    "Your plugin's constructor, initializer or class initializer tried to access classes that were not yet loaded. " +
                            "Try to move stuff over to onLoad() or onEnable().");
        } catch (ScalaPluginLoaderException e) {
            throw new InvalidDescriptionException(e, "Failed to create scala library classloader.");
        } catch (MalformedURLException e) {
            throw new InvalidDescriptionException(e, "Invalid jar file location.");
        } catch (IOException e) {
            throw new InvalidDescriptionException(e, "Failed to create scala plugin classloader.");
        }
    }

    /**
     * Get the jar file of a ScalaPlugin.
     * @param scalaPlugin the plugin
     * @return the jar file
     * @throws IOException if a jarfile could not be created
     */
    public JarFile getJarFile(ScalaPlugin scalaPlugin) throws IOException {
        return Compat.jarFile(scalaPlugin.getClassLoader().getPluginJarFile());
    }

    @SuppressWarnings("unchecked")
    private static void injectClassesIntoJavaPlugin(Stream<? extends Class<?>> classes, JavaPlugin javaPlugin) {
        ClassLoader javaPluginClassLoader = javaPlugin.getClass().getClassLoader();
        try {
            Field field = javaPluginClassLoader.getClass().getField("classes");
            field.setAccessible(true);
            Map<String, Class<?>> classesMap = (Map<String, Class<?>>) field.get(javaPluginClassLoader);
            classes.forEach(clazz -> classesMap.put(clazz.getName(), clazz));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    //TODO make an equivalent version for the scala standard library.
    //TODO it should inspect the javaplugin's bytecode to see which scala library classes
    //TODO are needed - so we don't load them all (that would be A LOT of RAM xD).
    //TODO call that method in openUpToJavaPlugin.
    /**
     * Makes classes from a ScalaPlugin visible to the JavaPlugin's classloader so that the ScalaPlugin
     * can be used by the JavaPlugin.
     * @apiNote this only makes the classes that are in the ScalaPlugin's jar file available - that excludes the Scala standard library classes.
     * @param scalaPlugin the scala plugin
     * @param javaPlugin the java plugin
     */
    //TODO deprecate this if this does not work without illegal access enabled
    //TODO test this!
    public void openUpToJavaPlugin(ScalaPlugin scalaPlugin, JavaPlugin javaPlugin) {
        try {
            injectClassesIntoJavaPlugin(getAllClasses(scalaPlugin), javaPlugin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all classes from a scala plugin.
     * @param scalaPlugin the scala plugin
     * @return an open stream that provides all classes that are in the ScalaPlugin's jar file
     * @throws IOException if the stream could not be opened for whatever reason
     */
    public Stream<? extends Class<?>> getAllClasses(ScalaPlugin scalaPlugin) throws IOException {
        JarFile jarFile = getJarFile(scalaPlugin);
        return jarFile.stream()
                .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                .map(jarEntry -> {
                    try {
                        return jarFile.getInputStream(jarEntry);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(inputStream -> {
                    try {
                        return new DescriptionScanner(inputStream); //TODO create a smaller Scanner class?
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(DescriptionScanner::hasClass)
                .map(DescriptionScanner::getClassName)
                .map(className -> {
                    try {
                        return Class.forName(className, false, scalaPlugin.getClassLoader());
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

    /**
     * Loads all classes in the ScalaPlugin's jar file.
     * @param scalaPlugin the ScalaPlugin
     *
     * @deprecated Use {@link #openUpToJavaPlugin(ScalaPlugin, JavaPlugin)} instead.
     *             This method used to inject classes from the ScalaPlugin into the 'global' JavaPluginLoader scope,
     *             so that JavaPlugins could find classes from the ScalaPlugin.
     *             But since it no longer does that, it has no use to call this method anymore.
     *             This method will be removed in a future version!
     */
    @Deprecated
    public void forceLoadAllClasses(ScalaPlugin scalaPlugin) {
        try {
            getAllClasses(scalaPlugin).forEach(noop -> {});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        ScalaPlugin plugin = scalaPluginsByAbsolutePath.get(file.toPath().toAbsolutePath());

        if (plugin != null) {
            // assume a ScalaPlugin was loaded by getPluginDescription
            for (String dependency : plugin.getScalaDescription().getHardDependencies()) {
                boolean dependencyFound = server.getPluginManager().getPlugin(dependency) != null;
                if (!dependencyFound) {
                    throw new UnknownDependencyException("Dependency " + dependency + " not found while loading plugin " + plugin.getName());
                }
            }

            plugin.getLogger().info("Loading " + plugin.getScalaDescription().getFullName());
            plugin.onLoad();
            return plugin;
        } else {
            // A ScalaPlugin was not loaded by getPluginDescription - try to load a JavaPlugin
            return getJavaPluginLoader().loadPlugin(file);
        }
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        if (plugin instanceof ScalaPlugin) {
            ScalaPlugin scalaPlugin = (ScalaPlugin) plugin;
            if (scalaPlugin.isEnabled()) return;

            ScalaPluginEnableEvent event = new ScalaPluginEnableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            plugin.getLogger().info("Enabling " + scalaPlugin.getScalaDescription().getFullName());
            scalaPlugin.setEnabled(true);
            scalaPlugin.onEnable();
        } else {
            //delegate unknown plugin types
            getJavaPluginLoader().enablePlugin(plugin);
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (plugin instanceof ScalaPlugin) {
            ScalaPlugin scalaPlugin = (ScalaPlugin) plugin;
            if (!scalaPlugin.isEnabled()) return;

            ScalaPluginDisableEvent event = new ScalaPluginDisableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            plugin.getLogger().info("Disabling " + scalaPlugin.getScalaDescription().getFullName());
            scalaPlugin.onDisable();
            scalaPlugin.setEnabled(false);

            //get the classloader
            ScalaPluginClassLoader scalaPluginClassLoader = scalaPlugin.getClassLoader();
            //de-register codecs
            RuntimeConversions.clearCodecs(scalaPluginClassLoader);
            //unload shared classes
            ScalaRelease scalaCompatRelease = scalaPluginClassLoader.getScalaRelease();
            Map<String, Class<?>> classes = sharedScalaPluginClasses.get(scalaCompatRelease);
            if (classes != null) {
                scalaPluginClassLoader.getClasses().forEach((className, clazz) -> {
                    classes.remove(className, clazz);
                    //will bukkit ever get a proper pluginloader api? https://hub.spigotmc.org/jira/browse/SPIGOT-4255
                    //scalaPluginClassLoader.removeFromJavaPluginLoaderScope(className);
                });
                if (classes.isEmpty()) {
                    sharedScalaPluginClasses.remove(scalaCompatRelease);
                }
            }

            CopyOnWriteArrayList<ScalaPluginClassLoader> classLoaders = sharedScalaPluginClassLoaders.get(scalaCompatRelease);
            if (classLoaders != null) {
                classLoaders.remove(scalaPluginClassLoader);
                //noinspection SuspiciousMethodCalls - Thank IntelliJ but this is how you do an atomic removeIfEmpty.
                sharedScalaPluginClassLoaders.remove(scalaCompatRelease, Compat.emptyList());
            }

            try {
                scalaPluginClassLoader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //delegate unknown plugin types
            getJavaPluginLoader().disablePlugin(plugin);
        }
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        Pattern[] patterns = getScalaLoader().getJavaPluginLoaderPatterns();
        if (patterns != null) return patterns;
        return pluginFileFilters.clone();
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return getJavaPluginLoader().createRegisteredListeners(listener, plugin);
    }

    /**
     * Make a class visible for all {@link ScalaPlugin}s with a certain (or binary compatible) Scala version.
     * @implNote Classes loaded by a {@link ScalaLibraryClassLoader} directly are rejected.
     *
     * @param scalaVersion the scala version
     * @param className the name of the class
     * @param clazz the class
     * @return whether the class was added to the cache of this plugin loader
     * @deprecated use {@link #addClassGlobally(ScalaRelease, String, Class)} instead
     */
    @Deprecated
    public boolean addClassGlobally(String scalaVersion, String className, Class<?> clazz) {
        return addClassGlobally(ScalaRelease.fromScalaVersion(scalaVersion), className, clazz);
    }

    /**
     * Make a class visible for all {@link ScalaPlugin}s with a binary compatible version of Scala.
     * @implNote scala standard library classes are immediately rejected.
     *
     * @param scalaCompat the compability version of Scala
     * @param className the name of the class
     * @param clazz the class
     * @return true if the class was added to the cache of this plugin loader, otherwise false
     */
    public boolean addClassGlobally(ScalaRelease scalaCompat, String className, Class<?> clazz) {
        if (clazz.getClassLoader() instanceof ScalaLibraryClassLoader) return false;

        return cacheClass(scalaCompat, className, clazz) == null;
    }

    /**
     * Caches a class, making it accessible to {@link ScalaPlugin}s using a certain version of Scala.
     * @param scalaRelease the compatilibity-release version of Scala
     * @param className the name of the class
     * @param clazz the class
     * @return a the class that was cached before, using the same scala version and the same class name, or null if no such class was cached
     */
    private Class<?> cacheClass(ScalaRelease scalaRelease, String className, Class<?> clazz) {
        return sharedScalaPluginClasses
                .computeIfAbsent(scalaRelease, version -> new ConcurrentHashMap<>())
                .putIfAbsent(className, clazz);
    }

    /**
     * Finds classes from {@link ScalaPlugin}s. This method can possibly be called by multiple threads concurrently
     * since {@link ScalaPluginClassLoader}s are parallel capable.
     *
     * @param scalaCompatRelease the Scala version the plugin uses
     * @param className the name of the class
     * @return the class object, if a class with the given name exists, and is binary compatible with the given Scala version
     * @throws ClassNotFoundException if no scala plugin has a class with the given name, or the Scala version is incompatible
     */
    protected Class<?> getScalaPluginClass(final ScalaRelease scalaCompatRelease, final String className) throws ClassNotFoundException {
        //try load from 'global' cache
        Map<String, Class<?>> scalaPluginClasses = sharedScalaPluginClasses.get(scalaCompatRelease);
        Class<?> found = scalaPluginClasses == null ? null : scalaPluginClasses.get(className);
        if (found != null) return found;

        //try load from classloaders - check all scala plugins that use compatible versions of scala.
        CopyOnWriteArrayList<ScalaPluginClassLoader> classLoaders = sharedScalaPluginClassLoaders.get(scalaCompatRelease);
        if (classLoaders != null) {
            for (ScalaPluginClassLoader scalaPluginClassLoader : classLoaders) {
                try {
                    found = scalaPluginClassLoader.findClass(className, false);
                    //ScalaPluginLoader#findClass calls ScalaPluginLoader#addClassGlobally, but we might race against other threads.
                    Class<?> classLoadedByOtherThread = cacheClass(scalaCompatRelease, className, found);
                    if (classLoadedByOtherThread != null) found = classLoadedByOtherThread;

                    return found;
                } catch (ClassNotFoundException justContinueOn) {
                }
            }
        }

        throw new ClassNotFoundException("Couldn't find class " + className + " in any of the loaded ScalaPlugins.");
    }

}

