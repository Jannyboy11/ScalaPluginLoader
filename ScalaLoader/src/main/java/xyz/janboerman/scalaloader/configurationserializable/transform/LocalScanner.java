package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

class LocalScanner extends ClassVisitor {

    private final LocalScanResult result = new LocalScanResult();

    LocalScanner() {
        super(ASM_API);
    }

    LocalScanResult scan(ClassReader classReader) throws ConfigurationSerializableError {
        classReader.accept(this, ClassReader.EXPAND_FRAMES);

        //TODO some validation!
        /* TODO
         * if the configurationserializable annotation is present
         *      check that a field cannot have both the IncludeProperty and ExcludeProperty annotations
         *      if the ScanType is CASE_CLASS
         *          check that there exists a valid apply for the unapply //TODO didn't I already do this in the SerializableTransformer?
         */

        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
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
        } else if (BUKKIT_SERIALIZABLEAS_DESCRIPTOR.equals(descriptor)) {
            result.annotatedBySerializableAs = true;
        }

        return null;
    }

}
