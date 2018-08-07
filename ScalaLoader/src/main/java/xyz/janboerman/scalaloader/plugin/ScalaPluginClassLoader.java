package xyz.janboerman.scalaloader.plugin;

import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPluginLoader;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
public class ScalaPluginClassLoader extends URLClassLoader {

    private final String scalaVersion;
    private final ScalaPluginLoader pluginLoader;

    private final Map<String, Class<?>> classes = new HashMap<>();

    protected ScalaPluginClassLoader(ScalaPluginLoader pluginLoader, URL[] urls, ScalaLibraryClassLoader parent) {
        super(urls, parent);
        this.pluginLoader = pluginLoader;
        this.scalaVersion = parent.getScalaVersion();
    }

    public String getScalaVersion() {
        return scalaVersion;
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    public Class<?> findClass(final String name, boolean searchInScalaPluginLoader) throws ClassNotFoundException {
        Class<?> found = classes.get(name);
        if (found != null) return found;

        if (searchInScalaPluginLoader) {
            try {
                found = pluginLoader.getScalaPluginClass(getScalaVersion(), name);
                classes.put(name, found);
                return found;
            } catch (ClassNotFoundException e) { /*ignored - continue onwards*/ }
            if (found != null) return found;
        }

        found = super.findClass(name);
        if (found == null) throw new ClassNotFoundException("Could not find class " + name);
        classes.put(name, found);

        if (pluginLoader.addClassGlobally(getScalaVersion(), name, found)) {
            injectIntoJavaPluginLoaderScope(found);
        }

        return found;
    }

    protected Collection<Class<?>> getClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    @Override
    public void close() throws IOException {
        classes.values().forEach(this::removeFromJavaPluginLoaderScope);
        classes.clear();
        super.close();
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