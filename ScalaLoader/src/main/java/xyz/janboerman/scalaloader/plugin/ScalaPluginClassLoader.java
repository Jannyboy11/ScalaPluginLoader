package xyz.janboerman.scalaloader.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * ClassLoader that loads {@link ScalaPlugin}s.
 * The {@link ScalaPluginLoader} will create instances per scala plugin.
 */
public class ScalaPluginClassLoader extends URLClassLoader {

    private final String scalaVersion;

    public ScalaPluginClassLoader(URL[] urls, ScalaLibraryClassLoader parent) {
        super(urls, parent);
        this.scalaVersion = parent.getScalaVersion();
    }

//    @Override
//    public Class<?> findClass(String name) {
//        //TODO override this so that scala plugins can access classes from other scala plugins.
//        //TODO where do I store the shared set of classes? in the ScalaPluginLoader probably.
//        //TODO while doing this, I might try to put them into the JavaPluginLoader's global classes cache.
//        return super.findClass(name)
//    }

    public String getScalaVersion() {
        return scalaVersion;
    }

}

/**
 * ClassLoader that loads scala library classes.
 * The {@link ScalaPluginLoader} will create instances per scala version.
 */
class ScalaLibraryClassLoader extends URLClassLoader {

    private final String scalaVersion;

    ScalaLibraryClassLoader(String scalaVersion, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.scalaVersion = Objects.requireNonNull(scalaVersion, "Scala version cannot be null!");
    }

    public String getScalaVersion() {
        return scalaVersion;
    }

}
