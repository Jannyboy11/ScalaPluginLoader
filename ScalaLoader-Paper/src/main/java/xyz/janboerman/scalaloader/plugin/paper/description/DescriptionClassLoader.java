package xyz.janboerman.scalaloader.plugin.paper.description;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.group.SingletonPluginClassLoaderGroup;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.plugin.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPluginMeta;
import xyz.janboerman.scalaloader.plugin.paper.transform.MainClassBootstrapTransformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

//this is much like PaperSimplePluginClassLoader
public class DescriptionClassLoader extends URLClassLoader implements ConfiguredPluginClassLoader {

    private final JarFile jarFile;

    private DescriptionPlugin plugin;
    private PluginClassLoaderGroup classLoaderGroup;

    public DescriptionClassLoader(File jarFile, ScalaLibraryClassLoader parent) throws IOException {
        super(new URL[] {jarFile.toURI().toURL()}, parent);
        this.jarFile = Compat.jarFile(jarFile);
    }

    @Override
    public Class<?> findClass(final String className) throws ClassNotFoundException {

        String path = className.replace('.', '/') + ".class";
        JarEntry jarEntry = jarFile.getJarEntry(path);

        if (jarEntry != null) {
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                byte[] byteCode = Compat.readAllBytes(inputStream);

                //transform the bytecode
                ClassWriter classWriter = new ClassWriter(0) {
                    @Override
                    protected ClassLoader getClassLoader() {
                        return DescriptionClassLoader.this;
                    }
                };
                ClassReader classReader = new ClassReader(byteCode);
                classReader.accept(new MainClassBootstrapTransformer(classWriter), ClassReader.EXPAND_FRAMES);
                byteCode = classWriter.toByteArray();

                //define the package
                int dotIndex = className.lastIndexOf('.');
                if (dotIndex != -1) {
                    String packageName = className.substring(0, dotIndex);
                    if (getDefinedPackage(packageName) == null) {
                        try {
                            Manifest manifest = jarFile.getManifest();
                            if (manifest != null) {
                                definePackage(packageName, manifest, getURLs()[0]);
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

                debugClass(className, byteCode);

                //define the class
                CodeSigner[] codeSigners = jarEntry.getCodeSigners();
                CodeSource codeSource = new CodeSource(getURLs()[0], codeSigners);
                Class<?> found = defineClass(className, byteCode, 0, byteCode.length, codeSource);
                resolveClass(found);    //makes it so loadClass will find this class in the future.
                return found;
            } catch (IOException e) {
                throw new ClassNotFoundException(className, e);
            }
        } else {
            throw new ClassNotFoundException(className);
        }
    }

    private void debugClass(String className, byte[] bytecode) {
        IScalaLoader scalaLoader = IScalaLoader.getInstance();
        DebugSettings debugSettings = scalaLoader.getDebugSettings();
        if (debugSettings.isDebuggingClassLoadOf(className)) {
            Printer debugPrinter = switch (debugSettings.getFormat()) {
                case DebugSettings.ASMIFIED -> new ASMifier();
                default -> new Textifier();
            };
            scalaLoader.getLogger().info("[DEBUG] [DescriptionClassLoader] Dumping bytecode for class " + className);
            ClassReader debugReader = new ClassReader(bytecode);
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, debugPrinter, new PrintWriter(System.out));
            debugReader.accept(traceClassVisitor, 0);
        }
    }

    @Override
    public PluginMeta getConfiguration() {
        return new ScalaPluginMeta(plugin.getScalaDescription());
    }

    @Override
    public Class<?> loadClass(@NotNull String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        return loadClass(name, resolve);
    }

    @Override
    public void init(JavaPlugin javaPlugin) {
        this.plugin = (DescriptionPlugin) javaPlugin;
    }

    @Override
    public @Nullable DescriptionPlugin getPlugin() {
        return plugin;
    }

    @Override
    public @Nullable PluginClassLoaderGroup getGroup() {
        return classLoaderGroup == null ? classLoaderGroup = new SingletonPluginClassLoaderGroup(this) : classLoaderGroup;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    public String getScalaVersion() {
        ScalaPluginClassLoader parent = (ScalaPluginClassLoader) getParent();
        return parent.getScalaVersion();
    }

}
