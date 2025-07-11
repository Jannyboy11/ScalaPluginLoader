package xyz.janboerman.scalaloader.plugin;

import com.google.common.graph.MutableGraph;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.bytecode.Replaced;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
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

@Replaced //Paper
public class ScalaPluginLoader implements PluginLoader, IScalaPluginLoader {

    private static ScalaPluginLoader INSTANCE;

    private final Server server;
    private ScalaLoader lazyScalaLoader;
    private PluginLoader lazyJavaPluginLoader;

    private static final Pattern[] pluginFileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    private final ConcurrentMap<ScalaRelease, ConcurrentMap<String, Class<?>>> sharedScalaPluginClasses = new ConcurrentHashMap<>();
    private final ConcurrentMap<ScalaRelease, CopyOnWriteArrayList<ScalaPluginClassLoader>> sharedScalaPluginClassLoaders = new ConcurrentHashMap<>();
    private final ScalaCompatMap<PluginScalaVersion> scalaCompatMap = new ScalaCompatMap();
    private final Map<Path, PluginJarScanResult> preScannedPluginJars = new ConcurrentHashMap<>();

    private final Map<String, ScalaPlugin> scalaPlugins = new HashMap<>();
    private final Map<Path, ScalaPlugin> scalaPluginsByAbsolutePath = new HashMap<>();  //if the value is null, that means it's a JavaPlugin
    private final Collection<ScalaPlugin> scalaPluginsView = Collections.unmodifiableCollection(scalaPlugins.values());
    private final Set<File> scalapluginsWaitingForDependencies = new LinkedHashSet<>();

    private EventBus eventBus;
    private PluginYamlLibraryLoader pluginYamlLibraryLoader;

    private static final Comparator<DescriptionScanner> descriptionComparator;
    static {
        //filled optionals are smaller than empty optionals.
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(optional -> !optional.isPresent());
        //smaller package hierarchy = smaller string
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        //smaller element = better main class candidate!
        descriptionComparator = Comparator.nullsLast(Comparator /* get rid of null descriptors. */
                .<DescriptionScanner, Optional<?>>comparing(DescriptionScanner::getMainClass, optionalComparator /* get rid of descriptions without a main class. */)
                .thenComparing(DescriptionScanner::extendsScalaPlugin /* classes that extend ScalaPlugin directly are less likely to be the best candidate. */)
                .thenComparing(DescriptionScanner::getClassName, packageComparator /* less deeply nested class = better candidate. */)
                .thenComparing(DescriptionScanner::getClassName /* fallback - just compare the class name strings. */));
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
                            preScannedPluginJars.put(pluginJarFile.toPath().toAbsolutePath(), scanResult);
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
     * @deprecated use {@link IScalaLoader#getEventBus()} instead.
     */
    @Deprecated
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


    public DebugSettings debugSettings() {
        return getScalaLoader().getDebugSettings();
    }


    private static PluginJarScanResult scanJar(File file) throws IOException {
        Logger logger = getInstance().getScalaLoader().getLogger();

        PluginJarScanResult result = new PluginJarScanResult();
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
                    String mainClassEntry = yamlDefinedMainClassName.replace('.', '/') + ".class";
                    JarEntry pluginYamlDefinedMainJarEntry = jarFile.getJarEntry(mainClassEntry);

                    if (pluginYamlDefinedMainJarEntry != null) {
                        InputStream classBytesInputStream = jarFile.getInputStream(pluginYamlDefinedMainJarEntry);
                        DescriptionScanner yamlMainScanner = new DescriptionScanner(classBytesInputStream); //TODO use a less powerful scanner implementation here?

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
        if (alreadyPresent != null) {
            // it's not null so we have a ScalaPlugin!
            return alreadyPresent.getDescription();
        }
        else if (scalaPluginsByAbsolutePath.containsKey(path)) {
            // alreadyPresent is null, but it was set explicitly in the map!
            // this means we are dealing with a JavaPlugin!
            return getJavaPluginLoader().getPluginDescription(file);
        }

        PluginJarScanResult jarScanResult = preScannedPluginJars.get(path);
        if (jarScanResult == null) {
            try {
                jarScanResult = scanJar(file);
                if (jarScanResult.isJavaPluginExplicitly) {
                    scalaPluginsByAbsolutePath.put(path, null);
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
            scalaPluginsByAbsolutePath.put(path, null);
            return getJavaPluginLoader().getPluginDescription(file);
        }

        assert mainClassCandidate.getScalaVersion().isPresent() : "Plugin main class is present without a PluginScalaVersion o.0";

        //assume latest if unspecified
        // TODO can use String here? then we can use ApiVersion::latestVersionString as fallback
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

            //band-aid fix ScalaPluginDescription fields whose values are detected from scanning the classes.
            plugin.getScalaDescription().setMain(mainClass);
            plugin.getScalaDescription().setApiVersion(apiVersion.getVersionString());
            plugin.getScalaDescription().setScalaVersion(scalaVersion.getScalaVersion());

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
    public static JarFile getJarFile(ScalaPlugin scalaPlugin) throws IOException {
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
     *
     * @deprecated use {@link IScalaPluginLoader#openUpToJavaPlugin(IScalaPlugin, JavaPlugin)} instead.
     */
    @Deprecated
    //can be static, but this is a public api though, and I might want to change the implementation details again later, so I think it's good that this is a virtual method.
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
    public static Stream<? extends Class<?>> getAllClasses(ScalaPlugin scalaPlugin) throws IOException {
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
    public static void forceLoadAllClasses(ScalaPlugin scalaPlugin) {
        try {
            getAllClasses(scalaPlugin).forEach(noop -> {});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        Path path = file.toPath().toAbsolutePath();
        ScalaPlugin scalaPlugin = scalaPluginsByAbsolutePath.get(path);
        Plugin plugin = scalaPlugin;

        if (scalaPlugin != null) {
            // A ScalaPlugin was loaded by getPluginDescription().
            for (String dependency : scalaPlugin.getScalaDescription().getHardDependencies()) {
                boolean dependencyFound = server.getPluginManager().getPlugin(dependency) != null;
                if (!dependencyFound) {
                    throw new UnknownDependencyException("Dependency " + dependency + " not found while loading plugin " + scalaPlugin.getName());
                }
            }
            scalaPlugin.getLogger().info("Loading " + scalaPlugin.getScalaDescription().getFullName());
            scalaPlugin.onLoad();
        } else if (scalaPluginsByAbsolutePath.containsKey(path)) {
            // A null value was put into the map! This means it is a JavaPlugin!
            // A ScalaPlugin was not loaded by getPluginDescription - try to load a JavaPlugin.
            plugin = getJavaPluginLoader().loadPlugin(file);
        } else {
            // We got here before getPluginDescription() was called.
            // So let's call it now and retry.
            try {
                getPluginDescription(file);
                assert scalaPluginsByAbsolutePath.containsKey(path) : "Expected an already-scanned jar on path: " + path;
                return loadPlugin(file);
            } catch (InvalidDescriptionException e) {
                throw new InvalidPluginException(e);
            }
        }
        
        //if the newly-loaded plugin is a dependency of a waiting ScalaPlugin, then try to load the ScalaPlugin again.
        Iterator<File> fileIterator = scalapluginsWaitingForDependencies.iterator();
        while (fileIterator.hasNext()) {
            File dependentFile = fileIterator.next();
            ScalaPlugin dependent = scalaPluginsByAbsolutePath.get(dependentFile.toPath().toAbsolutePath());
            if (dependent != null) {
                ScalaPluginDescription desc = dependent.getScalaDescription();
                if (desc.getHardDependencies().contains(plugin.getName())) {
                    try {
                        //prepare for recursive call because the dependency is not known the plugin manager at this point.
                        //another workaround could be to call addPluginToPluginManager(plugin), but I'm not certain that won't cause issues at the SimplePluginManager.
                        desc.moveHardDependencyToSoftDependency(plugin.getName());

                        //try to load the depending plugin again
                        ScalaPlugin lateScalaPlugin = (ScalaPlugin) loadPlugin(dependentFile);
                        //hack it into the PluginManager
                        addPluginToPluginManager(lateScalaPlugin);
                        //remove it from the waiting queue
                        fileIterator.remove();
                    } catch (UnknownDependencyException thereAreMoreDependenciesLeft) {
                        //ignore
                    }
                }
            }
        }

        return plugin;
    }

    private void addPluginToPluginManager(ScalaPlugin plugin) {
        synchronized (server.getPluginManager()) {
            try {
                Field pluginsField = SimplePluginManager.class.getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                List<Plugin> plugins = (List) pluginsField.get(server.getPluginManager());
                plugins.add(plugin);
            } catch (Exception tooBad) {
                getScalaLoader().getLogger().severe("Could not register plugin to PluginManager: " + plugin.getName());
            }

            Set<String> provides = plugin.getScalaDescription().getProvides();
            if (!provides.isEmpty()) {
                try {
                    Field lookupNamesField = SimplePluginManager.class.getDeclaredField("lookupNames");
                    lookupNamesField.setAccessible(true);
                    Map<String, Plugin> lookupNames = (Map) lookupNamesField.get(server.getPluginManager());
                    lookupNames.put(plugin.getName(), plugin);
                    for (String provide : provides)
                        lookupNames.putIfAbsent(provide, plugin);
                } catch (Exception tooBad) {
                    getScalaLoader().getLogger().severe("Could not register plugin lookupNames to PluginManager: " + plugin.getName());
                }
            }

            Set<String> hardDeps = plugin.getScalaDescription().getHardDependencies(),
                    softDeps = plugin.getScalaDescription().getSoftDependencies(),
                    inverseDeps = plugin.getScalaDescription().getInverseDependencies();
            if (!(hardDeps.isEmpty() && softDeps.isEmpty() && inverseDeps.isEmpty())) {
                try {
                    Field dependencyGraphField = SimplePluginManager.class.getDeclaredField("dependencyGraph");
                    dependencyGraphField.setAccessible(true);
                    MutableGraph<String> dependencyGraph = (MutableGraph) dependencyGraphField.get(server.getPluginManager());
                    for (String hardDep : hardDeps)
                        dependencyGraph.putEdge(plugin.getName(), hardDep);
                    for (String softDep : softDeps)
                        dependencyGraph.putEdge(plugin.getName(), softDep);
                    for (String inverseDep : inverseDeps)
                        dependencyGraph.putEdge(inverseDep, plugin.getName());
                } catch (NoSuchFieldException expected) {
                    //the dependencyGraph field does not exist yet in Bukkit 1.8.8. therefore this behaviour is expected in some scenarios.
                    //therefore we only log the exception if the field does exist.
                } catch (Exception e) {
                    getScalaLoader().getLogger().severe("Could not register plugin dependencies to PluginManager: " + plugin.getName());
                }
            }
        }
    }

    public void loadWhenDependenciesComeAvailable(File file) {
        scalapluginsWaitingForDependencies.add(file);
    }

    public Set<File> getPluginsWaitingForDependencies() {
        return Collections.unmodifiableSet(scalapluginsWaitingForDependencies);
    }

    public void clearPluginsWaitingForDependencies() {
        scalapluginsWaitingForDependencies.clear();
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        if (plugin instanceof ScalaPlugin) {
            ScalaPlugin scalaPlugin = (ScalaPlugin) plugin;
            if (scalaPlugin.isEnabled()) return;

            ScalaPluginEnableEvent event = new ScalaPluginEnableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            PluginEnableEvent bukkitEvent = new PluginEnableEvent(scalaPlugin);
            server.getPluginManager().callEvent(bukkitEvent);

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

            //call the Scala event first
            ScalaPluginDisableEvent event = new ScalaPluginDisableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            PluginDisableEvent bukkitEvent = new PluginDisableEvent(scalaPlugin);
            server.getPluginManager().callEvent(bukkitEvent);

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
                //noinspection SuspiciousMethodCalls - Thanks IntelliJ but this is how you do an atomic removeIfEmpty.
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

    protected boolean removeClassGlobally(ScalaRelease scalaCompat, String className, Class<?> clazz) {
        ConcurrentMap<String, Class<?>> classesForThisVersion = sharedScalaPluginClasses.get(scalaCompat);
        if (classesForThisVersion == null) return false;

        return classesForThisVersion.remove(className, clazz);
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

