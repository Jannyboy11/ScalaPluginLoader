package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import org.objectweb.asm.FieldVisitor;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import xyz.janboerman.scalaloader.configurationserializable.Scan;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

class LocalScanner extends ClassVisitor {

    private final LocalScanResult result = new LocalScanResult();

    private boolean hasModule$;
    private Scan.Type scanType;
    private String classDescriptor;

    LocalScanner() {
        super(ASM_API);
    }

    LocalScanResult scan(ClassReader classReader) throws ConfigurationSerializableError {
        classReader.accept(this, 0);

        //TODO some validation!
        /* TODO
         * if the ConfigurationSerializable annotation is present
         *      check that the DelegateSerialization method is NOT present
         *
         */

        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classDescriptor = 'L' + name + ';';

        if (interfaces != null) {
            for (String itf : interfaces) {
                if (BUKKIT_CONFIGURATIONSERIALIZABLE_NAME.equals(itf)) {
                    result.implementsConfigurationSerializable = true;
                    break;
                }
            }
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (SCALALOADER_CONFIGURATIONSERIALIZABLE_DESCRIPTOR.equals(descriptor)) {
            result.annotatedByConfigurationSerializable = true;

            return new AnnotationVisitor(ASM_API) {
                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    if (SCAN_NAME.equals(name) && SCALALOADER_SCAN_DESCRIPTOR.equals(descriptor)) {
                        return new AnnotationVisitor(ASM_API) {
                            @Override
                            public void visitEnum(String name, String descriptor, String value) {
                                if ("value".equals(name) && SCALALAODER_SCANTYPE_DESCRIPTOR.equals(descriptor)) {
                                    scanType = Scan.Type.valueOf(value);
                                }
                            }
                        };
                    }

                    return null;
                }
            };

        } else if (SCALALOADER_DELEGATESERIALIZATION_DESCRIPTOR.equals(descriptor)) {
            result.annotatedByDelegateSerialization = true;
        } else if (BUKKIT_SERIALIZABLEAS_DESCRIPTOR.equals(descriptor)) {
            result.annotatedBySerializableAs = true;
        }

        return null;
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
        if (scanType == Scan.Type.SINGLETON_OBJECT && !hasModule$) {
            result.annotatedByConfigurationSerializable = false; //override! don't generate serialization methods for companion classes of singleton objects!

            //this is necessary so that the plugin's onEnable won't try to call $regsiterWithConfigurationSerialization() for example.
            //see PluginTransformer.java
        }
    }


}
