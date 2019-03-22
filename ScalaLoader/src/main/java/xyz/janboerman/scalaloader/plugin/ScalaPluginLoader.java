package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.event.ScalaPluginDisableEvent;
import xyz.janboerman.scalaloader.event.ScalaPluginEnableEvent;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

    /**
     * Per PluginLoader API, the constructor has only one parameter: the Server.
     * @param server the server.
     */
    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");

        //Static abuse but I cannot find a more elegant way to do this.
        if (INSTANCE == null) {
            INSTANCE = this;
        }
    }

    /**
     * Get the instance that was created when this ScalaPluginLoader was constructed.
     * @apiNote if you call this method from your own plugin, your plugin must have a dependency on {@link ScalaLoader}.
     * @return the instance that was created either by bukkit's {@link PluginManager} or by the {@link ScalaLoader},
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

    @SuppressWarnings("unchecked")
    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        final ScalaPlugin alreadyPresent = scalaPluginsByFile.get(file);
        if (alreadyPresent != null) return alreadyPresent.getDescription();

        getScalaLoader().getLogger().info("Reading ScalaPlugin jar: " + file.getName() + "..");

        //filled optionals are smaller then empty optionals.
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(optional -> !optional.isPresent());
        //smaller package hierarchy = smaller string
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        //smaller element = better main class candidate!
        Comparator<DescriptionScanner> descriptionComparator = Comparator.nullsLast(Comparator /* get rid of null descriptors */
                .<DescriptionScanner, Optional<?>>comparing(DescriptionScanner::getMainClass, optionalComparator /* get rid of descriptions without a main class */)
                .thenComparing(DescriptionScanner::extendsScalaPlugin /* classes that extend ScalaPlugin directly are less likely to be the best candidate. */)
                .thenComparing(DescriptionScanner::getClassName, packageComparator /* less deeply nested class = better candidate*/)
                .thenComparing(DescriptionScanner::getClassName /* fallback - just compare the class name strings */));

        Map<String, Object> pluginYamlData = null;

        try {
            DescriptionScanner mainClassCandidate = null;

            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entryEnumeration = jarFile.entries();
            while (entryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = entryEnumeration.nextElement();
                if (jarEntry.getName().endsWith(".class")) {

                    InputStream classBytesInputStream = jarFile.getInputStream(jarEntry);

                    DescriptionScanner descriptionScanner = new DescriptionScanner(classBytesInputStream);

                    //Emit a warning when the class does extend ScalaPlugin, but does not have de @Scala or @CustomScala annotation
                    if (descriptionScanner.extendsScalaPlugin() && !descriptionScanner.getScalaVersion().isPresent()) {
                        getScalaLoader().getLogger().warning("Class " + jarEntry.getName() + " extends ScalaPlugin but does not have the @Scala or @CustomScala annotation.");
                    }

                    //TODO in the future I could transform the class to use the the relocated scala library?
                    //TODO if I find the 'perfect' main class candidate - break the loop early. That would break access from JavaPlugins though.
                    //TODO What if I create a 'fake' PluginClassLoader and add it to the JavaPluginLoader that uses the ScalaPluginClassLoader as a parent? :)

                    //The smallest element is the best candidate!
                    mainClassCandidate = BinaryOperator.minBy(descriptionComparator).apply(mainClassCandidate, descriptionScanner);


                } else if (jarEntry.getName().equals("plugin.yml")) {
                    //If it contains a main class and it doesn't extend ScalaPlugin directly we should try to delegate to the JavaPluginLoader
                    //If it doesn't contain a main class then we add the 'fields' of the plugin yaml to the ScalaPluginDescription.

                    Yaml yaml = new Yaml();
                    InputStream pluginYamlInputStream  = jarFile.getInputStream(jarEntry);
                    pluginYamlData = (Map<String, Object>) yaml.loadAs(pluginYamlInputStream, Map.class);

                    if (pluginYamlData.containsKey("main")) {
                        String yamlDefinedMainClassName = pluginYamlData.get("main").toString();
                        String mainClassEntry = yamlDefinedMainClassName + ".class";
                        JarEntry pluginYamlDefinedMainJarEntry = jarFile.getJarEntry(mainClassEntry);

                        if (pluginYamlDefinedMainJarEntry != null) {
                            InputStream classBytesInputStream = jarFile.getInputStream(pluginYamlDefinedMainJarEntry);
                            DescriptionScanner yamlMainScanner = new DescriptionScanner(classBytesInputStream);

                            if (yamlMainScanner.extendsJavaPlugin()) {
                                //TODO check whether this main class depends on a scala version - if yes transform the classes from the java plugin
                                return getJavaPluginLoader().getPluginDescription(file);
                            }
                        } //else: main does exist and is not a javaplugin. just continue
                    } //else: plugin yaml doesn't contain main
                } //else: jarentry is not the plugin.yml

            } //end while - no more JarEntries

            if (mainClassCandidate == null || !mainClassCandidate.getMainClass().isPresent()) {
                getScalaLoader().getLogger().warning("Could not find main class in file " + file.getName() + ". Did you annotate your main class with @Scala and is it public?");
                getScalaLoader().getLogger().warning("Delegating to JavaPluginLoader...");
                return getJavaPluginLoader().getPluginDescription(file);
            }

            //null for unknown api version
            ApiVersion apiVersion = mainClassCandidate.getBukkitApiVersion().orElse(null);

            //TODO transform bytes if necessary based on apiVersion

            PluginScalaVersion scalaVersion = mainClassCandidate.getScalaVersion().get();

            try {
                final String mainClass = mainClassCandidate.getMainClass().get();

                //load scala version if not already present
                ScalaLibraryClassLoader scalaLibraryClassLoader = getScalaLoader().loadOrGetScalaVersion(scalaVersion);
                //create plugin classloader using the resolved scala classloader
                ScalaPluginClassLoader scalaPluginClassLoader =
                        new ScalaPluginClassLoader(this, new URL[]{file.toURI().toURL()}, scalaLibraryClassLoader,
                                server, pluginYamlData, file, apiVersion == null ? null : apiVersion.getVersionString());
                sharedScalaPluginClassLoaders.computeIfAbsent(scalaVersion.getScalaVersion(), v -> new CopyOnWriteArrayList<>()).add(scalaPluginClassLoader);

                //create our plugin
                Class<? extends ScalaPlugin> pluginMainClass = (Class<? extends ScalaPlugin>) Class.forName(mainClass, true, scalaPluginClassLoader);
                ScalaPlugin plugin;
                try {
                     plugin = createPluginInstance(pluginMainClass);
                } catch (ScalaPluginLoaderException e) {
                    throw new InvalidDescriptionException(e, "Couldn't create/get plugin instance for main class " + mainClass);
                }

                //moved to ScalaPlugin constructor
                //plugin.getScalaDescription().setApiVersion(apiVersion == null ? null : apiVersion.getVersionString());
                //plugin.getScalaDescription().setMain(mainClass); //required per PluginDescriptionFile constructor - not actually used.
                //plugin.init(this, server, pluginYmlFileYaml, new File(file.getParent(), plugin.getName()), file, scalaPluginClassLoader);

                if (scalaPlugins.putIfAbsent(plugin.getName().toLowerCase(), plugin) != null) {
                    throw new InvalidDescriptionException("Duplicate plugin names found: " + plugin.getName());
                }

                //be sure to cache the plugin - later in loadPlugin we just return the cached instance!
                scalaPluginsByFile.put(file, plugin);

                //used by forceLoadAllClasses
                filesByScalaPlugin.put(plugin, file);

                //it is so stupid bukkit doesn't let me extend PluginDescriptionFile.
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
     * Get the jar file of a ScalaPlugin.
     * @param scalaPlugin the plugin
     * @return the jar file
     * @throws IOException if a jarfile could not be created
     */
    public JarFile getJarFile(ScalaPlugin scalaPlugin) throws IOException {
        return new JarFile(filesByScalaPlugin.get(scalaPlugin));
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
    /**
     * Makes classes from a ScalaPlugin visible to the JavaPlugin's classloader so that the ScalaPlugin
     * can be used by the JavaPlugin.
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
     * @return an open stream that provides all classes
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
                        return new DescriptionScanner(inputStream);
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
     * @deprecated pollutes the JavaPluginLoader with scala-version-specific classes.
     *             use {@link #openUpToJavaPlugin(ScalaPlugin, JavaPlugin)} instead.
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
        if (plugin instanceof JavaPlugin) {
            getJavaPluginLoader().enablePlugin(plugin);
        } else if (plugin instanceof ScalaPlugin) {
            ScalaPlugin scalaPlugin = (ScalaPlugin) plugin;
            if (scalaPlugin.isEnabled()) return;

            ScalaPluginEnableEvent event = new ScalaPluginEnableEvent(scalaPlugin);
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;

            plugin.getLogger().info("Enabling " + scalaPlugin.getScalaDescription().getFullName());
            scalaPlugin.setEnabled(true);
            scalaPlugin.onEnable();
        } else {
            throw new IllegalArgumentException("ScalaPluginLoader can only enable " + ScalaPlugin.class.getSimpleName() + "s");
        }
    }

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
                    scalaPluginClassLoader.removeFromJavaPluginLoaderScope(className);
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
        }
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        Pattern[] patterns = getScalaLoader().getJavaPluginLoaderPattners();
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
    public Class<?> getScalaPluginClass(final String scalaVersion, final String className) throws ClassNotFoundException {
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
        //TODO this seems very scala-compiler-implementation-detail dependent. I hope this will still work in Scala 3.
        //TODO how to make this more robust?

        if (clazz.getName().endsWith("$")) {
            //we found a scala singleton object.
            //the instance is already present in the MODULE$ field when this class is loaded.

            try {
                Field field = clazz.getField("MODULE$");
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

            ScalaPluginLoaderException exception;

            try {
                Constructor ctr = clazz.getConstructor();
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

