package xyz.janboerman.scalaloader.plugin.paper.description;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.CustomScala;
import xyz.janboerman.scalaloader.plugin.description.Scala;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.Version;
import xyz.janboerman.scalaloader.plugin.description.Version.ScalaLibrary;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency.Builtin;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency.Custom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScalaLibraryScanner extends ClassVisitor {

    private static final String SCALA_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Scala.class);
    private static final String CUSTOMSCALA_ANNOTATION_DESCRIPTOR = Type.getDescriptor(CustomScala.class);
    private static final String VERSION_ANNOTATION_DESCRIPTOR = Type.getDescriptor(Version.class);
    private static final String SCALALIBRARY_ANNOTATION_DESCRIPTOR = Type.getDescriptor(ScalaLibrary.class);

    private ScalaDependency scannedScalaDependency;

    private ScalaLibraryScanner() {
        super(AsmConstants.ASM_API);
    }

    public ScalaLibraryScanner(InputStream classBytes) throws IOException {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, 0);
    }

    public ScalaLibraryScanner(byte[] classBytes) {
        this();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(this, 0);
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

        return null;
    }

    public ScalaDependency result() {
        return scannedScalaDependency;
    }

}
