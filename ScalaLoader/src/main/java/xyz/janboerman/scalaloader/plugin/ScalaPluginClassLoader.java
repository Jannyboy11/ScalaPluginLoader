package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
public class ScalaPluginClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final String scalaVersion;
    private final ScalaPluginLoader pluginLoader;
    private final Server server;
    private final Map<String, Object> extraPluginYaml;
    private final File pluginJarFile;
    private final String apiVersion;

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
     * @param apiVersion bukkit's api version that's used by the plugin, see {@link ApiVersion#getVersionString()}.
     */
    protected ScalaPluginClassLoader(ScalaPluginLoader pluginLoader,
                                     URL[] urls,
                                     ScalaLibraryClassLoader parent,
                                     Server server,
                                     Map<String, Object> extraPluginYaml,
                                     File pluginJarFile,
                                     String apiVersion) {
        super(urls, parent);

        this.pluginLoader = pluginLoader;
        this.scalaVersion = parent.getScalaVersion();

        this.server = server;
        this.extraPluginYaml = extraPluginYaml;
        this.pluginJarFile = pluginJarFile;
        this.apiVersion = apiVersion;
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
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * Finds and loads a class used by the ScalaPlugin loaded by this ClassLoader.
     *
     * @param name the name of the class to be found
     * @return a class with the given name, if found
     * @throws ClassNotFoundException if no class with the given name could be found
     * @see #findClass(String, boolean)
     */
    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        return findClass(name, true);
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
            found = super.findClass(name);
        } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }

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