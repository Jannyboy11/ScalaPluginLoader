package xyz.janboerman.scalaloader.paper.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.logging.Logger;
import java.util.jar.JarFile;

import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.entrypoint.classloader.ClassloaderBytecodeModifier;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
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
        //super call already sets ClassLoaderGroup

        this.pluginJarFile = pluginJarFile;
        this.jarFile = Compat.jarFile(pluginJarFile);   // TODO is there any particular reason why we have this field? Why can't we simply use super.jar? Did the jar field in the super class always exist?
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

        hackDataFolder();
        hackPluginDescriptionFile();
        registerCommandsFromPluginYaml();
        //TODO might need this in the future when Paper completely switches to its own PaperPermissionManager implementation.
        //TODO how come the permissions are already registered?
        //registerPermissionsFromPluginYaml();    // is it really necessary to register our permissions with the Paper PluginManager though?

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
        return (ScalaPluginMeta) _getConfiguration();
    }

    private PluginMeta _getConfiguration() {
        //Paper changed PaperSimplePluginClassLoader from PaperPluginMeta to PluginMeta:
        //https://github.com/PaperMC/Paper/pull/10758/files#diff-1b48bde36fde990048ba454fb0bcc4e2b92a441c8387c350f1a8f0f09dbc8f8eR1127

        try {
            Field field = PaperSimplePluginClassLoader.class.getDeclaredField("configuration");
            field.setAccessible(true);
            return (PluginMeta) field.get(this);
        } catch (Throwable e) {
            throw new RuntimeException("Could not get scala plugin configuration?", e);
        }
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

    //now that scala library classes are handled by the library classloader, this should simplify a whole lot. I think.

    //probably don't have to override loadClass because the PaperPluginClassLoader implementation is already perfect for us? :D

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        JarEntry entry = this.jar.getJarEntry(path);
        if (entry == null) {
            throw new ClassNotFoundException(name);
        }

        byte[] classBytes;

        try (InputStream is = this.jarFile.getInputStream(entry)) {
            classBytes = is.readAllBytes();
        } catch (IOException ex) {
            throw new ClassNotFoundException(name, ex);
        }

        classBytes = transformBytecode(name, classBytes);
        debugClass(name, classBytes);

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
                    // parallel-capable class loaders: re-verify in case of a race condition
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

    // No need to worry about classloader groups, since the super implementation (from PaperPluginClassLoader is already correct for us).

    private void debugClass(String className, byte[] bytecode) {
        DebugSettings debugSettings = getPluginLoader().debugSettings();
        if (debugSettings.isDebuggingClassLoadOf(className)) {
            Printer debugPrinter = switch (debugSettings.getFormat()) {
                case DebugSettings.ASMIFIED -> new ASMifier();
                default -> new Textifier();
            };
            ScalaLoader.getInstance().getLogger().info("[DEBUG] [ScalaPluginClassLoader] Dumping bytecode for class " + className);
            ClassReader debugReader = new ClassReader(bytecode);
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, debugPrinter, new PrintWriter(System.out));
            debugReader.accept(traceClassVisitor, 0);
        }
    }

    private byte[] transformBytecode(String className, byte[] byteCode) {
        //Paper-supported bytecode transformer via ServiceLoader api!
        byteCode = ClassloaderBytecodeModifier.bytecodeModifier().modify(getConfiguration(), byteCode);

        //ScalaLoader transformations (everything except main class)
        byteCode = ClassLoaderUtils.transform(className, byteCode, this, transformerRegistry, this, ScalaLoader.getInstance().getLogger());

        //ScalaLoader main class transformations
        if (className.equals(mainClassName)) {
            byteCode = transformMainClass(byteCode, transformerRegistry.mainClassTransformers);
        }

        return byteCode;
    }

    private byte[] transformMainClass(byte[] byteCode, List<BiFunction<ClassVisitor, String, ClassVisitor>> transformers) {
        if (transformers == null || transformers.isEmpty())
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
        byteCode = transformBytecode(className, byteCode); //only really need the migration code, but this will do.
        debugClass(className, byteCode);

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

    @Override
    @Deprecated
    public void addURL(URL url) {
        //do I even want this method? if yes, should the URL be added to the LibraryClassLoader? or to this one?
        //for now stick, with 'this one'.
        super.addURL(url);
    }

    private void hackDataFolder() {
        try {
            Field dataFolderField = JavaPlugin.class.getDeclaredField("dataFolder");
            dataFolderField.setAccessible(true);
            dataFolderField.set(getPlugin(), getDataDirectory().toFile());
            //luckily, this field is not declared as final.
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    private void hackPluginDescriptionFile() {
        try {
            Field descriptionField = JavaPlugin.class.getDeclaredField("description");
            descriptionField.setAccessible(true);
            descriptionField.set(getPlugin(), getConfiguration().description.toPluginDescriptionFile());
        } catch (Throwable ignored) {
        }
    }

    private void registerCommandsFromPluginYaml() {
        var descriptionCommands = getConfiguration().description.getCommands();
        List<Command> pluginCommands = new ArrayList<>(descriptionCommands.size());
        for (var cmd : descriptionCommands) {
            PluginCommand command = newPluginCommand(cmd.getName(), getPlugin());
            cmd.getDescription().ifPresent(command::setDescription);
            cmd.getUsage().ifPresent(usage -> command.setUsage(usage.replace("<command>", cmd.getName())));
            command.setAliases(Compat.listCopy(cmd.getAliases()));
            cmd.getPermission().ifPresent(command::setPermission);
            cmd.getPermissionMessage().ifPresent(command::setPermissionMessage);
            pluginCommands.add(command);
        }

        getServer().getCommandMap().registerAll(getPlugin().getName(), pluginCommands);
    }

    private void registerPermissionsFromPluginYaml() {
        ScalaPluginDescription scalaPluginDescription = getConfiguration().description;
        PermissionDefault defaultPermission = scalaPluginDescription.getPermissionDefault();
        PaperHacks.getPaperPluginManager().addPermissions(scalaPluginDescription.getPermissions().stream()
                .map(descriptionPermission -> toBukkitPermission(descriptionPermission, defaultPermission))
                .toList());
    }

    private static final PluginCommand newPluginCommand(String name, ScalaPlugin plugin) {
        try {
            Constructor<PluginCommand> ctor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            ctor.setAccessible(true);
            return ctor.newInstance(name, plugin);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Permission toBukkitPermission(xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.Permission descriptionPermission, PermissionDefault defaultPermission) {
        return new Permission(descriptionPermission.getName(), descriptionPermission.getDescription().orElse(null), defaultPermission, toBukkitChildren(descriptionPermission.getChildren()));
    }

    private static final Map<String, Boolean> toBukkitChildren(Map<xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.Permission, Boolean> children) {
        if (children == null || children.isEmpty()) return Compat.emptyMap();
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        for (var entry : children.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue());
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
        jarFile.close();
    }
}
