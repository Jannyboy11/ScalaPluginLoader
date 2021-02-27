package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.transform.AddVariantTransformer;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanResult;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanner;
import xyz.janboerman.scalaloader.configurationserializable.transform.PluginTransformer;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.event.plugin.ScalaPluginDisableEvent;
import xyz.janboerman.scalaloader.event.plugin.ScalaPluginEnableEvent;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScalaPluginLoader implements PluginLoader {

    private static ScalaPluginLoader INSTANCE;

    private final Server server;
    private ScalaLoader lazyScalaLoader;
    private PluginLoader lazyJavaPluginLoader;

    private static final Pattern[] pluginFileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    //Map<ScalaVersion, Map<ClassName, Class<?>>>
    private final ConcurrentMap<String, ConcurrentMap<String, Class<?>>> sharedScalaPluginClasses = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CopyOnWriteArrayList<ScalaPluginClassLoader>> sharedScalaPluginClassLoaders = new ConcurrentHashMap<>();

    private final Map<String, ScalaPlugin> scalaPlugins = new HashMap<>();
    private final Map<File, ScalaPlugin> scalaPluginsByFile = new HashMap<>();
    private final Map<ScalaPlugin, File> filesByScalaPlugin = new HashMap<>();
    private final Collection<ScalaPlugin> scalaPluginsView = Collections.unmodifiableCollection(scalaPlugins.values());

    private final EventBus eventBus;

    private final Comparator<DescriptionScanner> descriptionComparator;
    {
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

    private final ScalaCompatMap scalaCompatMap = new ScalaCompatMap();
    private final Map<File, PluginJarScanResult> preScannedPluginJars = new HashMap<>();

    @SuppressWarnings("deprecation")
    public ScalaPluginLoader(ScalaLoader scalaLoader) {
        this.lazyScalaLoader = scalaLoader;
        this.server = scalaLoader.getServer();
        this.eventBus = new EventBus(server.getPluginManager());
        init();
    }

    /**
     * Per PluginLoader API, the constructor has only one parameter: the Server.
     * @param server the server.
     */
    @SuppressWarnings("deprecation")
    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");
        this.eventBus = new EventBus(server.getPluginManager());
        init();
    }

    private void init() {
        //Static abuse but I cannot find a more elegant way to do this.
        if (INSTANCE == null) {
            INSTANCE = this;
        } else {
            throw new IllegalStateException("The ScalaPluginLoader can only be instantiated once!");
        }

        //pre-scan plugins so that scala-versions can be detected BEFORE the main classes are instantiated!
        //this is just a best-effort thing because the PluginManager may load plugins at arbitrary points in time.

        File pluginsFolder = getScalaLoader().getScalaPluginsFolder();
        if (pluginsFolder.exists()) {
            File[] pluginJarFiles = pluginsFolder.listFiles((dir, name) -> Arrays.stream(getPluginFileFilters())
                    .anyMatch(pattern -> pattern.matcher(name).find()));
            if (pluginJarFiles != null) {
                for (File pluginJarFile : pluginJarFiles) {
                    try {
                        PluginJarScanResult scanResult = scanJar(pluginJarFile);

                        preScannedPluginJars.put(pluginJarFile, scanResult);
                        scanResult.mainClassCandidate.getScalaVersion().ifPresent(scalaCompatMap::add);

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


    private PluginJarScanResult scanJar(File file) throws IOException {
        PluginJarScanResult result = new PluginJarScanResult();

        getScalaLoader().getLogger().info("Reading ScalaPlugin file: " + file.getName() + "..");

        TransformerRegistry transformerRegistry = new TransformerRegistry();

        Map<String, Object> pluginYamlData = Collections.emptyMap();
        DescriptionScanner mainClassCandidate = null;

        JarFile jarFile = Compat.jarFile(file);
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
                    getScalaLoader().getLogger().warning("Class " + jarEntry.getName() + " extends ScalaPlugin but does not have the @Scala or @CustomScala annotation.");
                    //this is just a soft warning and not a hard error because this class itself may be subclassed by the actual main class
                }

                //TODO in the future I could transform the class to use the the relocated scala library?
                //TODO if I find the 'perfect' main class candidate - break the loop early. That would break access from JavaPlugins though.
                //TODO What if I create a 'fake' PluginClassLoader and add it to the JavaPluginLoader that uses the ScalaPluginClassLoader as a parent? :)

                //The smallest element is the best candidate!
                mainClassCandidate = BinaryOperator.minBy(descriptionComparator).apply(mainClassCandidate, descriptionScanner);

                //scan class to see if this class is configurationserializable and wants to register a plugin transformer:
                final GlobalScanResult configSerResult = new GlobalScanner().scan(new ClassReader(classBytes));
                PluginTransformer.addTo(transformerRegistry, configSerResult);
                AddVariantTransformer.addTo(transformerRegistry, configSerResult);

            } else if (jarEntry.getName().equals("plugin.yml")) {
                //If it contains a main class and it doesn't extend ScalaPlugin directly we should try to delegate to the JavaPluginLoader
                //If it doesn't contain a main class then we add the 'fields' of the plugin yaml to the ScalaPluginDescription.

                Yaml yaml = new Yaml();
                InputStream pluginYamlInputStream = jarFile.getInputStream(jarEntry);
                pluginYamlData = (Map<String, Object>) yaml.loadAs(pluginYamlInputStream, Map.class);

                if (pluginYamlData.containsKey("main")) {
                    String yamlDefinedMainClassName = pluginYamlData.get("main").toString();
                    String mainClassEntry = yamlDefinedMainClassName + ".class";
                    JarEntry pluginYamlDefinedMainJarEntry = jarFile.getJarEntry(mainClassEntry);

                    if (pluginYamlDefinedMainJarEntry != null) {
                        InputStream classBytesInputStream = jarFile.getInputStream(pluginYamlDefinedMainJarEntry);
                        DescriptionScanner yamlMainScanner = new DescriptionScanner(classBytesInputStream);

                        if (yamlMainScanner.extendsJavaPlugin()) {
                            //TODO check whether this main class depends on a scala version - if yes then can we make the scala library classes accessible?
                            //TODO would I need to transform the bytecode so that is uses a relocated scala library?
                            result.isJavaPluginExplicitly = true;
                        }
                    }
                }
            }
        }

        result.mainClassCandidate = mainClassCandidate;
        result.transformerRegistry = transformerRegistry;
        result.extraYaml = pluginYamlData;

        return result;
    }



    @SuppressWarnings("unchecked")
    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        final ScalaPlugin alreadyPresent = scalaPluginsByFile.get(file);
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
        Map<String, Object> pluginYamlData = jarScanResult.extraYaml;
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

            //create plugin classloader using the resolved scala classloader
            ScalaPluginClassLoader scalaPluginClassLoader =
                    new ScalaPluginClassLoader(this, new URL[]{ file.toURI().toURL() }, scalaLibraryClassLoader,
                            server, pluginYamlData, file, apiVersion, mainClass, transformerRegistry);
            sharedScalaPluginClassLoaders.computeIfAbsent(scalaVersion.getScalaVersion(), v -> new CopyOnWriteArrayList<>()).add(scalaPluginClassLoader);

            //create our plugin
            Class<? extends ScalaPlugin> pluginMainClass = (Class<? extends ScalaPlugin>) Class.forName(mainClass, true, scalaPluginClassLoader);
            //sadly this can't be postponed to #loadPlugin(File), because the plugin's constructor constructs part of the description
            //that's used by the PluginDescriptionFile that we need to return

            ScalaPlugin plugin;
            try {
                 plugin = createPluginInstance(pluginMainClass);
            } catch (ScalaPluginLoaderException e) {
                throw new InvalidDescriptionException(e, "Couldn't create/get plugin instance for main class " + mainClass);
            }

            if (scalaPlugins.putIfAbsent(plugin.getName().toLowerCase(), plugin) != null) {
                throw new InvalidDescriptionException("Duplicate plugin names found: " + plugin.getName());
            }

            //be sure to cache the plugin - later in #loadPlugin(File) we just return the cached instance!
            scalaPluginsByFile.put(file, plugin);

            //used by forceLoadAllClasses
            filesByScalaPlugin.put(plugin, file);

            //it is so stupid bukkit doesn't let me extend PluginDescriptionFile.
            return plugin.getDescription();

            //Ideally I would use ScalaPluginDescription.toPluginDescriptionFile().
            //but the ScalaPluginDescription requires a live ScalaPlugin instance (for now)
            //So I need to give up on this idea, or analyze the bytecode (that is going to be hard o.0)

        } catch (ClassNotFoundException e) {
            throw new InvalidDescriptionException(e, "The main class of your plugin could not be found!");
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
        return Compat.jarFile(filesByScalaPlugin.get(scalaPlugin));
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
                        return Class.forName(className, true, scalaPlugin.getClassLoader());
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
     * @deprecated This method pollutes the JavaPluginLoader with scala-version-specific classes.
     *             Use {@link #openUpToJavaPlugin(ScalaPlugin, JavaPlugin)} instead.
     *             ForRemoval because as of January 2020 this method is inherently broken.
     *             The PluginClassLoader used to load JavaPlugins will now try to explicitly cast the ClassLoader
     *             of classes in the JavaPluginLoader's classes cache to PluginClassLoader - resulting in a ClassCastException.
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
        ScalaPlugin plugin = scalaPluginsByFile.get(file);
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

            //unload shared classes
            ScalaPluginClassLoader scalaPluginClassLoader = scalaPlugin.getClassLoader();
            String scalaVersion = scalaPluginClassLoader.getScalaVersion();
            Map<String, Class<?>> classes = sharedScalaPluginClasses.get(scalaVersion);
            if (classes != null) {
                scalaPluginClassLoader.getClasses().forEach((className, clazz) -> {
                    classes.remove(className, clazz);
                    //TODO will bukkit ever get a proper pluginloader api? https://hub.spigotmc.org/jira/browse/SPIGOT-4255
                    //scalaPluginClassLoader.removeFromJavaPluginLoaderScope(className);
                });
                if (classes.isEmpty()) {
                    sharedScalaPluginClasses.remove(scalaVersion);
                }
            }

            CopyOnWriteArrayList<ScalaPluginClassLoader> classLoaders = sharedScalaPluginClassLoaders.get(scalaVersion);
            if (classLoaders != null) {
                classLoaders.remove(scalaPluginClassLoader);
                //noinspection SuspiciousMethodCalls - Thank IntelliJ but this is how you do an atomic removeIfEmpty.
                sharedScalaPluginClassLoaders.remove(scalaVersion, Collections.emptyList());
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
     */
    public boolean addClassGlobally(String scalaVersion, String className, Class<?> clazz) {
        if (clazz.getClassLoader() instanceof ScalaLibraryClassLoader) return false;

        return cacheClass(scalaVersion, className, clazz) == null;
    }

    /**
     * Caches a class, making it accessible to {@link ScalaPlugin}s using a certain version of Scala.
     * @param scalaVersion the Scala version
     * @param className the name of the class
     * @param clazz the class
     * @return a the class that was cached before, using the same scala version and the same class name, or null if no such class was cached
     */
    private Class<?> cacheClass(String scalaVersion, String className, Class<?> clazz) {
        return sharedScalaPluginClasses
                .computeIfAbsent(scalaVersion, version -> new ConcurrentHashMap<>())
                .putIfAbsent(className, clazz);
    }

    /**
     * Finds classes from {@link ScalaPlugin}s. This method can possibly be called by multiple threads concurrently
     * since {@link ScalaPluginClassLoader}s are parallel capable.
     *
     * @param scalaVersion the Scala version the plugin uses
     * @param className the name of the class
     * @return the class object, if a class with the given name exists, and is binary compatible with the given Scala version
     * @throws ClassNotFoundException if no scala plugin has a class with the given name, or the Scala version is incompatible
     */
    Class<?> getScalaPluginClass(final String scalaVersion, final String className) throws ClassNotFoundException {
        //try load from 'global' cache
        Map<String, Class<?>> scalaPluginClasses = sharedScalaPluginClasses.get(scalaVersion);
        Class<?> found = scalaPluginClasses == null ? null : scalaPluginClasses.get(className);
        if (found != null) return found;

        //try load from classloaders - check all scala plugins that use compatible versions of scala.
        CopyOnWriteArrayList<ScalaPluginClassLoader> classLoaders = sharedScalaPluginClassLoaders.entrySet().parallelStream()
                .filter(e -> checkCompat(scalaVersion, e.getKey()))
                .flatMap(e -> e.getValue().parallelStream())
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

        for (ScalaPluginClassLoader scalaPluginClassLoader : classLoaders) {
            try {
                found = scalaPluginClassLoader.findClass(className, false);
                //ScalaPluginLoader#findClass calls ScalaPluginLoader#addClassGlobally, but we might race against other threads.
                Class<?> classLoadedByOtherThread = cacheClass(scalaVersion, className, found);
                if (classLoadedByOtherThread != null) found = classLoadedByOtherThread;

                return found;
            } catch (ClassNotFoundException justContinueOn) {
            }
        }

        throw new ClassNotFoundException("Couldn't find class " + className + " in any of the loaded ScalaPlugins.");
    }

    private static boolean checkCompat(final String ownVersion, final String otherVersion) {
        //TODO special-case Scala 2.13.x and Scala 3.0.0
        //TODO do we still need this/does this need to be changed now that we have the ScalaCompatMap?

        //TODO I think we no longer need to iterate over all ScalaPluginClassLoaders and apply a filter
        //TODO it can probably just be done with a map lookup.

        int indexOfDot = ownVersion.lastIndexOf('.');
        if (indexOfDot == -1) return ownVersion.equals(otherVersion);

        int otherIndexOfDot = otherVersion.lastIndexOf('.');
        if (otherIndexOfDot != indexOfDot) return false;

        String beforeLastDot1 = ownVersion.substring(0, indexOfDot);
        String beforeLastDot2 = otherVersion.substring(0, indexOfDot);
        return beforeLastDot1.equals(beforeLastDot2);

        //we don't care what comes after the last dot because those versions are compatible
    }


    /**
     * Tries to get the plugin instance from the scala plugin class.
     * This method is able to get the static instances from scala `object`s,
     * as well as it is able to create plugins using their public NoArgsConstructors.
     * @param clazz the plugin class
     * @param <P> the plugin type
     * @return the plugin's instance.
     * @throws ScalaPluginLoaderException when a plugin instance could not be created for the given class
     */
    private <P extends ScalaPlugin> P createPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
        //TODO change this logic:
        //TODO If there is a static final field with name MODULE$ with the same type as the class itself AND the name ends with a '$' character, then it must be a singleton object!
        //TODO this seems very scala-compiler-implementation-detail dependent. I hope this will still work in Scala 3.
        //TODO how to make this more robust?

        if (clazz.getName().endsWith("$")) {
            //we found a scala singleton object.
            //the instance is already present in the MODULE$ field when this class is loaded.

            try {
                Field field = clazz.getDeclaredField("MODULE$");
                //TODO assert that it is public, static and final

                Object pluginInstance = field.get(null);

                return clazz.cast(pluginInstance);
            } catch (NoSuchFieldException e) {
                throw new ScalaPluginLoaderException("Static field MODULE$ not found in class " + clazz.getName(), e);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Couldn't access static field MODULE$ in class " + clazz.getName(), e);
            }

        } else {
            //we found are a regular class.
            //it should have a public zero-argument constructor

            try {
                Constructor<?> ctr = clazz.getConstructor();
                Object pluginInstance = ctr.newInstance();

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

    }

}

