package xyz.janboerman.scalaloader.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

public class ScalaPluginClassLoader extends URLClassLoader {


    public ScalaPluginClassLoader(URL[] urls, ScalaLibraryClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> findClass(String name) {
        //TODO

        return null;
    }

}

/**
 * ClassLoader that loads scala library classes.
 * The {@link ScalaPluginLoader} will create instances per scala version.
 */
//TODO using this class loader hierarchy I probably don't even have to relocate the scala classes?? :))
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
