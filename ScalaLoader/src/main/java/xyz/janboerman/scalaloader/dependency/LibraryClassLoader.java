package xyz.janboerman.scalaloader.dependency;

import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.util.ClassLoaderUtils;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is NOT part of the public API!
 */
public class LibraryClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final ConcurrentHashMap<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final File[] jarFiles;
    private final Logger logger;
    private final ScalaPluginClassLoader plugin;
    private final TransformerRegistry transformerRegistry;

    public LibraryClassLoader(File[] jarFiles, ScalaLibraryClassLoader parent, Logger logger, ScalaPluginClassLoader plugin, TransformerRegistry transformerRegistry) {
        super(urls(jarFiles), parent);
        this.jarFiles = jarFiles;
        this.logger = logger;
        this.plugin = plugin;
        this.transformerRegistry = transformerRegistry;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        //search in cache
        Class<?> found = classes.get(name);
        if (found != null) return found;

        //search in jars
        String path = name.replace('.', '/') + ".class";
        for (File file : jarFiles) {
            try {
                JarFile jarFile = Compat.jarFile(file);
                JarEntry jarEntry = jarFile.getJarEntry(path);

                if (jarEntry != null) {
                    try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        byte[] classBytes = Compat.readAllBytes(inputStream);
                        URL url = file.toURI().toURL();

                        //transform the bytecode
                        classBytes = ClassLoaderUtils.transform(name, classBytes, this, transformerRegistry, plugin, logger);

                        //define the package
                        int dotIndex = name.lastIndexOf('.');
                        if (dotIndex != -1) {
                            String packageName = name.substring(0, dotIndex);
                            if (getPackage(packageName) == null) {
                                try {
                                    Manifest manifest = jarFile.getManifest();
                                    if (manifest != null) {
                                        definePackage(packageName, manifest, url);
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
                        CodeSource codeSource = new CodeSource(url, codeSigners);
                        found = defineClass(name, classBytes, 0, classBytes.length, codeSource);

                        //cache the class, possibly racing against other threads
                        Class<?> newClass = found;
                        return classes.computeIfAbsent(name, k -> newClass);
                    }
                }

            } catch (IOException e) {
                logger.log(Level.SEVERE, "File " + file + ", is not a valid jar file", e);
            }
        }

        //dive in our own jars again, because the class that we are looking for might have become available
        //because somebody possibly called #addURL(URL).
        //see: https://hub.spigotmc.org/jira/browse/SPIGOT-3723
        try {
            super.findClass(name);
        } catch (ClassNotFoundException ignored) {}

        throw new ClassNotFoundException(name);
    }

    private static URL[] urls(File[] files) {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                urls[i] = file.toURI().toURL();
            } catch (MalformedURLException impossibru) {
                throw new RuntimeException("jar file " + file + " has a malformed url" , impossibru);
            }
        }
        return urls;
    }
}
