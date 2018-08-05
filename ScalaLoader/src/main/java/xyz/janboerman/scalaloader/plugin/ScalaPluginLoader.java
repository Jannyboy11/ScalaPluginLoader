package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.ClassReader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.description.DescriptionScanner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class ScalaPluginLoader implements PluginLoader {
    //TODO the pluginloader should only have one instance.
    //TODO the pluginclassloader has per-plugin instances.

    private final Server server;
    private final ScalaLoader scalaLoader;
    private final JavaPluginLoader javaPluginLoader;

    private final Set<PluginScalaVersion> scalaVersions = new HashSet<>();
    private final Pattern[] fileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    private final Map<String, ScalaLibraryClassLoader> scalaVersionParentLoaders = new HashMap<>();

    private final Map<String, Class<?>> sharedScalaPluginClasses = new HashMap<>();
    private final Map<String, ScalaPlugin> scalaPlugins = new HashMap<>();

    //TODO do I need to keep track of the loaders?
    //private final List<ScalaPluginClassLoader> loaders = new CopyOnWriteArrayList<>();

    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");
        this.scalaLoader = JavaPlugin.getPlugin(ScalaLoader.class);
        this.javaPluginLoader = (JavaPluginLoader) scalaLoader.getPluginLoader(); //TODO might fail if the
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
        //TODO I want to be able to use both 'object extends ScalaPlugin' and 'class extends ScalaPlugin'
        //TODO even plugins written in java should be able to
        //TODO delegate to JavaPluginLoader if the jar file does not contain a scala plugin.
        return null;
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

                    scalaLoader.getLogger().info("DEBUG Description Scanner = " + descriptionScanner);

                    //Emit a warning when the class does extend ScalaPlugin, but does not have de @Scala or @CustomScala annotation
                    if (descriptionScanner.extendsScalaPlugin() && !descriptionScanner.getScalaVersion().isPresent()) {
                        scalaLoader.getLogger().warning("Class " + jarEntry.getName() + " extends ScalaPlugin but does not have the @Scala or @CustomScala annotation.");
                    }

                    //TODO in the future I could transform the class to use the the relocated scala library?

                    //The smallest element is the best candidate!
                    mainClassCandidate = BinaryOperator.minBy(descriptionComparator).apply(mainClassCandidate, descriptionScanner);


                } else if (jarEntry.getName().equals("plugin.yml")) {
                    scalaLoader.getLogger().warning("Found plugin.yml in scala plugin. Ignoring.");
                }

            }

            if (mainClassCandidate == null) {
                throw new InvalidDescriptionException("Could not find main class in file " + file.getName() + ". Did you annotate your main class with @Scala?");
            }

            PluginScalaVersion scalaVersion = mainClassCandidate.getScalaVersion().get();

            try {
                //load scala version if not already present
                ScalaLibraryClassLoader scalaLibraryClassLoader = loadOrGetScalaVersion(scalaVersion);
                ScalaPluginClassLoader scalaPluginClassLoader = new ScalaPluginClassLoader(new URL[]{file.toURI().toURL()}, scalaLibraryClassLoader);

                //create our plugin
                Class<? extends ScalaPlugin> pluginMainClass = (Class<? extends ScalaPlugin>) Class.forName(mainClassCandidate.getMainClass().get(), true, scalaPluginClassLoader);
                ScalaPlugin plugin = createPluginInstance(pluginMainClass);
                plugin.init(this, server, new File(file.getParent(), plugin.getName()), file, scalaPluginClassLoader);
                if (scalaPlugins.putIfAbsent(plugin.getName().toLowerCase(), plugin) != null) {
                    throw new InvalidDescriptionException("Duplicate plugin names found: " + plugin.getName());
                }
                return plugin.getDescription();

            } catch (ScalaPluginLoaderException e) {
                throw new InvalidDescriptionException(e, "Failed to create scala library classloader");
            } catch (ClassNotFoundException e) {
                throw new InvalidDescriptionException(e, "Could find the class that was found the main class");
            } catch (NoClassDefFoundError e) {
                throw new InvalidDescriptionException(e,
                        "Your plugin's constructor and/or initializer blocks tried to access classes that were not yet loaded." +
                        "Try to move stuff over to onLoad().");
            }

        } catch (IOException e) {
            throw new InvalidDescriptionException(e, "Could not read jar file " + file.getName());
        }
    }


    private ScalaLibraryClassLoader loadOrGetScalaVersion(PluginScalaVersion scalaVersion) throws ScalaPluginLoaderException {
        ScalaLibraryClassLoader scalaLibraryLoader = scalaVersionParentLoaders.get(scalaVersion);
        if (scalaLibraryLoader != null) return scalaLibraryLoader;

        if (!scalaLoader.downloadScalaJarFiles()) {
            //load classes over the network
            try {
                new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), new URL[]{
                        new URL(scalaVersion.getScalaLibraryUrl()),
                        new URL(scalaVersion.getScalaReflectUrl())
                }, scalaLoader.getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw new ScalaPluginLoaderException("Could not load scala libraries for version " + scalaVersion, e);
            }
        } else {
            //check if downloaded already (if not, do download)
            //then load classes from the downloaded jar

            //TODO!!!!!!
        }

        throw new ScalaPluginLoaderException("Could not load scala libraries for version " + scalaVersion);
    }

    /**
     * Returns a list of all filename filters expected by this PluginLoader
     *
     * @return The filters
     */
    @Override
    public Pattern[] getPluginFileFilters() {
        return getPluginFileFilters().clone();
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
        return javaPluginLoader.createRegisteredListeners(listener, plugin);
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

    }

    /**
     * Tries to get the plugin instance from the scala plugin class.
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

                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Could not access the NoArgsConstructor of " + clazz.getName() + ", please make it public", e);
            } catch (InvocationTargetException e) {
                throw new ScalaPluginLoaderException("Error instantiating class " + clazz.getName() + ", its constructor threw something at us", e);
            } catch (NoSuchMethodException e) {
                throw new ScalaPluginLoaderException("Could not find NoArgsContstructor in class " + clazz.getName(), e);
            } catch (InstantiationException e) {
                throw new ScalaPluginLoaderException("Could not instantiate class " + clazz.getName(), e);
            }

        }

        else return null;
    }

}

