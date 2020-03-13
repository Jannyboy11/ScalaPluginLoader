package xyz.janboerman.scalaloader.plugin;

//import net.glowstone.GlowServer;
//import net.glowstone.util.GlowUnsafeValues;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.event.transform.EventTransformations;
import xyz.janboerman.scalaloader.event.transform.EventUsageError;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
public class ScalaPluginClassLoader extends URLClassLoader {

    private enum Platform {
        CRAFTBUKKIT {
            private MethodHandle commodoreConvert = null;

            @Override
            protected byte[] transform(String jarEntryPath, byte[] original, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
                if (commodoreConvert == null) {
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    Server craftServer = currentPluginClassLoader.getServer();
                    Class<?> commodoreClass = Class.forName(
                            craftServer.getClass().getPackageName() + ".util.Commodore");
                    String methodName = "convert";
                    MethodType methodType =  MethodType.methodType(byte[].class, new Class<?>[] {byte[].class, boolean.class});
                    commodoreConvert = lookup.findStatic(commodoreClass, methodName, methodType);
                }

                boolean isModern = currentPluginClassLoader.getApiVersion() != ApiVersion.LEGACY;
                return (byte[]) commodoreConvert.invoke(original, isModern);
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

        protected byte[] transform(String jarEntryPath, byte[] original, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
            Server server = currentPluginClassLoader.getServer();
            UnsafeValues unsafeValues = server.getUnsafe();
            String fakeDescription = "name: Fake" + System.lineSeparator() +
                    "version: 1.0" + System.lineSeparator() +
                    "main: xyz.janboerman.scalaloader.FakePlugin" + System.lineSeparator() +
                    "api-version: " + currentPluginClassLoader.getApiVersion().getVersionString() + System.lineSeparator();

            PluginDescriptionFile pluginDescriptionFile = new PluginDescriptionFile(new StringReader(fakeDescription));

            return unsafeValues.processClass(pluginDescriptionFile, jarEntryPath, original);
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
                                     ApiVersion apiVersion) throws IOException {
        super(urls, parent);

        this.pluginLoader = pluginLoader;
        this.scalaVersion = parent.getScalaVersion();

        this.server = server;
        this.extraPluginYaml = extraPluginYaml;
        this.pluginJarFile = pluginJarFile;
        this.jarFile = new JarFile(pluginJarFile);
        this.apiVersion = apiVersion;

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
     * @return a
     * @throws ClassNotFoundException
     */
    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        //load order:
        //  1.  the plugin's jar
        //  2.  other scalaplugins
        //  3.  javaplugins, Bukkit/NMS classes (parent)

        ClassNotFoundException fallback = new ClassNotFoundException("Could not find class: " + name + ".");
        Class<?> clazz;

        try {
            //findClass tries to find the class in the ScalaPlugin's jar first,
            //if that fails, it attempts to find the class in other ScalaPlugins using the ScalaPluginLoader
            clazz = findClass(name, true);
            if (clazz != null) return clazz;
        } catch (ClassNotFoundException e) {
            fallback.addSuppressed(e);
            try {
                clazz = getParent().loadClass(name);
                if (clazz != null) return clazz;
            } catch (ClassNotFoundException e2) {
                fallback.addSuppressed(e2);
            }
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
    public Class<?> findClass(final String name, boolean searchInScalaPluginLoader) throws ClassNotFoundException {
        //search in cache
        Class<?> found = classes.get(name);
        if (found != null) return found;

        //search in our own jar
        try {
            //do a manual search so that we can transform the class bytes.
            String path = name.replace('.', '/') + ".class";
            JarEntry jarEntry = jarFile.getJarEntry(path);

            if (jarEntry != null) {
                //a classfile exists for the given class name
                try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                    byte[] classBytes = inputStream.readAllBytes();

                    try {
                        classBytes = EventTransformations.transform(classBytes, this);
                    } catch (EventUsageError throwable) {
                        getPluginLoader().getScalaLoader().getLogger().log(Level.SEVERE, "Event class " + name + " is invalid.", throwable);
                        throw new ClassNotFoundException(throwable.getMessage());
                    }

                    try {
                        classBytes = platform.transform(path, classBytes, this);
                    } catch (Throwable throwable) {
                        if (!(throwable instanceof NoSuchMethodError)) {
                            getPluginLoader().getScalaLoader().getLogger().log(Level.SEVERE, "Could not transform class: " + path, throwable);
                        } //else: running on bukkit 1.12.2 or earlier
                        //just continue with the original classBytes
                    }

                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex != -1) {
                        String packageName = name.substring(0, dotIndex);
                        if (getDefinedPackage(packageName) == null) {
                            try {
                                Manifest manifest = jarFile.getManifest();
                                if (manifest != null) {
                                    definePackage(packageName, manifest, this.getURLs()[0]);
                                } else {
                                    definePackage(packageName, null, null, null, null, null, null, null);
                                }
                            } catch (IllegalArgumentException e) {
                                if (getDefinedPackage(packageName) == null) {
                                    throw new IllegalStateException("Cannot find package " + packageName);
                                }
                            }
                        }
                    }

                    CodeSigner[] codeSigners = jarEntry.getCodeSigners();
                    CodeSource codeSource = new CodeSource(getURLs()[0], codeSigners);

                    found = defineClass(name, classBytes, 0, classBytes.length, codeSource);
                } catch (IOException e) {
                    throw new ClassNotFoundException("Could not find class: " + name, e);
                }
            }

            if (found == null) {
                //fallback to URLClassLoader - only needed because plugin authors might call addURL reflectively
                //this block can be removed once we implement a library loading api
                //see https://hub.spigotmc.org/jira/browse/SPIGOT-3723
                found = super.findClass(name);
            }
        } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }

        //TODO search in library classloaders?

        //search in other ScalaPlugins
        if (found == null && searchInScalaPluginLoader) {
            try {
                found = pluginLoader.getScalaPluginClass(getScalaVersion(), name);
            } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }
        }

        if (found == null) {
            throw new ClassNotFoundException("Could not find class " + name + ".");
        }

        final Class<?> loadedConcurrently = classes.putIfAbsent(name, found);
        if (loadedConcurrently == null) {
            //if we find this class for the first time
            if (pluginLoader.addClassGlobally(getScalaVersion(), name, found)) {
                injectIntoJavaPluginLoaderScope(name, found);
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
     */
    protected final void injectIntoJavaPluginLoaderScope(String className, Class<?> clazz) {
        PluginLoader likelyJavaPluginLoader = pluginLoader.getJavaPluginLoader();
        //TODO loop the plugin(class)loader hierarchy until we find a JavaPluginLoader?
        //TODO this seems impossible to do properly because bukkit provides no api to go from Plugin or ClassLoader to PluginLoader.
        //TODO best we can do is hardcode checks for common plugin loaders - the JavaPluginLoader and the ScalaPluginLoader (and maybe an EtaPluginLoader in the future? :))

        if (likelyJavaPluginLoader instanceof JavaPluginLoader) {
            JavaPluginLoader javaPluginLoader = (JavaPluginLoader) likelyJavaPluginLoader;
            //JavaPluginLoader#setClass is not thread-safe. Bukkit calls this method in PluginClassLoader#findClass, similar to what we're doing here.
            //That would have been a concurrency bug if the classloader for JavaPlugins was parallel capable, but it turns out,
            //that the regular PluginClassLoaders are NOT parallel capable. so... yeah. Bukkit is weird/stupid sometimes.

            //To make sure we don't confuse JavaPlugins we use the main thread to call the JavaPluginLoader#setClass(String, Class) method.
            //I wish bukkit provided me with a lock to do this safely asynchronously.
            getPluginLoader().getScalaLoader().runInMainThread(() -> {
                try {
                    Method method = javaPluginLoader.getClass().getDeclaredMethod("setClass", String.class, Class.class);
                    method.setAccessible(true);
                    method.invoke(javaPluginLoader, className, clazz);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException tooBad) {
                    //too bad - JavaPlugins won't be able to depend on this ScalaPlugin.
                }
            });
        }
    }

    /**
     * Removes a class from the {@link JavaPluginLoader}'s global classes cache.
     *
     * @param className the name of the class
     * @see #injectIntoJavaPluginLoaderScope(String, Class)
     */
    protected final void removeFromJavaPluginLoaderScope(String className) {
        PluginLoader likelyJavaPluginLoader = pluginLoader.getJavaPluginLoader();
        if (likelyJavaPluginLoader instanceof JavaPluginLoader) {
            JavaPluginLoader javaPluginLoader = (JavaPluginLoader) likelyJavaPluginLoader;
            getPluginLoader().getScalaLoader().runInMainThread(() -> {
                try {
                    Method method = javaPluginLoader.getClass().getDeclaredMethod("removeClass", String.class);
                    method.setAccessible(true);
                    method.invoke(javaPluginLoader, className);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException tooBad) {
                }
            });
        }
    }

}