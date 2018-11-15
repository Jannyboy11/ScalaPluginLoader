package xyz.janboerman.scalaloader.plugin.description;

import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Annotation scanner dat reads the scala version from the plugin's main class.
 */
public class DescriptionScanner extends ClassVisitor {

    private static final int ASM_API_VERSION = Opcodes.ASM7;

    private static final String SCALAPLUGIN_CLASS_NAME = ScalaPlugin.class.getName().replace('.', '/');
    private static final String JAVAPLUGIN_CLASS_NAME = JavaPlugin.class.getName().replace('.', '/');
    private static final String JAVA_LANG_OBJECT_CLASS_NAME = Object.class.getName().replace('.', '/');

    private static final String SCALA_ANNOTATION_DESCRIPTOR = "L" + Scala.class.getName().replace('.', '/') + ";";
    private static final String CUSTOMSCALA_ANNOTATION_DESCRIPTOR = "L" + CustomScala.class.getName().replace('.', '/') + ";";
    private static final String VERSION_ANNOTATION_DESCRIPTOR = "L" + Version.class.getName().replace('.', '/') + ";";
    private static final String API_ANNOTATION_DESCRIPTOR = "L" + Api.class.getName().replace('.', '/') + ";";

    private String mainClassCandidate;
    private PluginScalaVersion scalaVersion;
    private ApiVersion bukkitApiVersion;
    private boolean extendsScalaPlugin;
    private boolean extendsJavaPlugin;
    private boolean extendsJavaLangObject;
    private boolean isAbstract;
    private boolean isModule;
    private boolean hasSuitableConstructor;

    public DescriptionScanner() {
        super(ASM_API_VERSION);
    }

    public DescriptionScanner(InputStream classBytes) throws IOException {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, ClassReader.EXPAND_FRAMES);
    }

    public DescriptionScanner(byte[] classBytes) throws IOException {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, ClassReader.EXPAND_FRAMES);
    }

    @Override
    public String toString() {
        return "Description"
                + "{mainClassCandiate=" + mainClassCandidate
                + ",scalaVersion=" + scalaVersion
                + ",bukkitApiVersion=" + bukkitApiVersion
                + ",extendsScalaPluginDirectly=" + extendsScalaPlugin
                + ",extendsJavaPluginDirectly=" + extendsJavaPlugin
                + ",extendsJavaLangObject=" + extendsJavaLangObject
                + ",isAbstract=" + isAbstract
                + ",isModule=" + isModule
                + ",hasSuitableConstructor" + hasSuitableConstructor
                + "}";
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
        isModule = (access & Opcodes.ACC_MODULE) == Opcodes.ACC_MODULE;
        if (SCALAPLUGIN_CLASS_NAME.equals(superName)) {
            extendsScalaPlugin = true;
        } else if (JAVAPLUGIN_CLASS_NAME.equals(superName)) {
            extendsJavaPlugin = true;
        } else if (JAVA_LANG_OBJECT_CLASS_NAME.equals(superName)) {
            extendsJavaLangObject = true;
        }
    }

    //visit constructor
    @Override
    public MethodVisitor visitMethod(int access, java.lang.String name, java.lang.String descriptor, java.lang.String signature, java.lang.String[] exceptions) {
        boolean isConstructor = "<init>".equals(name);
        boolean isNoArgs = "()V".equals(descriptor);
        boolean isPublic = (Opcodes.ACC_PUBLIC & access) == Opcodes.ACC_PUBLIC;
        if (isNoArgs && (isPublic || isConstructor)) { //scala singleton objects have private constructors.
            hasSuitableConstructor = true;
        }

        return null;
    }

    public boolean hasClass() {
        return !isModule;
    }

    public String getClassName() {
        return mainClassCandidate;
    }

    public Optional<String> getMainClass() {
        return Optional.ofNullable(mainClassCandidate)
                .filter(x -> !isModule)
                .filter(x -> getScalaVersion().isPresent())
                .filter(x -> hasSuitableConstructor)
                .filter(x -> !isAbstract)
                .filter(x -> !extendsJavaLangObject);
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

    public boolean extendsJavaPlugin() {
        return extendsJavaPlugin;
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
            return "value".equals(name) && VERSION_ANNOTATION_DESCRIPTOR.equals(descriptor) ?
                new AnnotationVisitor(ASM_API_VERSION) {
                    @Override
                    public void visit(String name, Object value) {
                        switch(name) {
                            case "value":               version         = value.toString();     break;
                            case "scalaLibraryUrl":     scalaLibrary    = value.toString();     break;
                            case "scalaReflectUrl":     scalaReflect    = value.toString();     break;
                        }
                    }
                } : null;
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
