package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import org.objectweb.asm.Type;

import java.util.HashSet;

import xyz.janboerman.scalaloader.configurationserializable.InjectionPoint;
import xyz.janboerman.scalaloader.configurationserializable.Scan;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

/**
 * This class is NOT part of the public API!
 */
public class GlobalScanner extends ClassVisitor {

    private final GlobalScanResult result = new GlobalScanResult();

    private boolean hasModule$;
    private String classDescriptor;

    public GlobalScanner() {
        super(ASM_API);
    }

    public GlobalScanResult scan(ClassReader classReader) {
        classReader.accept(this, 0);

        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        result.className = name;
        result.isInterface = (access & ACC_INTERFACE) == ACC_INTERFACE;
        this.classDescriptor = 'L' + name + ';';
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (SCALALOADER_CONFIGURATIONSERIALIZABLE_DESCRIPTOR.equals(descriptor)) {
            result.annotatedByConfigurationSerializable = true;

            return new AnnotationVisitor(ASM_API) {
                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    if (REGISTERAT_NAME.equals(name) && SCALALOADER_INJECTIONPOINT_DESCRIPTOR.equals(descriptor)) {
                        result.registerAt = InjectionPoint.valueOf(value);
                    }
                }

                @Override
                public void visitEnd() {
                    if (result.registerAt == null) {
                        result.registerAt = InjectionPoint.PLUGIN_ONENABLE; //default value
                    }
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    if (SCAN_NAME.equals(name) && SCALALOADER_SCAN_DESCRIPTOR.equals(descriptor)) {
                        return new AnnotationVisitor(ASM_API) {
                            @Override
                            public void visitEnum(String name, String descriptor, String value) {
                                if ("value".equals(name) && SCALALOADER_SCANTYPE_DESCRIPTOR.equals(descriptor)) {
                                    result.scanType = Scan.Type.valueOf(value);
                                }
                            }
                        };
                    }

                    return null;
                }
            };
        }

        else if (SCALALOADER_DELEGATESERIALIZATION_DESCRIPTOR.equals(descriptor)) {
            result.annotatedByDelegateSerialization = true;

            return new AnnotationVisitor(ASM_API) {
                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    if (REGISTERAT_NAME.equals(name) && SCALALOADER_INJECTIONPOINT_DESCRIPTOR.equals(descriptor)) {
                        result.registerAt = InjectionPoint.valueOf(value);
                    }
                }

                @Override
                public void visitEnd() {
                    if (result.registerAt == null) {
                        result.registerAt = InjectionPoint.PLUGIN_ONENABLE; //default value
                    }
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    if ("value".equals(name)) {
                        return new AnnotationVisitor(ASM_API) {
                            @Override
                            public void visit(String name, Object value) {
                                if (result.sumAlternatives == null) result.sumAlternatives = new HashSet<>(2);

                                result.sumAlternatives.add((Type) value);
                            }
                        };
                    }

                    return null;
                }
            };
        }

        return null;
    }

    @Override
    public void visitPermittedSubclass(String permittedSubclass) {
        //permittedSubclass uses the internal-name naming scheme
        if (result.sumAlternatives == null) result.sumAlternatives = new HashSet<>(2);
        result.sumAlternatives.add(Type.getObjectType(permittedSubclass));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if ("MODULE$".equals(name) && (access & ACC_STATIC) == ACC_STATIC && classDescriptor.equals(descriptor)) {
            hasModule$ = true;
        }

        return null;
    }

    @Override
    public void visitEnd() {
        if (result.scanType == Scan.Type.SINGLETON_OBJECT && !hasModule$) {
            result.annotatedByConfigurationSerializable = false; //override! don't generate serialization methods for companion classes of singleton objects!
            result.annotatedByDelegateSerialization = false;

            //this is necessary so that the plugin's onEnable won't try to call $regsiterWithConfigurationSerialization() for example.
            //see PluginTransformer.java
        } else if (result.scanType != Scan.Type.SINGLETON_OBJECT && hasModule$) {
            //vice versa it also true that scan types other than SINGLETON_OBJECT can't be used for object singletons! :)
            result.annotatedByConfigurationSerializable = false;
            result.annotatedByDelegateSerialization = false;
        }
    }
}
