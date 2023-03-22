package xyz.janboerman.scalaloader.plugin.paper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.logging.Logger;
import java.util.jar.JarFile;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.ClassloaderBytecodeModifier;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;

import org.bukkit.Server;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassFile;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;
import xyz.janboerman.scalaloader.plugin.runtime.PersistentClasses;
import xyz.janboerman.scalaloader.util.ClassLoaderUtils;

public class ScalaPluginClassLoader extends PaperPluginClassLoader implements IScalaPluginClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final ScalaPluginLoader pluginLoader;
    private final File pluginJarFile;
    private final JarFile jarFile;
    private final String mainClassName;
    private final Map<String, Object> pluginYaml;
    private final TransformerRegistry transformerRegistry;
    private final Path dataDirectory;
    private PersistentClasses persistentClasses;

    public ScalaPluginClassLoader(Logger logger,
                                  File pluginJarFile,
                                  ScalaPluginMeta configuration,
                                  ClassLoader parent,
                                  URLClassLoader libraryLoader,

                                  ScalaPluginLoader pluginLoader,
                                  Map<String, Object> pluginYaml,
                                  TransformerRegistry transformerRegistry,
                                  Path dataDirectory) throws IOException {
        super(logger, pluginJarFile.toPath(), Compat.jarFile(pluginJarFile), configuration, parent, libraryLoader);

        this.pluginJarFile = pluginJarFile;
        this.jarFile = Compat.jarFile(pluginJarFile);
        this.mainClassName = configuration.getMainClass();
        this.pluginLoader = pluginLoader;
        this.pluginYaml = pluginYaml;
        this.transformerRegistry = transformerRegistry;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void init(JavaPlugin plugin) {
        assert plugin instanceof ScalaPlugin : "Used ScalaPluginClassLoader to initialise a plugin that is not a ScalaPlugin: " + plugin;
        // overriding this method just us the ability to do it *during* instantiation,
        // meaning that the bodies of ScalaPlugin subclasses' constructors are experiencing a fully initialised ScalaPlugin.
        super.init(plugin);

        this.persistentClasses = new PersistentClasses(getPlugin());
        for (ClassFile classFile : this.persistentClasses.load()) {
            ClassDefineResult cdr = getOrDefineClass(classFile.getClassName(), name -> classFile.getByteCode(false), false);
            if (cdr.isNew()) {
                Class<?> clazz = cdr.getClassDefinition();
                if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
                    Class<? extends ConfigurationSerializable> cls = (Class<? extends ConfigurationSerializable>) clazz;
                    ConfigurationSerialization.registerClass(cls, ConfigurationSerialization.getAlias(cls));
                }
            }
        }
    }

    public File getPluginJarFile() {
        return pluginJarFile;
    }

    @Override
    public ScalaPluginMeta getConfiguration() {
        return (ScalaPluginMeta) super.getConfiguration();
    }

    @Override
    public String getMainClassName() {
        return mainClassName;
    }

    @Override
    public ApiVersion getApiVersion() {
        return ApiVersion.byVersion(getConfiguration().getAPIVersion());
    }

    public String getScalaVersion() {
        return getConfiguration().getScalaVersion();
    }

    public Map<String, Object> getExtraPluginYaml() {
        return Collections.unmodifiableMap(pluginYaml);
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public Server getServer() {
        return ScalaLoader.getInstance().getServer();
    }

    @Override
    public ScalaPlugin getPlugin() {
        return (ScalaPlugin) super.getPlugin();
    }

    @Override
    public ScalaPluginLoader getPluginLoader() {
        return pluginLoader;
    }

    //now that scala library classes are handled by the library classoader, this should simplify a whole lot. I think.

    //probably don't have to override loadClass because the PaperPluginClassLoader implementation is already perfect for us? :D

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        JarEntry entry = this.jar.getJarEntry(path);
        if (entry == null) {
            throw new ClassNotFoundException();
        }

        byte[] classBytes;

        try (InputStream is = this.jarFile.getInputStream(entry)) {
            classBytes = is.readAllBytes();
        } catch (IOException ex) {
            throw new ClassNotFoundException(name, ex);
        }

        classBytes = transformBytecode(name, classBytes);
        //TODO debugClass

        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            String pkgName = name.substring(0, dot);
            // Get defined package does not correctly handle sealed packages.
            if (this.getDefinedPackage(pkgName) == null) {
                try {
                    if (this.jarManifest != null) {
                        this.definePackage(pkgName, this.jarManifest, this.jarUrl);
                    } else {
                        this.definePackage(pkgName, null, null, null, null, null, null, null);
                    }
                } catch (IllegalArgumentException ex) {
                    // parallel-capable class loaders: re-verify in case of a
                    // race condition
                    if (this.getDefinedPackage(pkgName) == null) {
                        // Should never happen
                        throw new IllegalStateException("Cannot find package " + pkgName);
                    }
                }
            }
        }

        CodeSigner[] signers = entry.getCodeSigners();
        CodeSource source = new CodeSource(this.jarUrl, signers);

        return this.defineClass(name, classBytes, 0, classBytes.length, source);
    }


    private byte[] transformBytecode(String className, byte[] byteCode) {
        //Paper-supported bytecode transformer via ServiceLoader api!
        byteCode = ClassloaderBytecodeModifier.bytecodeModifier().modify(configuration, byteCode);

        //ScalaLoader transformations (everything except main class)
        byteCode = ClassLoaderUtils.transform(className, byteCode, this, transformerRegistry, this, ScalaLoader.getInstance().getLogger());

        //ScalaLoader main class transformations
        if (className.equals(mainClassName)) {
            byteCode = transformMainClass(byteCode, transformerRegistry.mainClassTransformers);
        }

        return byteCode;
    }

    private byte[] transformMainClass(byte[] byteCode, List<BiFunction<ClassVisitor, String, ClassVisitor>> transformers) {
        if (transformers.isEmpty())
            //short-circuit
            return byteCode;

        //subclass ClassWriter because we need to override the getClassLoader() method.
        ClassWriter classWriter = new ClassWriter(0) {
            @Override
            protected ClassLoader getClassLoader() {
                return ScalaPluginClassLoader.this;
            }
        };

        //nest classvisitors inside classvisitors until we have no more! :D
        ClassVisitor classVisitor = classWriter;
        for (BiFunction<ClassVisitor, String, ClassVisitor> mainClassTransformer : transformerRegistry.mainClassTransformers) {
            classVisitor = mainClassTransformer.apply(classVisitor, mainClassName);
        }

        //read the bytecode, transform it, return the new bytecode.
        ClassReader classReader = new ClassReader(byteCode);
        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }

    @Override
    public ClassDefineResult getOrDefineClass(String className, ClassGenerator classGenerator, boolean persist) {
        final Class<?> oldClass = findLoadedClass(className);
        if (oldClass != null) {
            return ClassDefineResult.oldClass(oldClass);
        }

        byte[] byteCode = classGenerator.generate(className);
        //debugClass(className, byteCode); //TODO implement dumping.

        boolean isNew;
        Class<?> clazz;

        synchronized (getClassLoadingLock(className)) {
            Class<?> existingClass = findLoadedClass(className);
            if (existingClass == null) {
                clazz = defineClass(className, byteCode, 0, byteCode.length);
                resolveClass(clazz);
                isNew = true;
            } else {
                clazz = existingClass;
                isNew = false;
            }
        }

        if (isNew) {
            if (persist) {
                persistentClasses.save(new ClassFile(className, byteCode));
            }
            return ClassDefineResult.newClass(clazz);
        } else {
            return ClassDefineResult.oldClass(clazz);
        }
    }

}
