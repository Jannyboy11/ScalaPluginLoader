package xyz.janboerman.scalaloader.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.compat.Migration;
import xyz.janboerman.scalaloader.compat.Platform;
import xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableError;
import xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations;
import xyz.janboerman.scalaloader.event.transform.EventError;
import xyz.janboerman.scalaloader.event.transform.EventTransformations;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassLoaderUtils {

    private ClassLoaderUtils() {
    }

    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] transform(
            final String className, byte[] classBytes, final ClassLoader definer,
            final TransformerRegistry registry, final ScalaPluginClassLoader plugin, final Logger logger) {

        final String path = className.replace('.', '/') + ".class";
        final Platform platform = Platform.detect(plugin.getServer());

        //apply event bytecode transformations
        try {
            classBytes = EventTransformations.transform(classBytes, definer);
        } catch (EventError e) {
            logger.log(Level.SEVERE, "Event class " + className + " is not valid", e);
        }

        //apply configurationserializable bytecode transformations
        try {
            classBytes = ConfigurationSerializableTransformations.transform(classBytes, definer, plugin);
        } catch (ConfigurationSerializableError e) {
            logger.log(Level.SEVERE, "ConfigurationSerializable class " + className + " is not valid", e);
        }

        //apply transformations that were registered by other classes
        {
            ClassWriter classWriter = new ClassWriter(0) {
                @Override
                protected ClassLoader getClassLoader() {
                    return definer;
                }
            };

            ClassVisitor classVisitor = classWriter;

            //can't apply main class transformations, because the plugin's main class is never loaded through this classloader

            //apply target transformations
            List<Function<ClassVisitor, ClassVisitor>> targetedTransformers = registry.byClassTransformers.get(className);
            if (targetedTransformers != null && !targetedTransformers.isEmpty()) {
                for (Function<ClassVisitor, ClassVisitor> transformer : targetedTransformers) {
                    classVisitor = transformer.apply(classVisitor);
                }
            }

            //if there were any transformers, then apply the transformations!
            if (classVisitor != classWriter) {
                ClassReader classreader = new ClassReader(classBytes);
                classreader.accept(classVisitor, 0);
                classBytes = classWriter.toByteArray();
            }
        }

        //apply migration bytecode transformations
        try {
            classBytes = Migration.transform(classBytes, definer);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An unexpected error occurred when updating bytecode to work with new references to classes that have been moved or otherwise refactored.", e);
        }

        //TODO paper transformations

        //apply bukkit bytecode transformations
        try {
            classBytes = platform.transform(path, classBytes, plugin);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Server implementation could not transform class: " + path, e);
        }

        return classBytes;
    }

}
