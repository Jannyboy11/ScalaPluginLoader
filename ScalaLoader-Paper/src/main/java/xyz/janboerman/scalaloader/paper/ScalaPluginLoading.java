package xyz.janboerman.scalaloader.paper;

import org.objectweb.asm.ClassReader;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.transform.AddVariantTransformer;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanResult;
import xyz.janboerman.scalaloader.configurationserializable.transform.GlobalScanner;
import xyz.janboerman.scalaloader.configurationserializable.transform.PluginTransformer;
import xyz.janboerman.scalaloader.dependency.PluginYamlLibraryLoader;
import xyz.janboerman.scalaloader.paper.plugin.PluginJarScanResult;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionClassLoader;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionPlugin;
import xyz.janboerman.scalaloader.paper.plugin.description.MainClassScanner;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ScalaPluginLoading {

    private ScalaPluginLoading() {
    }

    //smaller = better candidate!
    private static final Comparator<MainClassScanner> candidateComparator;
    static {
        Comparator<Optional<?>> optionalComparator = Comparator.comparing(Optional::isEmpty);
        Comparator<String> packageComparator = Comparator.comparing(className -> className.split("\\."), Comparator.comparing(array -> array.length));

        candidateComparator = Comparator.nullsLast(
                Comparator.comparing(MainClassScanner::getMainClass, optionalComparator)
                        .thenComparing(scanner -> !scanner.hasScalaAnnotation())            //better candidate if it has a @Scala annotation
                        .thenComparing(scanner -> !scanner.isSingletonObject())             //better candidate if it is an object
                        .thenComparing(MainClassScanner::extendsObject)                     //worse candidate if it extends Object directly
                        .thenComparing(MainClassScanner::extendsScalaPlugin)                //worse candidate if it extends ScalaPlugin directly
                        .thenComparing(MainClassScanner::getClassName, packageComparator)   //worse candidate if the package consists of a long namespace
                        .thenComparing(MainClassScanner::getClassName)                      //worse candiate if the class name is longer
        );
    }

    static PluginJarScanResult read(JarFile pluginJarFile) throws IOException {
        final PluginJarScanResult result = new PluginJarScanResult();

        MainClassScanner bestCandidate = null;
        TransformerRegistry transformerRegistry = new TransformerRegistry();
        Map<String, Object> pluginYamlData = Compat.emptyMap();

        //enumerate the class files!
        Enumeration<JarEntry> entryEnumeration = pluginJarFile.entries();
        while (entryEnumeration.hasMoreElements()) {
            JarEntry jarEntry = entryEnumeration.nextElement();
            if (jarEntry.getName().endsWith(".class")) {
                InputStream bytecodeStream = pluginJarFile.getInputStream(jarEntry);
                byte[] classBytes = Compat.readAllBytes(bytecodeStream);

                MainClassScanner scanner = new MainClassScanner(classBytes);
                bestCandidate = BinaryOperator.minBy(candidateComparator).apply(bestCandidate, scanner);

                //targeted bytecode transformers
                final GlobalScanResult configSerResult = new GlobalScanner().scan(new ClassReader(classBytes));
                PluginTransformer.addTo(transformerRegistry, configSerResult);
                AddVariantTransformer.addTo(transformerRegistry, configSerResult);
            }
        }

        result.mainClassScanner = bestCandidate;
        result.transformerRegistry = transformerRegistry;

        JarEntry pluginYamlEntry = pluginJarFile.getJarEntry("paper-plugin.yml");
        if (pluginYamlEntry == null) pluginYamlEntry = pluginJarFile.getJarEntry("plugin.yml");
        if (pluginYamlEntry != null) {
            Yaml yaml = new Yaml();
            InputStream pluginYamlStream = pluginJarFile.getInputStream(pluginYamlEntry);
            pluginYamlData = (Map<String, Object>) yaml.loadAs(pluginYamlStream, Map.class);
        }

        result.pluginYaml = pluginYamlData;

        return result;
    }

    static Optional<? extends DescriptionPlugin> buildDescriptionPlugin(
            File file,
            PluginJarScanResult scanResult,
            ApiVersion apiVersion,
            String mainClassName,
            ScalaDependency scalaDependency,
            Logger logger,
            File librariesDir) {
        DescriptionClassLoader classLoader;
        try {
            ScalaLibraryClassLoader scalaLibraryClassLoader = getOrCreateScalaLibrary(scalaDependency);
            ClassLoader libraryLoader = createLibraryClassLoader(scalaLibraryClassLoader, scanResult.pluginYaml, logger, librariesDir);
            classLoader = new DescriptionClassLoader(file, libraryLoader, apiVersion, mainClassName, scalaLibraryClassLoader.getScalaVersion());
        } catch (ScalaPluginLoaderException e) {
            logger.log(Level.SEVERE, "Could not download all libraries from plugin's description.", e);
            return Optional.empty();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not create classloader to load " + file + "'s plugin description.", e);
            return Optional.empty();
        }

        try {
            Class<? extends DescriptionPlugin> clazz = Class.forName(mainClassName, true, classLoader).asSubclass(DescriptionPlugin.class);
            DescriptionPlugin plugin = ScalaLoaderUtils.createScalaPluginInstance(clazz);
            return Optional.of(plugin);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Could not find plugin's main class " + mainClassName + " in file " + file.getName() + ".", e);
            return Optional.empty();
        } catch (ScalaPluginLoaderException e) {
            logger.log(Level.SEVERE, "Could not instantiate plugin instance: " + mainClassName, e);
            return Optional.empty();
        } catch (Throwable throwable) {
            logger.log(Level.SEVERE, "Some error occurred in ScalaPlugin's constructor or initializer. " +
                    "Try to move stuff over to #onLoad() or #onEnable().", throwable);
            return Optional.empty();
        }
    }

    static ClassLoader createLibraryClassLoader(ClassLoader parent, Map<String, Object> pluginYaml, Logger logger, File localRepoDir) throws ScalaPluginLoaderException {
        if (pluginYaml == null || !pluginYaml.containsKey("libraries")) return parent;

        PluginYamlLibraryLoader pluginYamlLibraryLoader = new PluginYamlLibraryLoader(logger, localRepoDir);
        Collection<File> files = pluginYamlLibraryLoader.getJarFiles(pluginYaml);

        URL[] urls = new URL[files.size()];
        int i = 0;
        for (File file : files) {
            try {
                URL url = file.toURI().toURL();
                urls[i] = url;
            } catch (MalformedURLException e) {
                throw new ScalaPluginLoaderException("Malformed URL for file: " + file + "?!", e);
            }
            i += 1;
        }
        return new URLClassLoader(urls, parent);
    }

    static ScalaLibraryClassLoader getOrCreateScalaLibrary(ScalaDependency scalaDependency) throws ScalaPluginLoaderException {
        PluginScalaVersion scalaVersion;

        if (scalaDependency instanceof ScalaDependency.Builtin builtin) {
            scalaVersion = PluginScalaVersion.fromScalaVersion(builtin.scalaVersion());
        } else if (scalaDependency instanceof ScalaDependency.Custom custom) {
            scalaVersion = new PluginScalaVersion(custom.scalaVersion(), custom.urls(), custom.sha1hashes());
        } else if (scalaDependency instanceof ScalaDependency.YamlDefined yamlDefined) {
            scalaVersion = PluginScalaVersion.fromScalaVersion(ScalaVersion.fromVersionString(yamlDefined.scalaVersion()));
        } else {
            scalaVersion = PluginScalaVersion.fromScalaVersion(ScalaVersion.fromVersionString(scalaDependency.getVersionString()));
        }

        return ScalaLoader.getInstance().loadOrGetScalaVersion(scalaVersion);
    }
}
