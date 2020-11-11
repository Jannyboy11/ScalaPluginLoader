package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.List;

import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

public class GlobalScanner extends ClassVisitor {

    private final GlobalScanResult result = new GlobalScanResult();

    public GlobalScanner() {
        super(ASM_API);
    }

    public GlobalScanResult scan(ClassReader classReader) {
        classReader.accept(this, ClassReader.EXPAND_FRAMES);

        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        result.className = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (SCALALOADER_CONFIGURATIONSERIALIZABLE_DESCRIPTOR.equals(descriptor)) {
            result.annotatedByConfigurationSerializable = true;

            return new AnnotationVisitor(ASM_API) {
                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    if (REGISTERAT_NAME.equals(name) && SCALALOADER_INJECTIONPOINT_DESCRIPTOR.equals(descriptor)) {
                        result.registerAt = ConfigurationSerializable.InjectionPoint.valueOf(value);
                    }
                }

                @Override
                public void visitEnd() {
                    if (result.registerAt == null) {
                        result.registerAt = ConfigurationSerializable.InjectionPoint.PLUGIN_ONENABLE; //default value
                    }
                }
            };
        }

//        else if (SCALALOADER_DELEGATESERIALIZATION_DESCRIPTOR.equals(descriptor)) {
//            result.annotatedByDelegateSerialization = true;
//
//            return new AnnotationVisitor(ASM_API) {
//                @Override
//                public AnnotationVisitor visitArray(String name) {
//                    if ("value".equals(name)) {
//                        return new AnnotationVisitor(ASM_API) {
//                            List<Type> allowedSerializableSubtypes = new ArrayList<>(2);
//
//                            @Override
//                            public void visit(String name, Object value) {
//                                //this method will be called for every array element!
//
//                                //I guess name equals "value" here too
//                                Type type = (Type) value;
//                                if (type.getSort() == Type.OBJECT) {
//                                    allowedSerializableSubtypes.add(type);
//                                }
//                            }
//
//                            @Override
//                            public void visitEnd() {
//                                result.allowedSerializableSubtypes = allowedSerializableSubtypes.toArray(new Type[0]); //use Type[]::new in Java 11+
//                            }
//                        };
//                    }
//
//                    return null;
//                }
//            };
//        }

        return null;
    }
}
