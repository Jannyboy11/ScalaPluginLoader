package xyz.janboerman.scalaloader.plugin;

//import net.glowstone.GlowServer;
//import net.glowstone.util.GlowUnsafeValues;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.*;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.transform.*;
import xyz.janboerman.scalaloader.event.transform.EventTransformations;
import xyz.janboerman.scalaloader.event.transform.EventError;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public class ScalaPluginClassLoader extends URLClassLoader {

    private enum Platform {

        CRAFTBUKKIT {
            private MethodHandle commodoreConvert = null;
            private boolean attempted = false;

            @Override
            protected byte[] transform(String jarEntryPath, byte[] classBytes, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
                if (!attempted) {
                    attempted = true;
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    Server craftServer = currentPluginClassLoader.getServer();
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
                    boolean isModern = currentPluginClassLoader.getApiVersion() != ApiVersion.LEGACY;
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
//                glowUnsafeValues.processClass() -- not yet implemented in the GlowStone 1.15 branch
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
                            "main: xyz.janboerman.scalaloader.FakePlugin" + System.lineSeparator() +
                            "api-version: " + currentPluginClassLoader.getApiVersion().getVersionString() + System.lineSeparator();

                    PluginDescriptionFile pluginDescriptionFile = new PluginDescriptionFile(new StringReader(fakeDescription));

                    byte[] processed = unsafeValues.processClass(pluginDescriptionFile, jarEntryPath, original);
                    conversionMethodExists = true;
                    return processed;
                } catch (NoSuchMethodError e) {
                    //UnsafeValues#processClass does not exist, just return the original class bytes
                }
            }

            return original;
        }
    }

    static {
        registerAsParallelCapable();
    }

    private final String scalaVersion;
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
                                     TransformerRegistry transformerRegistry) throws IOException {
        super(urls, parent);

        this.pluginLoader = pluginLoader;
        this.scalaVersion = parent.getScalaVersion();

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
    }

    /**
     * Get the version of Scala used for the plugin loaded by this class loader.
     * @return the scala version
     */
    public String getScalaVersion() {
        return scalaVersion;
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
        //  1.  the plugin's jar
        //  2.  other scalaplugins
        //  3.  scala standard lib, javaplugins, Bukkit/NMS classes (parent)

        ClassNotFoundException fallback = new ClassNotFoundException(name);
        Class<?> clazz;

        try {
            //findClass tries to find the class in the ScalaPlugin's jar first,
            //if that fails, it attempts to find the class in other ScalaPlugins using the ScalaPluginLoader
            clazz = findClass(name, true);
            if (clazz != null) return clazz;
        } catch (ClassNotFoundException e) {
            fallback.addSuppressed(e);
        }

        try {
            //the parent classloader has access to:
            //  - scala library classes
            //  - javaplugins
            //  - server classes (bukkit, craftbukkit, nms, and server-included libraries)

            clazz = getParent().loadClass(name);
            if (clazz != null) return clazz;
        } catch (ClassNotFoundException e) {
            fallback.addSuppressed(e);
        }

        throw fallback;
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

//                    // ====== DEBUG TypeSignature ======
//
//                    ClassReader debugReader = new ClassReader(classBytes);
//                    debugReader.accept(new ClassVisitor(AsmConstants.ASM_API) {
//                        private boolean debug;
//
//                        @Override
//                        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//                            if ("xyz/janboerman/scalaloader/example/java/ArraySerializable".equals(name)
//                                || "xyz/janboerman/scalaloader/example/java/ListSerializable".equals(name)) {
//                                System.out.println("visiting " + name);
//                                debug = true;
//                            }
//                        }
//
//                        @Override
//                        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
//                            if (debug) {
//                                TypeSignature typeSignature = signature != null ? TypeSignature.ofSignature(signature) : TypeSignature.ofDescriptor(descriptor);
//                                System.out.println(typeSignature);
//                            }
//                            return null;
//                        }
//                    }, 0);
//
//                    // ====== END OF DEBUG ======

                    //TODO make it possible for TransformerRegistry to apply early transformations, before the event, configurationserialization transformations apply.
                    //TODO I may want to use this for sum types to generate the @ConfigurationSerialization annotation once Scan.Type.AUTO_DETECT is implemented!

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

                    //apply transformations that were registered by other classes (also used by configuration serialization)
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

                    // Note to self 2020-11-11:
                    // If I ever get a java.lang.ClassFormatError: Invalid length 65526 in LocalVariableTable in class file com/example/MyClass
                    // then the cause was: visitLocalVariable was not called before visitMaxes and visitEnd, but way earlier!
                    // this is not explained by the order documented in the MethodVisitor class!

                    //apply bukkit bytecode transformations
                    try {
                        classBytes = platform.transform(path, classBytes, this);
                    } catch (Throwable throwable) {
                        getPluginLoader().getScalaLoader().getLogger().log(Level.SEVERE, "Server implementation could not transform class: " + path, throwable);
                    }


                    //define the package
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex != -1) {
                        String packageName = name.substring(0, dotIndex);
                        //TODO use getDefinedPackage in Java11+
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
                found = pluginLoader.getScalaPluginClass(getScalaVersion(), name); /*Do I want this here? not in the loadClass method?*/
            } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }
        }

        if (found == null) {
            throw new ClassNotFoundException(name);
        }

        final Class<?> loadedConcurrently = classes.putIfAbsent(name, found);
        if (loadedConcurrently == null) {
            //if we find this class for the first time
            if (pluginLoader.addClassGlobally(getScalaVersion(), name, found)) {
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
        //best we can do is hardcode checks for common plugin loaders - the JavaPluginLoader and the ScalaPluginLoader (and maybe an EtaPluginLoader in the future? :))
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
            ScalaLoader scalaLoader = pluginLoader.getScalaLoader();
            if (javaPluginClassLoaderParallelCapable()) {
                //If JavaPlugins' classloader are parallel capable, just run the action, no matter what thread we are on.
                setClass.run();
            } else {
                //If not, schedule the action to be run in the main thread.
                scalaLoader.runInMainThread(setClass);
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

}