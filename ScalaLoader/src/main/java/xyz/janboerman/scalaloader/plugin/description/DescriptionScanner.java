package xyz.janboerman.scalaloader.plugin.description;

import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.*;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.description.Version.ScalaLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Annotation scanner dat reads the scala version from the plugin's main class.
 */
public class DescriptionScanner extends ClassVisitor {

    private static final int ASM_API = AsmConstants.ASM_API;

    private static final String SCALAPLUGIN_CLASS_NAME = Type.getInternalName(ScalaPlugin.class);
    private static final String JAVAPLUGIN_CLASS_NAME = Type.getInternalName(JavaPlugin.class);
    private static final String JAVA_LANG_OBJECT_CLASS_NAME = Type.getInternalName(Object.class);

    private static final String SCALA_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Scala.class);
    private static final String CUSTOMSCALA_ANNOTATION_DESCRIPTOR = Type.getDescriptor(CustomScala.class);
    private static final String VERSION_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Version.class);
    private static final String API_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Api.class);
    private static final String SCALALIBRARY_ANNOTATION_DESCRIPTOR = Type.getDescriptor(ScalaLibrary.class);

    private String asmClassName;
    private String mainClassCandidate;  //runtime class name format. e.g.: com.mydomain.project.ProjectPlugin
    private PluginScalaVersion scalaVersion;
    private ApiVersion bukkitApiVersion;
    private boolean extendsScalaPlugin;
    private boolean extendsJavaPlugin;
    private boolean extendsJavaLangObject;
    private boolean isAbstract;
    private boolean isModule;
    private boolean hasPublicNoArgsConstructor;
    private boolean isObject;

    /**
     * Initialises the {@link DescriptionScanner} without reading a class file.
     */
    private DescriptionScanner() {
        super(ASM_API);
    }

    /**
     * Create a {@link DescriptionScanner} and read a class file.
     * @param classBytes the inputstream that provides the class's bytecode
     * @throws IOException if something goes wrong with the InputStream
     */
    public DescriptionScanner(InputStream classBytes) throws IOException {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, 0);
    }

    /**
     * Create a {@link DescriptionScanner} and read a class file.
     * @param classBytes the class's bytecode
     */
    public DescriptionScanner(byte[] classBytes) {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, 0);
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
                + ",hasSuitableConstructor" + hasPublicNoArgsConstructor
                + "}";
    }

    //visit declaration
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        mainClassCandidate = (asmClassName = name).replace('/', '.');
        isAbstract = (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
        isModule = (access & Opcodes.ACC_MODULE) == Opcodes.ACC_MODULE;
        isObject = name.endsWith("$");
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
        if (isConstructor && isNoArgs && isPublic) {
            hasPublicNoArgsConstructor = true;
        }

        return null;
    }

    //visit fields
    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if ("MODULE$".equals(name)) {
            isObject &= ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) && descriptor.equals("L" + asmClassName + ";");
        }

        return null;
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
                .filter(x -> hasPublicNoArgsConstructor || isObject)
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

        private String version;
        private final Map<String, String> urls = new HashMap<>();

        private CustomScalaAnnotationVisitor() {
            super(ASM_API);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            return "value".equals(name) && VERSION_ANNOTATION_DESCRIPTOR.equals(descriptor) ?
                new AnnotationVisitor(ASM_API) {
                    @Override
                    public void visit(String name, Object value) {
                        switch(name) {
                            case "value":               version = value.toString();                                         break;
                            case "scalaLibraryUrl":     urls.put(PluginScalaVersion.SCALA2_LIBRARY_URL, value.toString());  break;
                            case "scalaReflectUrl":     urls.put(PluginScalaVersion.SCALA2_REFLECT_URL, value.toString());  break;
                        }
                    }

                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        return "scalaLibs".equals(name) ? new AnnotationVisitor(ASM_API) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                return SCALALIBRARY_ANNOTATION_DESCRIPTOR.equals(descriptor) ? new AnnotationVisitor(ASM_API) {
                                    String name = null;
                                    String url = null;

                                    @Override
                                    public void visit(String name, Object value) {
                                        if ("name".equals(name)) this.name = (String) value;
                                        else if ("url".equals(name)) this.url = (String) value;
                                    }

                                    @Override
                                    public void visitEnd() {
                                        if (name != null && url != null) {
                                            urls.put(name, url);
                                        }
                                    }
                                } : null;
                            }
                        } : null;
                    }
                } : null;
        }

        @Override
        public void visitEnd() {
            scalaVersion = new PluginScalaVersion(version, urls);
        }
    }

    // built-in scala
    private class ScalaAnnotationVisitor extends AnnotationVisitor {

        private ScalaAnnotationVisitor() {
            super(ASM_API);
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
            super(ASM_API);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            //should always be ApiVersion!
            bukkitApiVersion = ApiVersion.valueOf(value);
        }
    }

}
