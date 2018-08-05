package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.scala.CustomScala;
import xyz.janboerman.scalaloader.scala.Scala;
import xyz.janboerman.scalaloader.scala.ScalaVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ScalaPluginLoader implements PluginLoader {
    //TODO the pluginloader should only have one instance.
    //TODO the pluginclassloader has per-plugin instances.

    private final Server server;
    private final ScalaLoader scalaLoader;
    private final JavaPluginLoader javaPluginLoader;

    private final Set<PluginScalaVersion> scalaVersions = new HashSet<>();
    private final Pattern[] fileFilters = new Pattern[] { Pattern.compile("\\.jar$"), };

    private final Map<String, ScalaLibraryClassLoader> scalaVersionParentLoaders = new HashMap<>();

    private final Map<String, Class<?>> classes = new HashMap<>();
    private final List<ScalaPluginClassLoader> loaders = new CopyOnWriteArrayList<>();

    public ScalaPluginLoader(Server server) {
        this.server = Objects.requireNonNull(server, "Server cannot be null!");
        this.scalaLoader = JavaPlugin.getPlugin(ScalaLoader.class);
        this.javaPluginLoader = (JavaPluginLoader) scalaLoader.getPluginLoader();
    }

    /**
     * Loads the plugin contained in the specified file
     *
     * @param file File to attempt to load
     * @return Plugin that was contained in the specified file, or null if
     * unsuccessful
     * @throws InvalidPluginException     Thrown when the specified file is not a
     *                                    plugin
     * @throws UnknownDependencyException If a required dependency could not
     *                                    be found
     */
    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        //TODO I want to be able to use both 'object extends ScalaPlugin' and 'class extends ScalaPlugin'
        //TODO even plugins written in java should be able to
        //TODO delegate to JavaPluginLoader if the jar file does not contain a scala plugin.
        return null;
    }

    /**
     * Loads a PluginDescriptionFile from the specified file
     *
     * @param file File to attempt to load from
     * @return A new PluginDescriptionFile loaded from the plugin.yml in the
     * specified file
     * @throws InvalidDescriptionException If the plugin description file
     *                                     could not be created
     */
    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {


        //TODO read plugin information from bytecode. can we just call getDescription? we would need to instantiate the plugin for that.
        //TODO is it okay to do that? probably.

        try {
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entryEnumeration = jarFile.entries();
            while (entryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = entryEnumeration.nextElement();
                if (jarEntry.getName().endsWith(".class")) {
                    InputStream classBytesInputStream = jarFile.getInputStream(jarEntry);

                    ScalaVersionScanner scalaVersionScanner = new ScalaVersionScanner();
                    ClassReader classReader = new ClassReader(classBytesInputStream);
                    classReader.accept(scalaVersionScanner, 0);

                    scalaLoader.getLogger().info("Scanned scala version for plugin class "
                            + scalaVersionScanner.getMainClassCandidates() + " = "
                            + scalaVersionScanner.getScalaVersion());

                } else if (jarEntry.getName().equals("plugin.yml")) {
                    //TODO unsupported! use annotations instead!
                } else {
                    //scalaLoader.getLogger().info("not a class, nor plugin.yml entry: " + jarEntry.getName());
                }

                jarFile.getInputStream(jarEntry);
            }


        } catch (IOException e) {
            throw new InvalidDescriptionException(e, "Could not read jar file " + file.getName());
        }

        //TODO scala plugins are loaded differently from java plugins
        //TODO namely, scala plugins can provide their plugin description in regular code.
        //TODO we load our plugin
        return null;
    }

    /**
     * Returns a list of all filename filters expected by this PluginLoader
     *
     * @return The filters
     */
    @Override
    public Pattern[] getPluginFileFilters() {
        return getPluginFileFilters().clone();
    }

    /**
     * Creates and returns registered listeners for the event classes used in
     * this listener
     *
     * @param listener The object that will handle the eventual call back
     * @param plugin   The plugin to use when creating registered listeners
     * @return The registered listeners.
     */
    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return javaPluginLoader.createRegisteredListeners(listener, plugin);
    }

    /**
     * Enables the specified plugin
     * <p>
     * Attempting to enable a plugin that is already enabled will have no
     * effect
     *
     * @param plugin Plugin to enable
     */
    @Override
    public void enablePlugin(Plugin plugin) {

    }

    /**
     * Disables the specified plugin
     * <p>
     * Attempting to disable a plugin that is not enabled will have no effect
     *
     * @param plugin Plugin to disable
     */
    @Override
    public void disablePlugin(Plugin plugin) {

    }

    /**
     * Tries to get the plugin instance from the scala plugin class.
     * @param clazz the plugin class
     * @param <P> the plugin type
     * @return the plugin's instance.
     * @throws ScalaPluginLoaderException
     */
    private <P extends ScalaPlugin> P getPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
        boolean weFoundAScalaSingletonObject = false;

        if (clazz.getName().endsWith("$")) {
            weFoundAScalaSingletonObject = true;

            //we found a scala singleton object.
            //there must be a class with the same name, that holds our instance.

            try {
                Field field = clazz.getField("MODULE$");
                Object pluginInstance = field.get(null);

                return clazz.cast(pluginInstance);
            } catch (NoSuchFieldException e) {
                weFoundAScalaSingletonObject = false; //back paddle!
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Couldn't access MODULE$ field in class " + clazz.getName(), e);
            }
        }

        if (!weFoundAScalaSingletonObject) /*IntelliJ your code inspection is lying.*/ {
            //we found are a regular class.
            //it should have a NoArgsConstructor.

            try {
                Constructor ctr = clazz.getConstructor();
                Object pluginInstance = ctr.newInstance();

                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Could not access the NoArgsConstructor of " + clazz.getName() + ", please make it public", e);
            } catch (InvocationTargetException e) {
                throw new ScalaPluginLoaderException("Error instantiating class " + clazz.getName() + ", its constructor threw something at us", e);
            } catch (NoSuchMethodException e) {
                throw new ScalaPluginLoaderException("Could not find NoArgsContstructor in class " + clazz.getName(), e);
            } catch (InstantiationException e) {
                throw new ScalaPluginLoaderException("Could not instantiate class " + clazz.getName(), e);
            }

        }

        else return null;
    }

    public PluginScalaVersion readScalaPlugin(byte[] classBytes) {
        //TODO return compound object - the PluginScalaVersion * Plugin Main Class *
        return null;
    }

}

/**
 * Annotation scanner dat reads the scala version from the plugin's main class.
 */
class ScalaVersionScanner extends ClassVisitor {

    private static final String SCALAPLUGIN_CLASS_NAME = ScalaPlugin.class.getName().replace('.', '/');
    private static final String SCALA_ANNOTATION_DESCRIPTOR = "L" + Scala.class.getName().replace('.', '/') + ";";
    private static final String CUSTOMSCALA_ANNOTATION_DESCRIPTOR = "L" + CustomScala.class.getName().replace('.', '/') + ";";

    private final LinkedHashSet<String> mainClassCandidates = new LinkedHashSet<>();

    private PluginScalaVersion scalaVersion;

    ScalaVersionScanner() {
        super(Opcodes.ASM6);
    }

    //TODO emit a warning when the class does extend ScalaPlugin, but does not have de @Scala or @CustomScala annotation.

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {

        if (SCALA_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new ScalaAnnotationVisitor(this);
        } else if (CUSTOMSCALA_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new CustomScalaAnnotationVisitor(this);
        }

        return null;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (SCALAPLUGIN_CLASS_NAME.equals(superName)) {
            mainClassCandidates.add(name.replace('/', '.'));
        }
    }

    public Set<String> getMainClassCandidates() {
        return Collections.unmodifiableSet(mainClassCandidates);
    }

    void setScalaVersion(PluginScalaVersion scalaVersion) {
        this.scalaVersion = scalaVersion;
    }

    public PluginScalaVersion getScalaVersion() {
        return scalaVersion;
    }
}

class ScalaAnnotationVisitor extends AnnotationVisitor {

    private final ScalaVersionScanner scalaVersionScanner;

    private ScalaVersion scalaVersion;

    ScalaAnnotationVisitor(ScalaVersionScanner scanner) {
        super(Opcodes.ASM6);
        this.scalaVersionScanner = scanner;
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        //should always be ScalaVersion!
        this.scalaVersion = ScalaVersion.valueOf(value);
    }

    @Override
    public void visitEnd() {
        scalaVersionScanner.setScalaVersion(PluginScalaVersion.fromScalaVersion(scalaVersion));
    }
}

class CustomScalaAnnotationVisitor extends AnnotationVisitor {
    private final ScalaVersionScanner scalaVersionScanner;

    private String version, scalaLibrary, scalaReflect;

    CustomScalaAnnotationVisitor(ScalaVersionScanner scanner) {
        super(Opcodes.ASM6);
        this.scalaVersionScanner = scanner;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        //visits the Version value() field
        return new AnnotationVisitor(Opcodes.ASM6) {
            @Override
            public void visit(String name, Object value) {
                switch(name) {
                    case "value":               version         = value.toString();     break;
                    case "scalaLibraryUrl":     scalaLibrary    = value.toString();     break;
                    case "scalaReflectUrl":     scalaReflect    = value.toString();     break;
                }
            }
        };
    }

    @Override
    public void visitEnd() {
        scalaVersionScanner.setScalaVersion(new PluginScalaVersion(version, scalaLibrary, scalaReflect));
    }

}

