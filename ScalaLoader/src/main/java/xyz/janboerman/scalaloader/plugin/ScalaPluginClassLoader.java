package xyz.janboerman.scalaloader.plugin;

//import net.glowstone.GlowServer;
//import net.glowstone.util.GlowUnsafeValues;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.transform.*;
import xyz.janboerman.scalaloader.event.transform.EventTransformations;
import xyz.janboerman.scalaloader.event.transform.EventError;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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
import java.util.logging.Level;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
@Called
public class ScalaPluginClassLoader extends URLClassLoader {

    private enum Platform {

        CRAFTBUKKIT {
            private MethodHandle commodoreConvert = null;
            private boolean attempted = false;

            @Override
            protected byte[] transform(String jarEntryPath, byte[] classBytes, ScalaPluginClassLoader pluginClassLoader) throws Throwable {
                if (!attempted) {
                    attempted = true;
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    Server craftServer = pluginClassLoader.getServer();
                    try {
                        Class<?> commodoreClass = Class.forName(
                                craftServer.getClass().getPackage().getName() + ".util.Commodore"); //use getClass().getPackageName() in Java11+
                        String methodName = "convert";
                        MethodType methodType = MethodType.methodType(byte[].class, new Class<?>[]{byte[].class, boolean.class});
                        commodoreConvert = lookup.findStatic(commodoreClass, methodName, methodType);
                    } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                        //running on craftbukkit 1.12.2 or earlier
                    }
                }

                if (commodoreConvert != null) {
                    boolean isModern = pluginClassLoader.getApiVersion() != ApiVersion.LEGACY;
                    classBytes = (byte[]) commodoreConvert.invoke(classBytes, isModern);
                }

                return classBytes;
            }
        },
        GLOWSTONE {
//            @Override
//            public byte[] transform(String jarEntryPath, byte[] original, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
//                GlowServer glowServer = (GlowServer) currentPluginClassLoader.getServer();
//                GlowUnsafeValues glowUnsafeValues = (GlowUnsafeValues) glowServer.getUnsafe();
//                glowUnsafeValues.processClass() -- not yet implemented in the GlowStone 1.16 branch
//            }
        },
        UNKNOWN;


        private Boolean conversionMethodExists = null;

        protected byte[] transform(String jarEntryPath, byte[] original, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
            if (conversionMethodExists == null || conversionMethodExists) {
                try {
                    Server server = currentPluginClassLoader.getServer();
                    UnsafeValues unsafeValues = server.getUnsafe();
                    String fakeDescription = "name: Fake" + System.lineSeparator() +
                            "version: 1.0" + System.lineSeparator() +
                            "main: xyz.janboerman.scalaloader.FakePlugin" + System.lineSeparator();
                    ApiVersion apiVersion = currentPluginClassLoader.getApiVersion();
                    if (apiVersion != ApiVersion.LEGACY) {
                        //If api-version is not set, this will be ApiVersion.latest(). We assume all ScalaPlugins are made in the post-1.13 era.
                        fakeDescription += "api-version: " + apiVersion.getVersionString() + System.lineSeparator();
                    }

                    PluginDescriptionFile pluginDescriptionFile = new PluginDescriptionFile(new StringReader(fakeDescription));

                    byte[] processed = unsafeValues.processClass(pluginDescriptionFile, jarEntryPath, original);
                    conversionMethodExists = true;
                    return processed;
                } catch (NoSuchMethodError e) {
                    //UnsafeValues#processClass does not exist, just return the original class bytes
                    conversionMethodExists = false;
                }
            }

            return original;
        }
    }

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
    private final Platform platform;
    private final String mainClassName;
    private final TransformerRegistry transformerRegistry;

    private final ConcurrentMap<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final ScalaPlugin plugin;

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
     */
    protected ScalaPluginClassLoader(ScalaPluginLoader pluginLoader,
                                     URL[] urls,
                                     ScalaLibraryClassLoader parent,
                                     Server server,
                                     Map<String, Object> extraPluginYaml,
                                     File pluginJarFile,
                                     ApiVersion apiVersion,
                                     String mainClassName,
                                     TransformerRegistry transformerRegistry) throws IOException, ScalaPluginLoaderException, ClassNotFoundException {
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

        Platform platform = Platform.UNKNOWN;
        if (server.getClass().getName().startsWith("org.bukkit.craftbukkit")) {
            platform = Platform.CRAFTBUKKIT;
        } else if (server.getClass().getName().startsWith("net.glowstone")) {
            platform = Platform.GLOWSTONE;
        }
        this.platform = platform;

        this.plugin = createPluginInstance((Class<? extends ScalaPlugin>) Class.forName(mainClassName, true, this));
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
     *     <li>Search for the class in the ScalaPlugin's own jar file</li>
     *     <li>Search for the class in other ScalaPlugins</li>
     *     <li>Search for the class in JavaPlugins and Bukkit/Server implementation classes</li>
     * </ol>
     *
     * @param name the name of the class
     * @return a class with the given name
     * @throws ClassNotFoundException if a class could not be found by this classloader
     */
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        //load order:
        //  0.  scala standard lib (if applicable)
        //  1.  the plugin's jar
        //  2.  other scalaplugins
        //  3.  javaplugins, Bukkit/NMS classes (parent)

        if (name.startsWith("scala.") || name.startsWith("dotty.")) {
            //short-circuit scala standard library and dotty/tasty classes.
            //we do this because if some plugin or library (such as PDM) adds the scala library to this classloader using #addUrl(URL),
            //then we still want to use the scala library that is loaded by the parent classloader
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        ClassNotFoundException fallback = new ClassNotFoundException(name);
        Class<?> clazz;

        try {
            //findClass tries to find the class in the ScalaPlugin's jar first,
            //if that fails, it attempts to find the class in other ScalaPlugins using the ScalaPluginLoader
            //note to self april 2021: since addUrl is now exposed publicly, findClass may also find classes that were added by a dependency loader
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


    private boolean debugClassLoad(String className) {
        return getPluginLoader().debugClassNames().contains(className);
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

                    //apply event bytecode transformations
                    try {
                        classBytes = EventTransformations.transform(classBytes, this);
                    } catch (EventError eventError) {
                        getPluginLoader().getScalaLoader().getLogger().log(Level.SEVERE, "Event class " + name + " is not valid", eventError);
                    }

                    //apply configurationserializable bytecode transformations
                    try {
                        classBytes = ConfigurationSerializableTransformations.transform(classBytes, this);
                    } catch (ConfigurationSerializableError configSerError) {
                        getPluginLoader().getScalaLoader().getLogger().log(Level.SEVERE, "ConfigurationSerializable class " + name + " is not valid", configSerError);
                    }

                    //apply transformations that were registered by other classes
                    //
                    // This is used by the configuration-serializable framework to let the serializable classes be registered at bukkit's ConfigurationSerialization.
                    // Extra calls in methods in the plugin's main class are therefore added.
                    // Extra transformers are also registered for sum type serialization.
                    // They run on the subclasses so that their 'variant' is added to the Map<String, Object>
                    //
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

                        //apply other transformations
                        List<BiFunction<ClassVisitor, String, ClassVisitor>> targetedTransformers = transformerRegistry.byClassTransformers.get(name);
                        if (targetedTransformers != null) {
                            for (BiFunction<ClassVisitor, String, ClassVisitor> targetedTransformer : targetedTransformers) {
                                classVisitor = targetedTransformer.apply(classVisitor, mainClassName);
                            }
                        }

                        //if there were any transformers, then apply the transformations!
                        if (classVisitor != classWriter) {
                            ClassReader classReader = new ClassReader(classBytes);
                            classReader.accept(classVisitor, 0);
                            classBytes = classWriter.toByteArray();
                        }
                    }

                    //dump the class to the log in case classloading debugging was enabled for this class
                    if (debugClassLoad(name)) {
                        getPluginLoader().getScalaLoader().getLogger().info("[DEBUG] Dumping bytecode for class " + name);
                        ClassReader debugReader = new ClassReader(classBytes);
                        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, new Textifier(), new PrintWriter(System.out));
                        debugReader.accept(traceClassVisitor, 0);
                    }

                    // Note to self 2020-11-11:
                    // If I ever get a java.lang.ClassFormatError: Invalid length 65526 in LocalVariableTable in class file com/example/MyClass
                    // then the cause was: visitLocalVariable was not called before visitMaxes and visitEnd, but way earlier!
                    // this is not explained by the order documented in the MethodVisitor class!

                    //apply bukkit bytecode transformations
                    try {
                        classBytes = platform.transform(path, classBytes, this);
                    } catch (Throwable throwable) {
                        getPluginLoader().getScalaLoader().getLogger().log(Level.SEVERE, "Server implementation could not transform class: " + path, throwable);
                        //the reason we don't give up here is we do still want to load the class even though bukkit could not transform it.
                        //this most likely happened because its version of the ASM api is not up to date with recent java language features.
                        //it is very possible that the class that used a recent java language feature (e.g. sealed types) does not need the Material rewrite treatment.
                    }


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

            if (found == null) {
                //fallback to URLClassLoader - only needed because plugin authors might call addURL reflectively
                //this block can be removed once we implement a library loading api
                //see https://hub.spigotmc.org/jira/browse/SPIGOT-3723
                found = super.findClass(name);
                //note that bytecode transformations are NOT applied to classes that were loaded this way!!
            }
        } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }

        //TODO search in library classloaders?

        //search in other ScalaPlugins
        if (found == null && searchInScalaPluginLoader) {
            try {
                found = pluginLoader.getScalaPluginClass(getScalaRelease(), name); /*Do I want this here? not in the loadClass method?*/
            } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }
        }

        if (found == null) {
            throw new ClassNotFoundException(name);
        }

        final Class<?> loadedConcurrently = classes.putIfAbsent(name, found);
        if (loadedConcurrently == null) {
            //if we find this class for the first time
            if (pluginLoader.addClassGlobally(getScalaRelease(), name, found)) {
                //TODO will bukkit ever get a proper pluginloader api? https://hub.spigotmc.org/jira/browse/SPIGOT-4255
                //injectIntoJavaPluginLoaderScope(name, found);
            }
        } else {
            //if some other thread tried to load the same class and won the race, use that class instead.
            found = loadedConcurrently;
        }

        //we don't search in the parent classloader explicitly - this is done by the loadClass method.
        return found;
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
     * Similar to {@link ScalaPluginLoader#addClassGlobally(String, String, Class)}, the {@link JavaPluginLoader} also has such a method: setClass.
     * This is used by {@link org.bukkit.plugin.java.JavaPlugin}s to make their classes accessible to other JavaPlugins.
     * Since we want a  {@link ScalaPlugin}'s classes to be accessible to JavaPlugins, this method can be called to share a class with ALL JavaPlugins and ScalaPlugins.
     * This is dangerous business because it can pollute the JavaPluginLoader with classes for multiple (binary incompatible) versions of Scala.
     * <br>
     * Be sure to call {@link #removeFromJavaPluginLoaderScope(String)} again when the class is no longer needed to prevent memory leaks.
     *
     * @param className the name of the class
     * @param clazz the class
     *
     * @deprecated JavaPlugins that try to find classes using the JavaPluginLoader expect to only find JavaPlugins
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
     *     <b>Only use this if you know that you are doing!!!</b>
     * </p>
     *
     * @apiNote Be sure to call this in the constructor or initializer of your plugin, and don't use the dependency before that point or else you will get a {@link NoClassDefFoundError}
     * @apiNote This method will become deprecated once ScalaLoader gets its own dependency framework
     *
     * @param url the location of the dependency
     * @deprecated Spigot itself is getting the ability to load plugins from Maven central, meaning that could just upload a dummy artifact there that depends on all your real plugin dependencies. see <a href="https://hub.spigotmc.org/jira/browse/SPIGOT-6419?focusedCommentId=38865&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-38865">SPIGOT-6419</a>
     */
    //in the future when the dependency api is added, annotate this with @Deprecated and @Replaced and redirect calls at class-load time
    public final void addUrl(URL url) {
        super.addURL(url);
    }

}