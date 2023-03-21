package xyz.janboerman.scalaloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * ClassLoader that loads scala library classes.
 */
public class ScalaLibraryClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final String scalaVersion;

    public ScalaLibraryClassLoader(String scalaVersion, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.scalaVersion = Objects.requireNonNull(scalaVersion, "Scala version cannot be null!");
    }

    public String getScalaVersion() {
        return scalaVersion;
    }

}

