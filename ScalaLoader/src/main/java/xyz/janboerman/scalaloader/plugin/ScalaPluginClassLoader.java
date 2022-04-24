package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.*;
//import org.objectweb.asm.tree.MethodNode;
//import org.objectweb.asm.tree.analysis.Analyzer;
//import org.objectweb.asm.tree.analysis.AnalyzerException;
//import org.objectweb.asm.tree.analysis.Interpreter;
//import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.util.*;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.dependency.LibraryClassLoader;
import xyz.janboerman.scalaloader.util.ClassLoaderUtils;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.Platform;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassFile;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;
import xyz.janboerman.scalaloader.plugin.runtime.PersistentClasses;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.jar.*;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
@Called
public class ScalaPluginClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final String scalaVersion;
    private final ScalaRelease scalaRelease;
    private final ScalaPluginLoader pluginLoader;
    private final Server server;
    private final Map<String, Object> extraPluginYaml;
    private final File pluginJarFile;
    private final JarFile jarFile;
    private final ApiVersion apiVersion;
    private final String mainClassName;
    private final TransformerRegistry transformerRegistry;

    private final ConcurrentMap<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final ScalaPlugin plugin;
    private final PersistentClasses persistentClasses;
    private final LibraryClassLoader libraryLoader;

    /**
     * Construct a ClassLoader that loads classes for {@link ScalaPlugin}s.
     *
     * @param pluginLoader the ScalaPluginClassLoader
     * @param urls the urls on which the classes are located
     * @param parent the parent classloader which must be a ScalaLibraryClassLoader
     * @param server the Server in which the plugin will run
     * @param extraPluginYaml extra plugin settings not defined through the ScalaPlugin's constructor, but in the plugin.yml file
     * @param pluginJarFile the plugin's jar file
     * @param apiVersion bukkit's api version that's used by the plugin
     *
     * @throws IOException if the plugin's file could not be read as a {@link JarFile}
     * @throws ScalaPluginLoaderException if the plugin instance could not be constructed
     */
    protected ScalaPluginClassLoader(ScalaPluginLoader pluginLoader,
                                     URL[] urls,
                                     ScalaLibraryClassLoader parent,
                                     Server server,
                                     Map<String, Object> extraPluginYaml,
                                     File pluginJarFile,
                                     ApiVersion apiVersion,
                                     String mainClassName,
                                     TransformerRegistry transformerRegistry,
                                     Collection<File> dependencies) throws IOException, ScalaPluginLoaderException {
        super(urls, parent);

        this.pluginLoader = pluginLoader;
        this.scalaVersion = parent.getScalaVersion();
        this.scalaRelease = ScalaRelease.fromScalaVersion(scalaVersion);

        this.server = server;
        this.extraPluginYaml = extraPluginYaml;
        this.pluginJarFile = pluginJarFile;
        this.jarFile = Compat.jarFile(pluginJarFile);
        this.apiVersion = apiVersion;
        this.mainClassName = mainClassName;
        this.transformerRegistry = transformerRegistry;

        this.libraryLoader = new LibraryClassLoader(dependencies.toArray(new File[dependencies.size()]),
                                                    parent,
                                                    pluginLoader.getScalaLoader().getLogger(),
                                                    this,
                                                    transformerRegistry);
        try {
            this.plugin = createPluginInstance((Class<? extends ScalaPlugin>) Class.forName(mainClassName, true, this));
        } catch (ClassNotFoundException e) {
            throw new ScalaPluginLoaderException("Could not find plugin's main class: " + mainClassName, e);
        }
        this.persistentClasses = new PersistentClasses(plugin);
        for (ClassFile classFile : this.persistentClasses.load()) {
            ClassDefineResult cdr = getOrDefineClass(classFile.getClassName(), name -> classFile.getByteCode(false), false);
            if (cdr.isNew()) {
                Class<?> clazz = cdr.getClassDefinition();
                if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
                    Class<? extends ConfigurationSerializable> cls = (Class<? extends ConfigurationSerializable>) clazz;
                    ConfigurationSerialization.registerClass(cls, ConfigurationSerialization.getAlias(cls));
                }
            }
        }
    }


    /**
     * Get the ScalaPlugin loaded by this class loader.
     * @return the plugin
     */
    public ScalaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Get the version of Scala used for the plugin loaded by this class loader.
     * @return the scala version
     */
    public String getScalaVersion() {
        return scalaVersion;
    }

    /**
     * Get the compatibility-release version of Scala used by the plugin.
     * @return the compatibility release
     */
    public ScalaRelease getScalaRelease() {
        return scalaRelease;
    }

    /**
     * Get the plugin loader that uses this class loader.
     * @return the scala plugin loader
     */
    public ScalaPluginLoader getPluginLoader() {
        return pluginLoader;
    }

    /**
     * Get the server the plugin runs on.
     * @return the server
     */
    public Server getServer() {
        return server;
    }

    /**
     * Get the extra plugin settings that was not defined in the constructor, but defined in the plugin.yml.
     * @return the extra plugin settings
     */
    public Map<String, Object> getExtraPluginYaml() {
        return extraPluginYaml;
    }

    /**
     * Get the file for the plugin.
     * @return the file
     */
    public File getPluginJarFile() {
        return pluginJarFile;
    }

    /**
     * Get the version of bukkit's api the plugin uses.
     * @return bukkit's api version
     */
    public ApiVersion getApiVersion() {
        return apiVersion;
    }

    /**
     * Get the name of the main class of the plugin.
     * @return the fully qualified name
     */
    public String getMainClassName() {
        return mainClassName;
    }

    /**
     * <p>
     *  Tries to load a class with the given name using the following search priorities:
     * </p>
     * <ol>
     *     <li>Search for the class in the Scala standard library</li>
     *     <li>Search for the class in the ScalaPlugin's own jar file</li>
     *     <li>Search for the class in one of the plugin's library dependencies</li>
     *     <li>Search for the class in other ScalaPlugins</li>
     *     <li>Search for the class in JavaPlugins and Bukkit/Server implementation classes</li>
     * </ol>
     *
     * @param name the name of the class
     * @return a class with the given name
     * @throws ClassNotFoundException if a class could not be found by this classloader
     * @see LibraryClassLoader
     */
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        //load order:
        //  0.  scala standard library classes
        //  1.  the plugin's jar
        //  2.  libraries
        //  3.  other scalaplugins
        //  4.  ScalaLoader, other javaplugins, Bukkit/NMS classes (parent)

        if (name.startsWith("scala.") || name.startsWith("dotty.")) {
            //short-circuit scala standard library and dotty/tasty classes.
            //we do this because if some plugin or library (such as PDM) adds the scala library to this classloader using #addUrl(URL),
            //then we still want to use the scala library that is loaded by the parent classloader
            //the same goes for libraries defined in the plugin.yml
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        ClassNotFoundException fallback = new ClassNotFoundException(name);
        Class<?> clazz;

        try {
            //findClass tries to find the class in the ScalaPlugin's jar first,
            //if that fails, it attempts to find the class in one of the library dependencies using LibraryClassLoader,
            //if that fails, it attempts to find the class in other ScalaPlugins using the ScalaPluginLoader,
            clazz = findClass(name, true);
            if (clazz != null) return clazz;
        } catch (ClassNotFoundException e) {
            fallback.addSuppressed(e);
        }

        try {
            if (name.startsWith("xyz.janboerman.scalaloader")) {
                if (name.startsWith("xyz.janboerman.scalaloader.bytecode")
                        || name.startsWith("xyz.janboerman.scalaloader.configurationserializable.transform")
                        || name.startsWith("xyz.janboerman.scalaloader.event.transform")
                        || name.startsWith("xyz.janboerman.scalaloader.compat")
                        || name.startsWith("xyz.janboerman.scalaloader.util")
                        || name.startsWith("xyz.janboerman.scalaloader.commands")
                        || name.startsWith("xyz.janboerman.scalaloader.dependency")
                        || name.equals("xyz.janboerman.scalaloader.plugin.runtime.PersistentClasses")
                ) throw new ClassNotFoundException("Can't access internal class: " + name);
            }

            //the parent classloader has access to:
            //  - scala library classes (but we already caught this case)
            //  - javaplugins (including ScalaLoader itself)
            //  - server classes (bukkit, craftbukkit, nms, and server-included libraries)

            clazz = getParent().loadClass(name);
            if (clazz != null) return clazz;
        } catch (ClassNotFoundException e) {
            fallback.addSuppressed(e);
        }

        throw fallback;
    }

    /**
     * Dumps the disassembled class bytecode to standard output, if debugging is enabled for this class.
     * @param className the fully qualified name of the class
     * @param bytecode the bytecode of the class
     * @see DebugSettings
     * @see xyz.janboerman.scalaloader.commands.SetDebug
     */
    private void debugClass(String className, byte[] bytecode) {
        DebugSettings debugSettings = getPluginLoader().debugSettings();
        if (debugSettings.isDebuggingClassLoadOf(className)) {
            Printer debugPrinter;
            switch (debugSettings.getFormat()) {
                case DebugSettings.ASMIFIED: debugPrinter = new ASMifier(); break;
                default: debugPrinter = new Textifier();
            }
            getPluginLoader().getScalaLoader().getLogger().info("[DEBUG] Dumping bytecode for class " + className);
            ClassReader debugReader = new ClassReader(bytecode);
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, debugPrinter, new PrintWriter(System.out));
            debugReader.accept(traceClassVisitor, 0);
            //TODO if check whether asm-analysis is enabled in the debugsettings, and if so, perform analysis using the SimpleVerifier
        }
    }

    /**
     * Finds and loads a class used by the ScalaPlugin loaded by this ClassLoader.
     *
     * @param name the name of the class to be found
     * @param searchInScalaPluginLoader whether or not to search in the 'global' classes cache of the {@link ScalaPluginLoader}.
     * @return a class with the given name, if found
     * @throws ClassNotFoundException if no class with the given name could be found
     * @apiNote this method never returns null, it either returns a class, or throws an exception or error
     */
    public Class<?> findClass(final String name, final boolean searchInScalaPluginLoader) throws ClassNotFoundException {
        //search in cache
        Class<?> found = classes.get(name);
        if (found != null) return found;

        //search in our own jar
        try {
            //do a manual search so that we can transform the class bytes.
            String path = name.replace('.', '/') + ".class";
            JarEntry jarEntry = jarFile.getJarEntry(path);  //if running on Paper and Java 11 or higher, this will find the class meant for the newest compatible release of Java. (Multi-Release JARs ftw!)
            // issue link: https://github.com/PaperMC/Paper/issues/4841
            // commit that introduced the patch: https://github.com/PaperMC/Paper/commit/f15abda5627005fcdf6da4b43f2636b17d41c96c

            if (jarEntry != null) {
                //a classfile exists for the given class name

                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    byte[] classBytes = Compat.readAllBytes(inputStream);

                    //apply generic transformations
                    classBytes = ClassLoaderUtils.transform(name, classBytes, this, transformerRegistry, this, getPluginLoader().getScalaLoader().getLogger());

                    //apply main class transformations
                    {
                        ClassWriter classWriter = new ClassWriter(0) {
                            @Override
                            protected ClassLoader getClassLoader() {
                                return ScalaPluginClassLoader.this;
                            }
                        };

                        ClassVisitor classVisitor = classWriter;

                        //apply main class transformations
                        if (name.equals(mainClassName)) {
                            for (BiFunction<ClassVisitor, String, ClassVisitor> mainClassTransformer : transformerRegistry.mainClassTransformers) {
                                classVisitor = mainClassTransformer.apply(classVisitor, mainClassName);
                            }
                        }

                        //if there were any transformers, then apply the transformations!
                        if (classVisitor != classWriter) {
                            ClassReader classreader = new ClassReader(classBytes);
                            classreader.accept(classVisitor, 0);
                            classBytes = classWriter.toByteArray();
                        }
                    }

                    //dump the class to the log in case classloading debugging was enabled for this class
                    debugClass(name, classBytes);

                    // Note to self 2020-11-11:
                    // If I ever get a java.lang.ClassFormatError: Invalid length 65526 in LocalVariableTable in class file com/example/MyClass
                    // then the cause was: visitLocalVariable was not called before visitMaxes and visitEnd, but way earlier!
                    // this is not explained by the order documented in the MethodVisitor class!

                    //define the package
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex != -1) {
                        String packageName = name.substring(0, dotIndex);
                        //use getDefinedPackage in Java11+
                        if (getPackage(packageName) == null) {
                            try {
                                Manifest manifest = jarFile.getManifest();
                                if (manifest != null) {
                                    definePackage(packageName, manifest, this.getURLs()[0]);
                                } else {
                                    definePackage(packageName, null, null, null, null, null, null, null);
                                }
                            } catch (IllegalArgumentException e) {
                                if (getPackage(packageName) == null) {
                                    throw new IllegalStateException("Cannot find package " + packageName);
                                }
                            }
                        }
                    }

                    //define the class
                    CodeSigner[] codeSigners = jarEntry.getCodeSigners();
                    CodeSource codeSource = new CodeSource(getURLs()[0], codeSigners);
                    found = defineClass(name, classBytes, 0, classBytes.length, codeSource);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }


        } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }

        //search in library dependencies
        if (found == null) {
            try {
                /* It is important here that we call libraryLoader.findClass(name) and not libraryLoader.loadClass(name)
                 * because we don't want to find classes from the parent classloader of the libraryLoader!
                 */
                found = libraryLoader.findClass(name);
            } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }
        }

        //search in other ScalaPlugins
        if (found == null && searchInScalaPluginLoader) {
            try {
                found = pluginLoader.getScalaPluginClass(getScalaRelease(), name); /*Do I want this here? not in the loadClass method?*/
            } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }
        }

        if (found == null) {
            //unfortunately all hope is lost if we get here
            throw new ClassNotFoundException(name);
        }

        //cache the class, possibly racing against other threads that try to load the same class.
        found = addClass(found);

        //we don't search in the parent classloader explicitly - this is done by the loadClass method.
        return found;
    }

    /**
     * Adds a class to this ClassLoader so that this ScalaPluginClassLoader can find the class
     * and the class can be used by the ScalaPlugin.
     *
     * @param toAdd the class
     * @return the same class, or an already existing class if one with the same name was found already
     */
    public Class<?> addClass(Class<?> toAdd) {
        String name = toAdd.getName();
        Class<?> loadedConcurrently = classes.putIfAbsent(name, toAdd);
        if (loadedConcurrently == null) {
            //if we find this class for the first time
            if (pluginLoader.addClassGlobally(getScalaRelease(), name, toAdd)) {
                if (canInjectIntoJavaPluginLoaderScope()) {
                    injectIntoJavaPluginLoaderScope(name, toAdd);
                }
            }
            //TODO should we 'link' this class using resolveClass(toAdd)? or maybe at our call sites? I haven't really ran into issues by NOT calling this method, so...
            //TODO what breaks when we don't link using resolveClass? classes can already be found by loadClass en findClass because they are in the classes Map.
            return toAdd;
        } else {
            //if some other tried to load the same class and won the race, use that class instead.
            return loadedConcurrently;
        }
    }

    /**
     * Generates a class for this class loader, or gets a cached version if a class with the same name was already loaded.
     *
     * @param className the name of the class
     * @param classGenerator the generator for the class
     * @param persist whether to automatically re-generate this class again the next time the plugin loads
     * @return the result of a class definition
     */
    public ClassDefineResult getOrDefineClass(String className, ClassGenerator classGenerator, boolean persist) {
        final Class<?> oldClass = classes.get(className);
        if (oldClass != null) {
            return ClassDefineResult.oldClass(oldClass);
        }

        byte[] byteCode = classGenerator.generate(className);
        debugClass(className, byteCode);

        boolean isNew;
        Class<?> clazz;

        synchronized (getClassLoadingLock(className)) {
            Class<?> existingClass = classes.get(className);
            if (existingClass == null) {
                Class<?> definition = defineClass(className, byteCode, 0, byteCode.length);
                clazz = addClass(definition);
                isNew = true;
            } else {
                clazz = existingClass;
                isNew = false;
            }
        }

        if (isNew) {
            if (persist) {
                persistentClasses.save(new ClassFile(className, byteCode));
            }
            return ClassDefineResult.newClass(clazz);
        } else {
            return ClassDefineResult.oldClass(clazz);
        }
    }

    /**
     * Finds a resource that is in the ScalaPlugin's jar file.
     *
     * @param resourcePath the name of the resource
     * @return the {@code URL} of the resource, or {@code null} if a resource with the given name did not exist
     */
    @Override
    public URL getResource(String resourcePath) {
        //override to avoid searching in the parent classloader
        return findResource(resourcePath);
    }

    //TODO should probably override findResource and findResources as to include resources from the library loader
    //TODO https://hub.spigotmc.org/jira/browse/SPIGOT-6904

    /**
     * Finds resources that are in the ScalaPlugin's jar file.
     *
     * @param resourcePath the name of the resource
     * @return An {@code Enumeration} of {@code URL}s. If the loader is closed, the Enumeration contains no elements.
     * @throws IOException if an I/O exception occurs
     */
    @Override
    public Enumeration<URL> getResources(String resourcePath) throws IOException {
        //override to avoid searching in the parent classloader
        return findResources(resourcePath);
    }

    /**
     * Gets a view of the plugin's classes.
     * @return an immutable view of the classes
     */
    protected final Map<String, Class<?>> getClasses() {
        return Collections.unmodifiableMap(classes);
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
    private static <P extends ScalaPlugin> P createPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
        boolean endsWithDollar = clazz.getName().endsWith("$");
        boolean hasStaticFinalModule$;
        Field module$Field;
        boolean hasPrivateConstructor;
        try {
            module$Field = clazz.getDeclaredField("MODULE$");
            int modifiers = module$Field.getModifiers();
            if (module$Field.getType().equals(clazz)
                    && (modifiers & Modifier.STATIC) == Modifier.STATIC
                    && (modifiers & Modifier.FINAL) == Modifier.FINAL) {
                hasStaticFinalModule$ = true;
            } else {
                hasStaticFinalModule$ = false;
            }
        } catch (NoSuchFieldException e) {
            hasStaticFinalModule$ = false;
            module$Field = null;
        }
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        hasPrivateConstructor = constructors.length == 1 && (constructors[0].getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE;

        //this seems to be how the scala compiler encodes 'object's. not sure if this is actually specified or an implementation detail.
        //in any case, it works good enough for me!
        boolean isObjectSingleton = endsWithDollar && hasStaticFinalModule$ && hasPrivateConstructor;

        if (isObjectSingleton) {
            //we found a scala singleton object.
            //the instance is already present in the MODULE$ field when this class is loaded.

            try {
                Object pluginInstance = module$Field.get(null);
                return clazz.cast(pluginInstance);
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


    /**
     * Similar to {@link ScalaPluginLoader#addClassGlobally(ScalaRelease, String, Class)}, the {@link JavaPluginLoader} also has such a method: setClass.
     * This is used by {@link org.bukkit.plugin.java.JavaPlugin}s to make their classes accessible to other JavaPlugins.
     * Since we want a  {@link ScalaPlugin}'s classes to be accessible to JavaPlugins, this method can be called to share a class with ALL JavaPlugins and ScalaPlugins.
     * This is dangerous business because it can pollute the JavaPluginLoader with classes for multiple (binary incompatible) versions of Scala.
     * <br>
     * Be sure to call {@link #removeFromJavaPluginLoaderScope(String)} again when the class is no longer needed to prevent memory leaks.
     * <br>
     * It is better (but still not ideal) to use {@link ScalaPluginLoader#openUpToJavaPlugin(ScalaPlugin, JavaPlugin)} instead.
     *
     * @param className the name of the class
     * @param clazz the class
     *
     * @deprecated JavaPlugins that try to find classes using the JavaPluginLoader expect to only find JavaPlugins.
     * @see <a href="https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/diff/src/main/java/org/bukkit/plugin/java/PluginClassLoader.java?until=c3aeaea0fb88600643e01b6b4259e9d5da49e0e7">PluginClassLoader</a>
     */
    @Deprecated
    protected final void injectIntoJavaPluginLoaderScope(String className, Class<?> clazz) {
        PluginLoader likelyJavaPluginLoader = pluginLoader.getJavaPluginLoader();
        //loop the plugin(class)loader hierarchy until we find a JavaPluginLoader?
        //this seems impossible to do properly because bukkit provides no api to go PluginLoader to the providing Plugin.
        //best we can do is hardcode checks for common plugin loaders - the JavaPluginLoader and the ScalaPluginLoader, possibly Steenooo's KotlinPluginLoader
        //I don't think it's worth the effort because I don't see custom PluginLoader implementations becoming a common thing in the bukkit development space.

        if (likelyJavaPluginLoader instanceof JavaPluginLoader) {
            JavaPluginLoader javaPluginLoader = (JavaPluginLoader) likelyJavaPluginLoader;
            //In the past JavaPluginLoader#setClass was not thread-safe, and PluginClassLoader was not parallel capable, but now they are.
            //So for backwards-compat reasons I need to query whether the PluginClassLoader is parallel capable.

            //Define the action that will share the class with the JavaPluginLoader
            Runnable setClass = () -> {
                try {
                    Method method = javaPluginLoader.getClass().getDeclaredMethod("setClass", String.class, Class.class);
                    method.setAccessible(true);
                    method.invoke(javaPluginLoader, className, clazz);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException tooBad) {
                    //too bad - JavaPlugins won't be able to magically depend on the ScalaPlugin associated with this classloader.
                }
            };

            //Are JavaPlugins' PluginClassLoaders parallel capable?
            if (javaPluginClassLoaderParallelCapable()) {
                //If JavaPlugins' classloader are parallel capable, just run the action, no matter what thread we are on.
                setClass.run();
            } else {
                //If not, schedule the action to be run in the main thread.
                pluginLoader.getScalaLoader().runInMainThread(setClass);
            }
        }
    }

    /**
     * Removes a class from the {@link JavaPluginLoader}'s global classes cache.
     *
     * @param className the name of the class
     * @see #injectIntoJavaPluginLoaderScope(String, Class)
     *
     * @deprecated JavaPlugins that try to find classes using the JavaPluginLoader expect to only find JavaPlugins
     * @see <a href="https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/diff/src/main/java/org/bukkit/plugin/java/PluginClassLoader.java?until=c3aeaea0fb88600643e01b6b4259e9d5da49e0e7">PluginClassLoader</a>
     */
    @Deprecated
    protected final void removeFromJavaPluginLoaderScope(String className) {
        PluginLoader likelyJavaPluginLoader = pluginLoader.getJavaPluginLoader();
        if (likelyJavaPluginLoader instanceof JavaPluginLoader) {
            JavaPluginLoader javaPluginLoader = (JavaPluginLoader) likelyJavaPluginLoader;

            Runnable removeClass = () -> {
                try {
                    Method method = javaPluginLoader.getClass().getDeclaredMethod("removeClass", String.class);
                    method.setAccessible(true);
                    method.invoke(javaPluginLoader, className);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException tooBad) {
                }
            };

            if (javaPluginClassLoaderParallelCapable()) {
                removeClass.run();
            } else {
                getPluginLoader().getScalaLoader().runInMainThread(removeClass);
            }
        }
    }

    private Boolean canInjectIntoJavaPluginLoaderScope;
    private boolean canInjectIntoJavaPluginLoaderScope() {
        if (canInjectIntoJavaPluginLoaderScope != null) return canInjectIntoJavaPluginLoaderScope;

        //injecting classes into the JavaPluginLoader worked up until January 26th 2020 (MC 1.15.2)
        //https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/diff/src/main/java/org/bukkit/plugin/java/PluginClassLoader.java?until=89586a4cfcc07fe91972d214d80017de887cb8e6
        //related issue: https://hub.spigotmc.org/jira/browse/SPIGOT-4255
        //but it works again since June 11 2021 (MC 1.17.1) after the introduction of the LibraryLoader (that itself got added in Bukkit 1.16.5)
        //https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/diff/src/main/java/org/bukkit/plugin/java/PluginClassLoader.java?until=7e29f7654411f0a17ebbcc2c3f6a7dfe93bff39e

        //so we can return false if 1.15.2 <= bukkitVersion < 1.17.1
        //otherwise we return true

        final String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion.startsWith("1.15.2")
            || bukkitVersion.startsWith("1.16")
            || (bukkitVersion.startsWith("1.17") && !bukkitVersion.startsWith("1.17."))) {
            canInjectIntoJavaPluginLoaderScope = false;
        } else {
            canInjectIntoJavaPluginLoaderScope = true;
        }

        return canInjectIntoJavaPluginLoaderScope;
    }

    private Boolean scalaLoaderClassLoaderParallelCapable = null;
    private boolean javaPluginClassLoaderParallelCapable() {
        if (scalaLoaderClassLoaderParallelCapable != null) return scalaLoaderClassLoaderParallelCapable;

        scalaLoaderClassLoaderParallelCapable = false;
        ClassLoader javaPluginClassLoader = pluginLoader.getScalaLoader().getClass().getClassLoader();
        try {
            Method method = javaPluginClassLoader.getClass().getMethod("isRegisteredAsParallelCapable", new Class<?>[0]);
            scalaLoaderClassLoaderParallelCapable = (Boolean) method.invoke(javaPluginClassLoader, new Object[0]);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassCastException ignored) {
        }
        return scalaLoaderClassLoaderParallelCapable;
    }

    /**
     * <p>
     *     Adds a url to this classloader. This will allow more classes to be found that the plugin can then depend on.
     * </p>
     * <p>
     *     <b>Only use this if you know what you are doing!!!</b>
     * </p>
     *
     * @apiNote Be sure to call this in the constructor or initializer of your plugin, and don't use the dependency before that point or else you will get a {@link NoClassDefFoundError}
     * @deprecated use <a href="https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/plugin/PluginDescriptionFile.html#getLibraries()">libraries</a> instead.
     *              The only reason this method still exist is that that method does not support user-defined repositories yet.
     *
     * @param url the location of the dependency
     */
    @Deprecated
    //in the future when the dependency api is added, annotate this with @Deprecated and @Replaced and redirect calls at class-load time
    public final void addUrl(URL url) {
        libraryLoader.addURL(url);
    }

    /**
     * If you are calling this (which is only possible reflectively on JDK 16 and earlier), then you already know that what you are doing is considered bad practice.
     * <br>
     * If you are using some kind of dependency loader framework, please update it to use {@link #addUrl(URL)} instead, or better: don't even use it at all because it's doing hacky stuff!
     * @param url the location of the dependency
     */
    @Deprecated
    @Override
    protected void addURL(URL url) {
        addUrl(url);
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, Class<?>> entry : classes.entrySet()) {
            String className = entry.getKey();
            Class<?> clazz = entry.getValue();
            if (pluginLoader.removeClassGlobally(scalaRelease, className, clazz)) {
                if (canInjectIntoJavaPluginLoaderScope()) {
                    removeFromJavaPluginLoaderScope(className);
                }
            }
        }
        classes.clear();

        try {
            super.close();
        } finally {
            jarFile.close();
        }
    }
}