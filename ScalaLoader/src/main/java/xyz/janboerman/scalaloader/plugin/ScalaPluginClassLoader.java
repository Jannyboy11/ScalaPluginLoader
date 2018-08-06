package xyz.janboerman.scalaloader.plugin;

import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
public class ScalaPluginClassLoader extends URLClassLoader {

    private final String scalaVersion;
    private final ScalaPluginLoader pluginLoader;

    private final Map<String, Class<?>> classes = new HashMap<>();

    //package protected consturctor by design
    ScalaPluginClassLoader(ScalaPluginLoader pluginLoader, URL[] urls, ScalaLibraryClassLoader parent) {
        super(urls, parent);
        this.pluginLoader = pluginLoader;
        this.scalaVersion = parent.getScalaVersion();
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        Class<?> found = classes.get(name);
        if (found != null) return found;

        Optional<Class<?>> fromScalaPluginLoader = pluginLoader.getScalaPluginClass(getScalaVersion(), name);
        if (fromScalaPluginLoader.isPresent()) return fromScalaPluginLoader.get();

        found = super.findClass(name);
        if (found == null) throw new ClassNotFoundException("Could not find class " + name);

        if (!(found.getClassLoader() instanceof ScalaLibraryClassLoader)) {
            //is this logic sound? I think at this point we will cache all classes that we find, except for the scala library ones, in our map.
            //that means that event the bukkit/craftbukkit/nms classes will be stored in our map.
            //the standard PluginClassLoader that loads JavaPlugins does this too though. y though.
            classes.put(name, found);
            pluginLoader.addClassGlobally(getScalaVersion(), name, found);

            //try to inject into the JavaPluginLoader cache so that JavaPlugins can find our ScalaPlugin classes.
            injectIntoJavaPluginLoaderScope(found);
        }

        return found;
    }

    @Override
    public void close() throws IOException {
        classes.values().forEach(this::removeFromJavaPluginLoaderScope);
        classes.clear();
        super.close();
    }

    public String getScalaVersion() {
        return scalaVersion;
    }


    private void injectIntoJavaPluginLoaderScope(Class<?> clazz) {
        PluginLoader likelyJavaPluginLoader = pluginLoader.getJavaPluginLoader();
        if (likelyJavaPluginLoader instanceof JavaPluginLoader) {
            JavaPluginLoader javaPluginLoader = (JavaPluginLoader) likelyJavaPluginLoader;
            try {
                Method method = javaPluginLoader.getClass().getDeclaredMethod("setClass", String.class, Class.class);
                method.setAccessible(true);
                method.invoke(javaPluginLoader, clazz.getName(), clazz);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException tooBad) {
                //too bad - JavaPlugins won't be able to depend on this ScalaPlugin.
            }
        }
    }

    private void removeFromJavaPluginLoaderScope(Class<?> clazz) {
        PluginLoader likelyJavaPluginLoader = pluginLoader.getJavaPluginLoader();
        if (likelyJavaPluginLoader instanceof JavaPluginLoader) {
            JavaPluginLoader javaPluginLoader = (JavaPluginLoader) likelyJavaPluginLoader;
            try {
                Method method = javaPluginLoader.getClass().getDeclaredMethod("removeClass", String.class);
                method.setAccessible(true);
                method.invoke(javaPluginLoader, clazz.getName());
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException tooBad) {
            }
        }
    }

}