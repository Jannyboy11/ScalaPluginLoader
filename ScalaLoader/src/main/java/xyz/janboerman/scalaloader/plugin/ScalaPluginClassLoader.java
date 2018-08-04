package xyz.janboerman.scalaloader.plugin;

import java.net.URL;
import java.net.URLClassLoader;

public class ScalaPluginClassLoader extends URLClassLoader {

    private String packagePrefix;

    public ScalaPluginClassLoader(String packagePrefix, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.packagePrefix = packagePrefix;
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        //TODO process bytes so that the package names scala packages (and references too scala packages) are changed.


        return null;
    }

}
