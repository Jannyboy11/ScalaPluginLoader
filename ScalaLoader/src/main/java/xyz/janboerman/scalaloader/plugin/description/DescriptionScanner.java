package xyz.janboerman.scalaloader.plugin.description;

import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Annotation scanner dat reads the scala version from the plugin's main class.
 */
public class DescriptionScanner extends ClassVisitor {

    private static final int ASM_API_VERSION = Opcodes.ASM6;

    private static final String SCALAPLUGIN_CLASS_NAME = ScalaPlugin.class.getName().replace('.', '/');
    private static final String SCALA_ANNOTATION_DESCRIPTOR = "L" + Scala.class.getName().replace('.', '/') + ";";
    private static final String CUSTOMSCALA_ANNOTATION_DESCRIPTOR = "L" + CustomScala.class.getName().replace('.', '/') + ";";
    private static final String API_ANNOTATION_DESCRIPTOR = "L" + Api.class.getName().replace('.', '/') + ";";

    private String mainClassCandidate;
    private PluginScalaVersion scalaVersion;
    private ApiVersion bukkitApiVersion;
    private boolean extendsScalaPlugin;
    private boolean isAbstract;
    private boolean hasNoArgsConstructor;

    public DescriptionScanner() {
        super(ASM_API_VERSION);
    }

    //visit declaration annotations
    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {

        if (SCALA_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new ScalaAnnotationVisitor();
        } else if (CUSTOMSCALA_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new CustomScalaAnnotationVisitor();
        } else if (API_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new ApiAnnotationVisitor();
        }

        return null;
    }

    //visit declaration
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        mainClassCandidate = name.replace('/', '.');
        isAbstract = (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
        if (SCALAPLUGIN_CLASS_NAME.equals(superName)) {
            extendsScalaPlugin = true;
        }
    }

    //visit constructor
    @Override
    public MethodVisitor visitMethod(int access, java.lang.String name, java.lang.String descriptor, java.lang.String signature, java.lang.String[] exceptions) {
        if ("<init>".equals(name) &&"()V".equals(descriptor) && (access & Opcodes.ACC_PUBLIC) == Opcodes.ACC_PUBLIC) {
            hasNoArgsConstructor = true;
        }

        return null;
    }


    public Optional<String> getMainClass() {
        return Optional.ofNullable(mainClassCandidate)
                .filter(x -> getScalaVersion().isPresent())
                .filter(x -> hasNoArgsContructor())
                .filter(x -> !isAbstract);
    }

    public Optional<PluginScalaVersion> getScalaVersion() {
        return Optional.ofNullable(scalaVersion);
    }

    public Optional<ApiVersion> getBukkitApiVersion() {
        return Optional.ofNullable(bukkitApiVersion);
    }

    public boolean extendsScalaPlugin() {
        return extendsScalaPlugin;
    }

    public boolean hasNoArgsContructor() {
        return hasNoArgsConstructor;
    }

    public String toString() {
        return "{main class = " + getMainClass() +
                ",scala version = " + getScalaVersion() +
                ",bukkit api version = " + getBukkitApiVersion() +
                ",extends scala plugin = " + extendsScalaPlugin() +
                "}";
    }

    // ======================= class annotation visitors =======================

    // custom scala
    private class CustomScalaAnnotationVisitor extends AnnotationVisitor {

        private String version, scalaLibrary, scalaReflect;

        private CustomScalaAnnotationVisitor() {
            super(ASM_API_VERSION);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            //visits the Version value() field
            return new AnnotationVisitor(ASM_API_VERSION) {
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
            scalaVersion = new PluginScalaVersion(version, scalaLibrary, scalaReflect);
        }
    }

    // built-in scala
    private class ScalaAnnotationVisitor extends AnnotationVisitor {

        private ScalaAnnotationVisitor() {
            super(ASM_API_VERSION);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            //should always be ScalaVersion!
            scalaVersion = PluginScalaVersion.fromScalaVersion(ScalaVersion.valueOf(value));
        }
    }

    // bukkit api version
    private class ApiAnnotationVisitor extends AnnotationVisitor {

        private ApiAnnotationVisitor() {
            super(ASM_API_VERSION);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            //should always be ApiVersion!
            bukkitApiVersion = ApiVersion.valueOf(value);
        }
    }

}
