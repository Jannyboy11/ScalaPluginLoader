package xyz.janboerman.scalaloader.paper.plugin.description;

import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.Api;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.description.Scala;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.Version;
import xyz.janboerman.scalaloader.plugin.description.Version.ScalaLibrary;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency.Builtin;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency.Custom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MainClassScanner extends ClassVisitor {

    private static final String SCALAPLUGIN_CLASS_NAME = "xyz/janboerman/scalaloader/plugin/ScalaPlugin";               //Type.getInternalName(xyz.janboerman.scalaloader.plugin.ScalaPlugin.class);
    private static final String SCALAPAPERPLUGIN_CLASS_NAME = "xyz/janboerman/scalaloader/paper/plugin/ScalaPlugin";    //Type.getInternalName(xyz.janboerman.scalaloader.paper.plugin.ScalaPlugin.class);
    private static final String JAVAPLUGIN_CLASS_NAME = Type.getInternalName(JavaPlugin.class);
    private static final String JAVA_LANG_OBJECT_CLASS_NAME = Type.getInternalName(Object.class);

    private static final String SCALA_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Scala.class);
    private static final String CUSTOMSCALA_ANNOTATION_DESCRIPTOR = Type.getDescriptor(CustomScala.class);
    private static final String VERSION_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Version.class);
    private static final String SCALALIBRARY_ANNOTATION_DESCRIPTOR = Type.getDescriptor(ScalaLibrary.class);
    private static final String API_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Api.class);

    private ScalaDependency scannedScalaDependency;
    private boolean isAbstract = true;
    private boolean isModule;
    private boolean extendsScalaPlugin;
    private boolean isObject;
    private String asmClassName;
    private String javaClassName;
    private boolean extendsJavaPlugin;
    private boolean extendsJavaLangObject;
    private boolean hasPublicNoArgsConstructor;
    private ApiVersion apiVersion;

    private MainClassScanner() {
        super(AsmConstants.ASM_API);
    }

    public MainClassScanner(InputStream classBytes) throws IOException {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, 0);
    }

    public MainClassScanner(byte[] classBytes) {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, 0);
    }

    public boolean hasScalaAnnotation() {
        return scannedScalaDependency != null;
    }

    public ScalaDependency getScalaDependency() {
        return scannedScalaDependency;
    }

    public boolean extendsScalaPlugin() {
        return extendsScalaPlugin;
    }

    public Optional<String> getMainClass() {
        return Optional.ofNullable(javaClassName)
                .filter(x -> !isModule)
                .filter(x -> hasPublicNoArgsConstructor || isObject)
                .filter(x -> !isAbstract)
                .filter(x -> !extendsJavaLangObject)
                .filter(x -> !extendsJavaPlugin);
    }

    public String getAsmClassName() {
        return asmClassName;
    }

    public String getClassName() {
        return javaClassName;
    }

    public boolean hasApiVersion() {
        return apiVersion != null;
    }

    public ApiVersion getApiVersion() {
        return apiVersion;
    }

    public boolean extendsObject() {
        return extendsJavaLangObject;
    }

    public boolean isSingletonObject() {
        return isObject;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        javaClassName = (asmClassName = name).replace('/', '.');
        isAbstract = (access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT;
        isModule = (access & Opcodes.ACC_MODULE) == Opcodes.ACC_MODULE;
        isObject = name.endsWith("$") && (access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;
        if (SCALAPLUGIN_CLASS_NAME.equals(superName) || SCALAPAPERPLUGIN_CLASS_NAME.equals(superName)) {
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
        if ("MODULE$".equals(name) && descriptor.equals("L" + asmClassName + ";")) {
            isObject &= ((access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);
        }

        return null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (SCALA_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(AsmConstants.ASM_API) {
                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    scannedScalaDependency = new Builtin(ScalaVersion.valueOf(value));
                }
            };
        }

        else if (CUSTOMSCALA_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(AsmConstants.ASM_API) {
                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    if ("value".equals(name) && VERSION_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
                        return new AnnotationVisitor(AsmConstants.ASM_API) {

                            private String version;
                            private Map<String, String> urls = new HashMap<>();

                            @Override
                            public void visit(String name, Object value) {
                                switch (name) {
                                    case "value":
                                        this.version = value.toString();
                                        break;
                                    case "scalaLibraryUrl":
                                        this.urls.put(PluginScalaVersion.SCALA2_LIBRARY_URL, value.toString());
                                        break;
                                    case "scalaReflectUrl":
                                        this.urls.put(PluginScalaVersion.SCALA2_REFLECT_URL, value.toString());
                                        break;
                                }
                            }

                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                if ("scalaLibs".equals(name)) {
                                    return new AnnotationVisitor(AsmConstants.ASM_API) {
                                        private String libraryName;
                                        private String libraryUrl;

                                        @Override
                                        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                                            if (SCALALIBRARY_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
                                                return new AnnotationVisitor(AsmConstants.ASM_API) {
                                                    @Override
                                                    public void visit(String name, Object value) {
                                                        switch (name) {
                                                            case "name":
                                                                libraryName = (String) value;
                                                            case "url":
                                                                libraryUrl = (String) value;
                                                        }
                                                    }

                                                    @Override
                                                    public void visitEnd() {
                                                        urls.put(libraryName, libraryUrl);
                                                    }
                                                };
                                            }

                                            else {
                                                return null;
                                            }
                                        }
                                    };
                                }

                                else {
                                    return null;
                                }
                            }

                            @Override
                            public void visitEnd() {
                                scannedScalaDependency = new Custom(version, Collections.unmodifiableMap(urls));
                            }
                        };
                    }

                    return null;
                }
            };
        }

        else if (API_ANNOTATION_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(AsmConstants.ASM_API) {
                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    apiVersion = ApiVersion.valueOf(value);
                }
            };
        }

        return null;
    }

    public ScalaDependency result() {
        return scannedScalaDependency;
    }

}
