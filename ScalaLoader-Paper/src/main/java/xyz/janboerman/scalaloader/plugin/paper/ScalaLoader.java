package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.configurationserializable.transform.AddVariantTransformer;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanResult;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanner;
import xyz.janboerman.scalaloader.configurationserializable.transform.PluginTransformer;
import xyz.janboerman.scalaloader.event.EventBus;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaCompatMap;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.paper.description.DescriptionClassLoader;
import xyz.janboerman.scalaloader.plugin.paper.description.DescriptionPlugin;
import xyz.janboerman.scalaloader.plugin.paper.description.MainClassScanner;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

/**
 * @author Jannyboy11
 */
public class ScalaLoader extends JavaPlugin implements IScalaLoader {

    private EventBus eventBus;
    private DebugSettings debugSettings = new DebugSettings(this);
    private File scalaPluginsFolder;
    private Set<ScalaPlugin> scalaPlugins = new HashSet<>();
    private final ScalaCompatMap<ScalaDependency> scalaCompatMap = new ScalaCompatMap();

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
        ScalaLoaderUtils.initConfiguration(this);
        loadScalaPlugins();
    }

    @Override
    public void onEnable() {
        ScalaLoaderUtils.initCommands(this);
        enableScalaPlugins();
        ScalaLoaderUtils.initBStats(this);
    }

    private void loadScalaPlugins() {
        //TODO might need to load them 'all at once' because of the new paper plugin update
        loadScalaPlugins(scalaPluginsFolder.listFiles((File dir, String name) -> name.endsWith(".jar")));
    }

    private void loadScalaPlugins(File[] files) {
        if (files == null) return;

        final Map<File, ScalaPluginDescription> descriptions = new HashMap<>();
        final Map<File, PluginJarScanResult> scanResults = new HashMap<>();

        for (File file : files) {
            try {
                PluginJarScanResult scanResult = read(Compat.jarFile(file));
                addScala(scalaCompatMap, scanResult.getScalaVersion());

                //Now, we instantiate the DescriptionPlugin
                DescriptionClassLoader classLoader = new DescriptionClassLoader(file, getClassLoader());
                String mainClassName = scanResult.getMainClass();
                Class<? extends DescriptionPlugin> descriptionClass = (Class<? extends DescriptionPlugin>) Class.forName(mainClassName, false, classLoader);
                DescriptionPlugin dummyPlugin = ScalaLoaderUtils.createScalaPluginInstance(descriptionClass);

                //and set the description
                ScalaPluginDescription description = dummyPlugin.getScalaDescription();
                if (description != null) {
                    description.setMain(mainClassName);
                    description.setApiVersion(scanResult.getApiVersion().getVersionString());
                    description.setScalaVersion(scanResult.getScalaVersion().getVersionString());
                } else {
                    String pluginName = scanResult.pluginYaml.get("name").toString();
                    String version = scanResult.pluginYaml.get("version").toString();
                    description = new ScalaPluginDescription(pluginName, version);
                    description.readFromPluginYamlData(scanResult.pluginYaml);
                }

                //store to load later
                descriptions.put(file, description);
                scanResults.put(file, scanResult);

            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to load ScalaPlugin from file: " + file.getName(), e);
            } catch (ClassNotFoundException e) {
                getLogger().log(Level.SEVERE, "Main class not found: " + file.getName(), e);
            } catch (ScalaPluginLoaderException e) {
                getLogger().log(Level.SEVERE, "Could not find main class in: " + file.getName(), e);
            }
        }

        //all ScalaPlugins have been scanned.
        //let's instantiate them!

        for (File file : files) {
            //the process will look as follows:
            //  - instantiate the bootstrapper:
            //  - instantiate the pluginloader
            //  - call bootstrapper.bootstrap(pluginprovidercontext)
            //  - call pluginloader.classloader(pluginclasspathbuilder)
            //  - call boostrapper.createPlugin(pluginprovidercontext)

            ScalaPluginDescription description = descriptions.get(file);
            ScalaPluginProviderContext context = new ScalaPluginProviderContext(description);

        }



        //TODO make sure to register the plugins with Paper's PluginInstanceManager.
        //TODO make sure that plugin dependencies are registered correctly.
        //TODO call PaperPluginInstanceManager#loadPlugin(Plugin provided) - it does both of the above!

        //TODO for every ScalaPlugin, make sure to call Plugin.onLoad().
    }

    private void enableScalaPlugins() {
        for (ScalaPlugin plugin : getScalaPlugins()) {
            PaperHacks.getPaperPluginManager().enablePlugin(plugin);
        }
    }

    private static PluginJarScanResult read(JarFile pluginJarFile) throws IOException {
        final PluginJarScanResult result = new PluginJarScanResult();

        MainClassScanner bestCandidate = null;
        TransformerRegistry transformerRegistry = new TransformerRegistry();
        Map<String, Object> pluginYamlData = null;

        //enumerate the class files!
        Enumeration<JarEntry> entryEnumeration = pluginJarFile.entries();
        while (entryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = entryEnumeration.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                InputStream bytecodeStream = pluginJarFile.getInputStream(jarEntry);
                byte[] classBytes = Compat.readAllBytes(bytecodeStream);

                MainClassScanner scanner = new MainClassScanner(classBytes);
                bestCandidate = BinaryOperator.minBy(candidateComparator).apply(bestCandidate, scanner);

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

    private static void addScala(ScalaCompatMap store, ScalaDependency scalaDep) {
        PluginScalaVersion scalaVersion;
        if (scalaDep instanceof ScalaDependency.Builtin builtin) {
            scalaVersion = PluginScalaVersion.fromScalaVersion(builtin.scalaVersion());
        } else if (scalaDep instanceof ScalaDependency.Custom custom) {
            scalaVersion = new PluginScalaVersion(custom.scalaVersion(), custom.urls());
        } else if (scalaDep instanceof ScalaDependency.YamlDefined yamlDefined) {
            scalaVersion = PluginScalaVersion.fromScalaVersion(ScalaVersion.fromVersionString(yamlDefined.scalaVersion()));
        } else {
            throw new IllegalArgumentException("Could not detect Scala dependency. Please annotate your main class with @Scala or @CustomScala.");
        }
        store.add(scalaVersion);
    }

    ScalaCompatMap<ScalaDependency> getScalaVersions() {
        return scalaCompatMap;
    }

}
