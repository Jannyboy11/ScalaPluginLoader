package xyz.janboerman.scalaloader.paper;

import com.destroystokyo.paper.utils.PaperPluginLogger;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.server.ServerLoadEvent.LoadType;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.Migration;
import xyz.janboerman.scalaloader.compat.Platform;
import xyz.janboerman.scalaloader.configurationserializable.transform.AddVariantTransformer;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanResult;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanner;
import xyz.janboerman.scalaloader.configurationserializable.transform.PluginTransformer;
import xyz.janboerman.scalaloader.dependency.PluginYamlLibraryLoader;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.event.plugin.ScalaPluginEnableEvent;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginBootstrapContext;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaCompatMap;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.paper.commands.DumpClassCommand;
import xyz.janboerman.scalaloader.paper.commands.ListScalaPluginsCommand;
import xyz.janboerman.scalaloader.paper.commands.ResetScalaUrlsCommand;
import xyz.janboerman.scalaloader.paper.commands.SetDebugCommand;
import xyz.janboerman.scalaloader.paper.plugin.PaperHacks;
import xyz.janboerman.scalaloader.paper.plugin.PluginJarScanResult;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginBootstrap;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginClasspathBuilder;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginLoader;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginProviderContext;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionClassLoader;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionPlugin;
import xyz.janboerman.scalaloader.paper.plugin.description.MainClassScanner;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency;
import xyz.janboerman.scalaloader.paper.transform.MainClassCallerMigrator;
import xyz.janboerman.scalaloader.paper.transform.PaperPluginTransformer;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * <p>
 *      This is ScalaLoader's main class when running on Paper.
 * </p>
 * <p>
 *      If you want to explicitly get the instance of the ScalaLoader plugin, use {@link IScalaLoader#getInstance()} instead.
 *      The implementation of ScalaLoader returned by this method depends on whether ScalaLoader is loaded as <a href=https://docs.papermc.io/paper/dev/getting-started/paper-plugins>Paper Plugin</a> or not.
 * </p>
 *
 * @author Jannyboy11
 */
public final class ScalaLoader extends JavaPlugin implements IScalaLoader, Listener {

    static {
        Migration.addMigrator(PaperPluginTransformer::new);
        //TODO Paper's ClassloaderBytecodeModifier api gives us the ability to transform bytecode of JavaPlugins.
        //TODO should I make use of this? Are there any ScalaLoader apis that I broke that can be called by JavaPlugins?
    }

    private EventBus eventBus;
    private final DebugSettings debugSettings = new DebugSettings(this);
    private File scalaPluginsFolder;

    //ScalaLoader has a tendency of becoming a God Object. May want to factor out plugin-loading stuff to a separate class ScalaPluginManager or something.
    private final LinkedHashSet<ScalaPlugin> scalaPlugins = new LinkedHashSet<>();

    private final ScalaCompatMap<ScalaDependency> scalaCompatMap = new ScalaCompatMap();
    private final Map<String, ScalaLibraryClassLoader> scalaLibraryClassLoaders = new HashMap<>();

    public ScalaLoader() {
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }
    }

    public static ScalaLoader getInstance() {
        return JavaPlugin.getPlugin(ScalaLoader.class);
    }

    @Override
    public boolean isPaperPlugin() {
        return true;
    }

    @Override
    public DebugSettings getDebugSettings() {
        return debugSettings;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus == null ? eventBus = new EventBus(getServer().getPluginManager()) : eventBus;
    }

    @Override
    public File getScalaPluginsFolder() {
        return scalaPluginsFolder;
    }

    @Override
    public Collection<ScalaPlugin> getScalaPlugins() {
        return Collections.unmodifiableSet(scalaPlugins);
    }

    protected void addScalaPlugin(ScalaPlugin plugin) {
        scalaPlugins.add(plugin);
    }

    @Override
    public void onLoad() {
        assert getPluginMeta().getLoadOrder() == PluginLoadOrder.STARTUP : "ScalaLoader must enable at PluginLoadOrder.STARTUP.";
        ScalaLoaderUtils.initConfiguration(this);
        loadScalaPlugins();
    }

    @Override
    public void onEnable() {
        initCommands();
        getServer().getPluginManager().registerEvents(this, this);
        enableScalaPlugins(PluginLoadOrder.STARTUP);    //Enable ScalaPlugins at STARTUP because ScalaLoader itself enables on STARTUP.
        ScalaLoaderUtils.initBStats(this);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        //enable ScalaPlugin's at POSTWORLD for both load types (STARTUP, RELOAD)
        enableScalaPlugins(PluginLoadOrder.POSTWORLD);
    }

    private void initCommands() {
        CommandMap commandMap = getServer().getCommandMap();
        commandMap.register(ResetScalaUrlsCommand.name, new ResetScalaUrlsCommand(this));
        commandMap.register(DumpClassCommand.name, new DumpClassCommand(this));
        commandMap.register(SetDebugCommand.name, new SetDebugCommand(this));
        commandMap.register(ListScalaPluginsCommand.name, new ListScalaPluginsCommand(this));

        //TODO should I create my own /plugins that displays ScalaPlugins in addition to Paper's default output?
    }

    private void loadScalaPlugins() {
        loadScalaPlugins(scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar")));
    }

    private void loadScalaPlugins(File[] files) {
        if (files == null) return;

        final Map<File, ScalaPluginDescription> descriptions = new HashMap<>();
        final Map<File, PluginJarScanResult> scanResults = new HashMap<>();
        final Map<String, File> byName = new HashMap<>();
        final Map<File, DescriptionPlugin> descriptionPlugins = new HashMap<>();
        final MutableGraph<String> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .expectedNodeCount(files.length)
                .<String>build();
        dependencyGraph.addNode("ScalaLoader");

        for (File file : files) {
            try {
                PluginJarScanResult scanResult = read(Compat.jarFile(file));

                //save scala version
                final ScalaDependency scalaDependency = scanResult.getScalaVersion();
                if (scalaDependency == null)
                    throw new ScalaPluginLoaderException("Could not find Scala dependency. Please annotate your main class with @Scala or @CustomScala.");
                scalaCompatMap.add(scalaDependency);

                final ApiVersion apiVersion = scanResult.getApiVersion();

                //register the GetClassLoaderMigrator (so that every call to MyPluginMainClass.getClassLoader() will be replaced by MyPluginMainClass.classLoader())
                final String mainClassName = scanResult.getMainClass();
                scanResult.transformerRegistry.addUnspecificTransformer(visitor -> new MainClassCallerMigrator(visitor, mainClassName));

                //Now, we instantiate the DescriptionPlugin
                var optionalDescriptionPlugin = buildDescriptionPlugin(file, scanResult, apiVersion, mainClassName, scalaDependency);
                if (optionalDescriptionPlugin.isEmpty()) continue;
                final DescriptionPlugin descriptionPlugin = optionalDescriptionPlugin.get();
                descriptionPlugins.put(file, descriptionPlugin);

                //and set the description
                ScalaPluginDescription description = descriptionPlugin.getScalaDescription();
                final String pluginName;
                if (description != null) {
                    //ScalaPluginDescription constructor was used! :)
                    description.readFromPluginYamlData(scanResult.pluginYaml);
                    pluginName = description.getName();
                } else {
                    //No-args constructor was used. Get the description from the pluginYaml.
                    pluginName = scanResult.pluginYaml.get("name").toString();
                    String version = scanResult.pluginYaml.get("version").toString();
                    description = new ScalaPluginDescription(pluginName, version);
                    description.readFromPluginYamlData(scanResult.pluginYaml);
                }
                description.setMain(mainClassName);
                description.setApiVersion(apiVersion.getVersionString());
                description.setScalaVersion(scalaDependency.getVersionString());

                //check for Folia
                if (Platform.isFolia() && !description.isFoliaSupported()) {
                    getLogger().log(Level.WARNING, "Plugin " + pluginName + " has not explicitly declared it supports the Folia server implementation.");
                    getLogger().log(Level.WARNING, "Skipping loading plugin " + file + ".");
                    continue;
                }

                //store to load later
                descriptions.put(file, description);
                scanResults.put(file, scanResult);
                byName.put(pluginName, file);
                dependencyGraph.addNode(pluginName);
                //fill dependency graph (dependencies are pointed to)
                for (String dep : description.getHardDependencies())
                    dependencyGraph.putEdge(pluginName, dep);
                for (String softDep : description.getSoftDependencies())
                    dependencyGraph.putEdge(pluginName, softDep);
                for (String inverseDep : description.getInverseDependencies())
                    dependencyGraph.putEdge(inverseDep, pluginName);
                //according to https://javadoc.io/doc/com.google.guava/guava/latest/com/google/common/graph/MutableGraph.html
                //method putEdge: "If nodeU and nodeV are not already present in this graph, this method will silently add nodeU and nodeV to the graph."
                //so this method should work if there are ScalaPlugins that depend on plain old JavaPlugins.

            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to load ScalaPlugin from file: " + file.getName(), e);
            } catch (ScalaPluginLoaderException e) {
                getLogger().log(Level.SEVERE, "Could not find main class in: " + file.getName(), e);
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Unknown error while loading plugin: " + file.getName(), e);
            }
        }

        //Food for thought:
        //perhaps, split this logic? maybe construct the ScalaPlugins from ScalaLoader's constructor
        //and then, call scalaPlugin.onLoad() from ScalaLoader's onLoad() method.

        //idea: create a ScalaPluginProvider? and register it at an EntrypointHandler? The problem in doing that is PluginFileType#guessType cannot guess our type :/

        //all ScalaPlugins have been scanned.
        //let's instantiate them!

        final List<String> pluginLoadOrder = new ArrayList<>(byName.size());
        pluginLoadOrder.addAll(byName.keySet()); //fill up the list using the byName variable. (dependencyGraph can contain plugin names which are not ScalaPlugins.)
        pluginLoadOrder.sort(dependencyOrder(dependencyGraph));

        for (String pluginName : pluginLoadOrder) {
            //the process will look as follows:
            //  - instantiate the bootstrapper:
            //  - instantiate the pluginloader
            //  - call bootstrapper.bootstrap(pluginprovidercontext)
            //  - call pluginloader.classloader(pluginclasspathbuilder)
            //  - call boostrapper.createPlugin(pluginprovidercontext)
            //  - finally, register them to Paper's PluginManager, and call .onLoad()

            File file = byName.get(pluginName);
            PluginJarScanResult scanResult = scanResults.get(file);
            ScalaPluginDescription description = descriptions.get(file);
            //TODO check dependencies. if not available, might want to try to load the ScalaPlugin again once Paper's own JavaPlugin loading process has finished.

            ScalaPluginProviderContext context = makeScalaPluginProviderContext(file, description); //used to be just new ScalaPluginProviderContext(file, description)
            var optionalBootstrap = getBootstrap(description, descriptionPlugins.get(file).descriptionClassLoader());
            if (optionalBootstrap.isEmpty()) continue;
            PluginBootstrap bootstrapper = optionalBootstrap.get();
            ScalaPluginLoader loader = new ScalaPluginLoader();
            ScalaPluginClasspathBuilder pluginClasspathBuilder = new ScalaPluginClasspathBuilder(context);

            bootstrap(bootstrapper, context);   //used to be just boostrapper.bootstrap(context), but PluginBootstrap#bootstrap(PluginProviderContext) got changed to PluginBootstrap#bootstrap(BootstrapContext).
            loader.classloader(pluginClasspathBuilder);

            var optionalPlugin = buildPlugin(pluginName, file, context, scanResult, loader, bootstrapper, pluginClasspathBuilder);
            if (optionalPlugin.isEmpty()) continue;
            ScalaPlugin plugin = optionalPlugin.get();

            addScalaPlugin(plugin);
            //don't register to Paper's PluginManager yet, see https://github.com/Jannyboy11/ScalaPluginLoader/issues/22#issuecomment-1656932716.
            plugin.onLoad();
        }

    }

    private static ScalaPluginProviderContext makeScalaPluginProviderContext(File file, ScalaPluginDescription description) {
        try {
            Class.forName("io.papermc.paper.plugin.bootstrap.BootstrapContext");
            return new ScalaPluginBootstrapContext(file, description);
        } catch (ClassNotFoundException e) {
            return new ScalaPluginProviderContext(file, description);
        }
    }

    private static void bootstrap(PluginBootstrap bootstrapper, ScalaPluginProviderContext context) {
        Method bootstrap = null;
        RuntimeException ex = new RuntimeException("could not bootstrap plugin using bootstrapper: " + bootstrapper + " and context " + context);

        try {
            Class<?> bootstrapContextClazz = Class.forName("io.papermc.paper.plugin.bootstrap.BootstrapContext");
            bootstrap = PluginBootstrap.class.getMethod("bootstrap", bootstrapContextClazz);
        } catch (ReflectiveOperationException e1) {
            ex.addSuppressed(e1);

            try {
                bootstrap = PluginBootstrap.class.getMethod("bootstrap", PluginProviderContext.class);
            } catch (ReflectiveOperationException e2) {
                ex.addSuppressed(e2);
            }
        }

        if (bootstrap != null) {
            try {
                bootstrap.invoke(bootstrapper, context);
                return;
            } catch (IllegalAccessException | InvocationTargetException e) {
                ex.addSuppressed(e);
            }
        }

        throw ex;
    }

    private void enableScalaPlugins(PluginLoadOrder loadOrder) {
        for (ScalaPlugin scalaPlugin : getScalaPlugins()) {
            if (scalaPlugin.getPluginMeta().getLoadOrder() == loadOrder) {

                //register the ScalaPlugin with Paper's pluginManager.
                PaperPluginManagerImpl paperPluginManager = PaperHacks.getPaperPluginManager();
                if (paperPluginManager.getPlugin(scalaPlugin.getName()) == null) {  //ensure idempotency
                    paperPluginManager.loadPlugin(scalaPlugin);                     //more like "registerPlugin" since PaperPluginInstanceManager.loadPlugin(Plugin) does not call Plugin.onLoad()!
                    //note that scalaPlugin.onLoad() has already been called!
                }

                //now, enable the scalaPlugin.
                if (!scalaPlugin.isEnabled()) {
                    ScalaPluginEnableEvent event = new ScalaPluginEnableEvent(scalaPlugin);
                    getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled())
                        continue;

                    PaperHacks.getPaperPluginManager().enablePlugin(scalaPlugin);
                }
            }
        }
    }

    private static Comparator<String> dependencyOrder(MutableGraph<String> dependencies) {
        return new Comparator<String>() {
            @Override
            public int compare(String plugin1, String plugin2) {
                boolean oneDependsOnTwo = dependsOn(dependencies, plugin1, plugin2);
                boolean twoDependsOnOne = dependsOn(dependencies, plugin2, plugin1);
                if (oneDependsOnTwo && !twoDependsOnOne) {
                    return 1; //plugin1 is greater - it must be loaded later
                } else if (twoDependsOnOne && !oneDependsOnTwo) {
                    return -1; //plugin2 is greater. plugin1 must be loaded earlier.
                } else {
                    return 0; //cyclic dependency, or no dependency.
                }
            }
        }.thenComparing(String.CASE_INSENSITIVE_ORDER);
    }

    private static boolean dependsOn(MutableGraph<String> dependencies, String plugin1, String plugin2) {
        return dependsOn(dependencies, plugin1, plugin2, new HashSet<>(dependencies.successors(plugin1)), new HashSet<>());
    }

    private static boolean dependsOn(MutableGraph<String> dependencies, String plugin1, String plugin2, Set<String> workingSet, Set<String> explored) {
        if (workingSet.contains(plugin2)) return true;
        explored.add(plugin1);

        workingSet = workingSet.stream().flatMap(plugin -> dependencies.successors(plugin).stream()).collect(Collectors.toSet());
        workingSet.removeAll(explored);

        if (workingSet.isEmpty()) return false;

        String dependency = workingSet.iterator().next();
        return dependsOn(dependencies, dependency, plugin2, workingSet, explored);
    }

    private static PluginJarScanResult read(JarFile pluginJarFile) throws IOException {
        final PluginJarScanResult result = new PluginJarScanResult();

        MainClassScanner bestCandidate = null;
        TransformerRegistry transformerRegistry = new TransformerRegistry();
        Map<String, Object> pluginYamlData = Compat.emptyMap();

        //enumerate the class files!
        Enumeration<JarEntry> entryEnumeration = pluginJarFile.entries();
        while (entryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = entryEnumeration.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                InputStream bytecodeStream = pluginJarFile.getInputStream(jarEntry);
                byte[] classBytes = Compat.readAllBytes(bytecodeStream);

                MainClassScanner scanner = new MainClassScanner(classBytes);
                bestCandidate = BinaryOperator.minBy(candidateComparator).apply(bestCandidate, scanner);

                //targeted bytecode transformers
                final GlobalScanResult configSerResult = new GlobalScanner().scan(new ClassReader(classBytes));
                PluginTransformer.addTo(transformerRegistry, configSerResult);
                AddVariantTransformer.addTo(transformerRegistry, configSerResult);
            }
        }

        result.mainClassScanner = bestCandidate;
        result.transformerRegistry = transformerRegistry;

        JarEntry pluginYamlEntry = pluginJarFile.getJarEntry("paper-plugin.yml");
        if (pluginYamlEntry == null) pluginYamlEntry = pluginJarFile.getJarEntry("plugin.yml");
        if (pluginYamlEntry != null) {
            Yaml yaml = new Yaml();
            InputStream pluginYamlStream = pluginJarFile.getInputStream(pluginYamlEntry);
            pluginYamlData = (Map<String, Object>) yaml.loadAs(pluginYamlStream, Map.class);
        }

        result.pluginYaml = pluginYamlData;

        return result;
    }

    //smaller = better candidate!
    private static final Comparator<MainClassScanner> candidateComparator;
    static {
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(Optional::isEmpty);
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        candidateComparator = Comparator.nullsLast(
            Comparator.comparing(MainClassScanner::getMainClass, optionalComparator)
                    .thenComparing(scanner -> !scanner.hasScalaAnnotation())            //better candidate if it has a @Scala annotation
                    .thenComparing(scanner -> !scanner.isSingletonObject())             //better candidate if it is an object
                    .thenComparing(MainClassScanner::extendsObject)                     //worse candidate if it extends Object directly
                    .thenComparing(MainClassScanner::extendsScalaPlugin)                //worse candidate if it extends ScalaPlugin directly
                    .thenComparing(MainClassScanner::getClassName, packageComparator)   //worse candidate if the package consists of a long namespace
                    .thenComparing(MainClassScanner::getClassName)                      //worse candiate if the class name is longer
        );
    }

    public ScalaCompatMap<ScalaDependency> getScalaVersions() {
        return scalaCompatMap;
    }

    private ScalaLibraryClassLoader getOrCreateScalaLibrary(ScalaDependency scalaDependency) throws ScalaPluginLoaderException {
        PluginScalaVersion scalaVersion;

        if (scalaDependency instanceof ScalaDependency.Builtin builtin) {
            scalaVersion = PluginScalaVersion.fromScalaVersion(builtin.scalaVersion());
        } else if (scalaDependency instanceof ScalaDependency.Custom custom) {
            scalaVersion = new PluginScalaVersion(custom.scalaVersion(), custom.urls());
        } else if (scalaDependency instanceof ScalaDependency.YamlDefined yamlDefined) {
            scalaVersion = PluginScalaVersion.fromScalaVersion(ScalaVersion.fromVersionString(yamlDefined.scalaVersion()));
        } else {
            scalaVersion = PluginScalaVersion.fromScalaVersion(ScalaVersion.fromVersionString(scalaDependency.getVersionString()));
        }

        return loadOrGetScalaVersion(scalaVersion);
    }

    private ClassLoader createLibraryClassLoader(ClassLoader parent, Map<String, Object> pluginYaml) throws ScalaPluginLoaderException {
        if (pluginYaml == null || !pluginYaml.containsKey("libraries")) return parent;

        PluginYamlLibraryLoader pluginYamlLibraryLoader = new PluginYamlLibraryLoader(getLogger(), new File(getDataFolder(), "libraries"));
        Collection<File> files = pluginYamlLibraryLoader.getJarFiles(pluginYaml);

        URL[] urls = new URL[files.size()];
        int i = 0;
        for (File file : files) {
            try {
                URL url = file.toURI().toURL();
                urls[i] = url;
            } catch (MalformedURLException e) {
                throw new ScalaPluginLoaderException("Malformed URL for file: " + file + "?!", e);
            }
            i += 1;
        }
        return new URLClassLoader(urls, parent);
    }

    private boolean downloadScalaJarFiles() {
        return getConfig().getBoolean("load-libraries-from-disk", true);
    }

    public ScalaLibraryClassLoader loadOrGetScalaVersion(PluginScalaVersion scalaVersion) throws ScalaPluginLoaderException {
        return ScalaLoaderUtils.loadOrGetScalaVersion(scalaLibraryClassLoaders, scalaVersion, downloadScalaJarFiles(), this);
    }

    private Optional<PluginBootstrap> getBootstrap(ScalaPluginDescription description, ClassLoader classLoader) {
        Class<?> bootstrapCls = description.getBootstrapper();
        if (bootstrapCls == null) {
            String bootstrapperName = description.getBootstrapperName();
            if (bootstrapperName != null) {
                try {
                    bootstrapCls = Class.forName(bootstrapperName, true, classLoader);
                } catch (ClassNotFoundException e) {
                    getLogger().log(Level.SEVERE, "Could not find bootstrapper class: " + bootstrapperName, e);
                    return Optional.empty();
                }
            }
        }

        if (bootstrapCls != null) {
            if (PluginBootstrap.class.isAssignableFrom(bootstrapCls)) {
                Class<? extends PluginBootstrap> bootstrapClass = bootstrapCls.asSubclass(PluginBootstrap.class);
                try {
                    return Optional.of(ScalaLoaderUtils.instantiate(bootstrapClass));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
                    getLogger().log(Level.SEVERE, "Could not instantiate bootstrapper: " + bootstrapClass.getName(), e);
                    return Optional.empty();
                }
            } else {
                getLogger().log(Level.SEVERE, "Bootstrapper " + bootstrapCls + " does not implement " + PluginBootstrap.class);
                return Optional.empty();
            }
        } else {
            return Optional.of(new ScalaPluginBootstrap());
        }
    }

    private Optional<? extends ScalaPlugin> buildPlugin(String pluginName, File file, ScalaPluginProviderContext context, PluginJarScanResult scanResult, ScalaPluginLoader loader, PluginBootstrap bootstrapper, ScalaPluginClasspathBuilder pluginClasspathBuilder) {
        try {
            ScalaPluginClassLoader pluginClassLoader = pluginClasspathBuilder.buildClassLoader(PaperPluginLogger.getLogger(context.getConfiguration()), getClassLoader(), file, scanResult.transformerRegistry, loader, scanResult.pluginYaml);
            context.setPluginClassLoader(pluginClassLoader);
            JavaPlugin javaPlugin = bootstrapper.createPlugin(context);
            if (javaPlugin instanceof ScalaPlugin scalaPlugin) {
                return Optional.of(scalaPlugin);
            } else {
                getLogger().log(Level.SEVERE, "Plugin instance returned by configured bootstrapper " + bootstrapper.getClass().getName() + " must have a type that extends " + ScalaPlugin.class + ", instead we got: " + (javaPlugin == null ? "null" : javaPlugin.getClass()));
                return Optional.empty();
            }
            //don't think I need this currently:
            //PaperPluginParent parent = new PaperPluginParent(file.toPath(), Compat.jarFile(file), context.getConfiguration(), pluginClassLoader, context);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "could not instantiate ScalaPlugin " + pluginName, e);
            return Optional.empty();
        }
    }

    private Optional<? extends DescriptionPlugin> buildDescriptionPlugin(File file, PluginJarScanResult scanResult, ApiVersion apiVersion, String mainClassName, ScalaDependency scalaDependency) {
        DescriptionClassLoader classLoader;
        try {
            ScalaLibraryClassLoader scalaLibraryClassLoader = getOrCreateScalaLibrary(scalaDependency);
            ClassLoader libraryLoader = createLibraryClassLoader(scalaLibraryClassLoader, scanResult.pluginYaml);
            classLoader = new DescriptionClassLoader(file, libraryLoader, apiVersion != ApiVersion.LEGACY, mainClassName, scalaLibraryClassLoader.getScalaVersion());
        } catch (ScalaPluginLoaderException e) {
            getLogger().log(Level.SEVERE, "Could not download all libraries from plugin's description.", e);
            return Optional.empty();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not create classloader to load " + file + "'s plugin description.", e);
            return Optional.empty();
        }

        try {
            Class<? extends DescriptionPlugin> clazz = Class.forName(mainClassName, true, classLoader).asSubclass(DescriptionPlugin.class);
            DescriptionPlugin plugin = ScalaLoaderUtils.createScalaPluginInstance(clazz);
            return Optional.of(plugin);
        } catch (ClassNotFoundException e) {
            getLogger().log(Level.SEVERE, "Could not find plugin's main class " + mainClassName + " in file " + file.getName() + ".", e);
            return Optional.empty();
        } catch (ScalaPluginLoaderException e) {
            getLogger().log(Level.SEVERE, "Could not instantiate plugin instance: " + mainClassName, e);
            return Optional.empty();
        } catch (Throwable throwable) {
            getLogger().log(Level.SEVERE, "Some error occurred in ScalaPlugin's constructor or initializer. " +
                    "Try to move stuff over to #onLoad() or #onEnable().", throwable);
            return Optional.empty();
        }
    }

    // Folia support

    @Deprecated
    private MethodHandle Server_GlobalRegionScheduler_execute;

    @Deprecated
    @Override
    public void runInMainThread(Runnable runnable) {
        if (!Platform.isFolia()) {
            getServer().getScheduler().runTask(this, runnable);
        } else {
            try {
                if (Server_GlobalRegionScheduler_execute == null) {
                    Server_GlobalRegionScheduler_execute = MethodHandles.lookup()
                            .findVirtual(Server.class, "getGlobalRegionScheduler", MethodType.methodType(Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")));
                }

                Server_GlobalRegionScheduler_execute.invoke(getServer(), this, runnable);
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Failed to schedule task on main thread (Folia/GlobalRegionScheduler)", e);
            }
        }
    }

}
