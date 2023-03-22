package xyz.janboerman.scalaloader.plugin.paper;

import com.destroystokyo.paper.utils.PaperPluginLogger;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.Migration;
import xyz.janboerman.scalaloader.configurationserializable.transform.AddVariantTransformer;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanResult;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanner;
import xyz.janboerman.scalaloader.configurationserializable.transform.PluginTransformer;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.event.plugin.ScalaPluginEnableEvent;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaCompatMap;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.paper.commands.DumpClassCommand;
import xyz.janboerman.scalaloader.plugin.paper.commands.ListScalaPluginsCommand;
import xyz.janboerman.scalaloader.plugin.paper.commands.ResetScalaUrlsCommand;
import xyz.janboerman.scalaloader.plugin.paper.commands.SetDebugCommand;
import xyz.janboerman.scalaloader.plugin.paper.description.DescriptionClassLoader;
import xyz.janboerman.scalaloader.plugin.paper.description.DescriptionPlugin;
import xyz.janboerman.scalaloader.plugin.paper.description.MainClassScanner;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency;
import xyz.janboerman.scalaloader.plugin.paper.transform.MainClassCallerMigrator;
import xyz.janboerman.scalaloader.plugin.paper.transform.PaperPluginTransformer;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
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
 * @author Jannyboy11
 */
public final class ScalaLoader extends JavaPlugin implements IScalaLoader {

    private EventBus eventBus;
    private final DebugSettings debugSettings = new DebugSettings(this);
    private File scalaPluginsFolder;

    private final LinkedHashSet<ScalaPlugin> scalaPlugins = new LinkedHashSet<>();

    private final ScalaCompatMap<ScalaDependency> scalaCompatMap = new ScalaCompatMap();
    private final Map<String, ScalaLibraryClassLoader> scalaLibraryClassLoaders = new HashMap<>();

    public ScalaLoader() {
        this.scalaPluginsFolder = new File(getDataFolder(), "scalaplugins");
        if (!scalaPluginsFolder.exists()) {
            scalaPluginsFolder.mkdirs();
        }
    }

    static ScalaLoader getInstance() {
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

    public EventBus getEventBus() {
        return eventBus == null ? eventBus = new EventBus(getServer().getPluginManager()) : eventBus;
    }

    @Override
    public File getScalaPluginsFolder() {
        return scalaPluginsFolder;
    }

    @Override
    public Collection<ScalaPlugin> getScalaPlugins() {
        return scalaPlugins;
    }

    protected void addScalaPlugin(ScalaPlugin plugin) {
        scalaPlugins.add(plugin);
    }

    @Override
    public void onLoad() {
        Migration.addMigrator(PaperPluginTransformer::new);
        ScalaLoaderUtils.initConfiguration(this);
        loadScalaPlugins();
    }

    @Override
    public void onEnable() {
        initCommands();

        //TODO should not be necessary, but apparently still is. (PaperPluginInstanceManager already enables plugins)
        enableScalaPlugins();
        //TODO have a look at how the ListPluginsCommand works -> I have to inject plugins in an EntryPointHandler dafuq?

        ScalaLoaderUtils.initBStats(this);
    }

    private void initCommands() {
        CommandMap commandMap = getServer().getCommandMap();
        commandMap.register(ResetScalaUrlsCommand.name, new ResetScalaUrlsCommand(this));
        commandMap.register(DumpClassCommand.name, new DumpClassCommand(this));
        commandMap.register(SetDebugCommand.name, new SetDebugCommand(this));
        commandMap.register(ListScalaPluginsCommand.name, new ListScalaPluginsCommand(this));
    }

    private void loadScalaPlugins() {
        loadScalaPlugins(scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar")));
    }

    private void loadScalaPlugins(File[] files) {
        if (files == null) return;

        final Map<File, ScalaPluginDescription> descriptions = new HashMap<>();
        final Map<File, PluginJarScanResult> scanResults = new HashMap<>();
        final Map<String, File> byName = new HashMap<>();
        final MutableGraph<String> dependencyGraph = GraphBuilder.directed()
                .allowsSelfLoops(false)
                .expectedNodeCount(files.length)
                .<String>build();
        dependencyGraph.addNode("ScalaLoader");

        for (File file : files) {
            try {
                PluginJarScanResult scanResult = read(Compat.jarFile(file));

                //save scala version
                ScalaDependency scalaDependency = scanResult.getScalaVersion();
                if (scalaDependency == null)
                    throw new ScalaPluginLoaderException("Could not find Scala dependency. Please annotate your main class with @Scala or @CustomScala.");
                scalaCompatMap.add(scalaDependency);

                //register the GetClassLoaderMigrator (so that every call to MyPluginMainClass.getClassLoader() will be replaced by MyPluginMainClass.classLoader())
                String mainClassName = scanResult.getMainClass();
                scanResult.transformerRegistry.addUnspecificTransformer(visitor -> new MainClassCallerMigrator(visitor, mainClassName));

                //Now, we instantiate the DescriptionPlugin
                DescriptionClassLoader classLoader = new DescriptionClassLoader(file, getOrCreateScalaLibrary(scalaDependency));
                DescriptionPlugin dummyPlugin;
                try {
                    Class<? extends DescriptionPlugin> descriptionClass = (Class<? extends DescriptionPlugin>) Class.forName(mainClassName, true, classLoader);
                    dummyPlugin = ScalaLoaderUtils.createScalaPluginInstance(descriptionClass);
                } catch (Error initializerOrConstructorError) {
                    getLogger().log(Level.SEVERE, "Some error occurred in ScalaPlugin's constructor or initializer. " +
                            "Try to move stuff over to #onLoad() or #onEnable().", initializerOrConstructorError);
                    continue;
                }

                //and set the description
                ScalaPluginDescription description = dummyPlugin.getScalaDescription();
                final String pluginName;
                if (description != null) {
                    description.setMain(mainClassName);
                    description.setApiVersion(scanResult.getApiVersion().getVersionString());
                    description.setScalaVersion(scalaDependency.getVersionString());
                    pluginName = description.getName();
                } else {
                    pluginName = scanResult.pluginYaml.get("name").toString();
                    String version = scanResult.pluginYaml.get("version").toString();
                    description = new ScalaPluginDescription(pluginName, version);
                    description.readFromPluginYamlData(scanResult.pluginYaml);
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
            } catch (ClassNotFoundException e) {
                getLogger().log(Level.SEVERE, "Main class not found: " + file.getName(), e);
            } catch (ScalaPluginLoaderException e) {
                getLogger().log(Level.SEVERE, "Could not find main class in: " + file.getName(), e);
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Unknown error while loading plugin: " + file.getName(), e);
            }
        }

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

            File file = byName.get(pluginName);
            PluginJarScanResult scanResult = scanResults.get(file);
            ScalaPluginDescription description = descriptions.get(file);

            ScalaPluginProviderContext context = new ScalaPluginProviderContext(description);
            ScalaPluginBootstrap bootstrapper = new ScalaPluginBootstrap();
            ScalaPluginLoader loader = new ScalaPluginLoader();
            ScalaPluginClasspathBuilder pluginClasspathBuilder = new ScalaPluginClasspathBuilder(context);

            bootstrapper.bootstrap(context);
            loader.classloader(pluginClasspathBuilder);

            ScalaPlugin plugin;

            try {
                ScalaPluginClassLoader pluginClassLoader = pluginClasspathBuilder.buildClassLoader(PaperPluginLogger.getLogger(context.getConfiguration()), getClassLoader(), file, scanResult.transformerRegistry, loader, scanResult.pluginYaml);
                context.setPluginClassLoader(pluginClassLoader);
                plugin = bootstrapper.createPlugin(context);
                //don't think I need this currently:
                //PaperPluginParent parent = new PaperPluginParent(file.toPath(), Compat.jarFile(file), context.getConfiguration(), pluginClassLoader, context);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "could not instantiate ScalaPlugin " + pluginName, e);
                continue;
            }

            PaperHacks.getPaperPluginManager().loadPlugin(plugin);  //calls PaperPluginInstanceManager#loadPlugin(Plugin provided)
            //this correctly takes dependencies and softdependencies into account, but not inverse dependencies. should I make the distinction between dependency graph and load graph?
            plugin.onLoad();
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

    private void enableScalaPlugins() {
        for (ScalaPlugin plugin : getScalaPlugins()) {
            if (!plugin.isEnabled()) {
                ScalaPluginEnableEvent event = new ScalaPluginEnableEvent(plugin);
                getServer().getPluginManager().callEvent(event);
                if (event.isCancelled())
                    continue;
            }

            PaperHacks.getPaperPluginManager().enablePlugin(plugin);
        }
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
        if (pluginYamlData.containsKey("main")) {
            String mainClassFile = pluginYamlData.get("main").toString().replace('.', '/') + ".class";
            result.mainClassScanner = new MainClassScanner(pluginJarFile.getInputStream(pluginJarFile.getJarEntry(mainClassFile)));
        }

        return result;
    }

    //smaller = better candidate!
    private static final Comparator<MainClassScanner> candidateComparator;
    static {
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(Optional::isEmpty);
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        candidateComparator = Comparator.nullsLast(
            Comparator.comparing(MainClassScanner::getMainClass, optionalComparator)
                    .thenComparing(scanner -> !scanner.hasScalaAnnotation())
                    .thenComparing(MainClassScanner::extendsScalaPlugin)
                    .thenComparing(MainClassScanner::getClassName, packageComparator)
                    .thenComparing(MainClassScanner::getClassName)
        );
    }

    ScalaCompatMap<ScalaDependency> getScalaVersions() {
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

    private boolean downloadScalaJarFiles() {
        return getConfig().getBoolean("load-libraries-from-disk", true);
    }

    public ScalaLibraryClassLoader loadOrGetScalaVersion(PluginScalaVersion scalaVersion) throws ScalaPluginLoaderException {
        return ScalaLoaderUtils.loadOrGetScalaVersion(scalaLibraryClassLoaders, scalaVersion, downloadScalaJarFiles(), this);
    }

}
