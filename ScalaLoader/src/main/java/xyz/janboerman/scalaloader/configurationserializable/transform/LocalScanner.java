package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import xyz.janboerman.scalaloader.configurationserializable.DeserializationMethod;
import xyz.janboerman.scalaloader.configurationserializable.Scan;
import xyz.janboerman.scalaloader.configurationserializable.Scan.ExcludeProperty;
import xyz.janboerman.scalaloader.configurationserializable.Scan.IncludeProperty;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

/**
 * This class is NOT part of the public API!
 */
class LocalScanner extends ClassVisitor {

    private final LocalScanResult result = new LocalScanResult();

    private String className;
    private String classDescriptor;
    private boolean hasModule$;
    private Scan.Type scanType;
    private boolean isEnum;
    private boolean isRecord;
    private boolean fieldProperties;
    private boolean methodProperties;
    private boolean hasStaticApply;
    private boolean hasStaticUnapply;
    private int applyParamCount;
    private int unapplyParamCount;

    LocalScanner() {
        super(ASM_API);
    }

    LocalScanResult scan(ClassReader classReader) throws ConfigurationSerializableError {
        classReader.accept(this, 0);

        if (result.annotatedByConfigurationSerializable && result.annotatedByDelegateSerialization) {
            throw new ConfigurationSerializableError(className.replace('/', '.') + " is annotated by both @ConfigurationSerializable and @DelegateSerialization");
        }

        if (scanType == null || scanType == Scan.Type.AUTO_DETECT) {
            if (hasModule$) scanType = Scan.Type.SINGLETON_OBJECT;
            else if (isEnum) scanType = Scan.Type.ENUM;
            else if (isRecord) scanType = Scan.Type.RECORD;
            else if (fieldProperties) scanType = Scan.Type.FIELDS;
            else if (methodProperties) scanType = Scan.Type.GETTER_SETTER_METHODS;
            else if (hasStaticUnapply && hasStaticApply && applyParamCount == unapplyParamCount) scanType = Scan.Type.CASE_CLASS;
            else scanType = Scan.Type.FIELDS;
        }

        result.scanType = scanType;
        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.classDescriptor = 'L' + name + ';';
        this.isEnum = (access & ACC_ENUM) == ACC_ENUM;
        this.isRecord = (access & ACC_RECORD) == ACC_RECORD;

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

            return new AnnotationVisitor(ASM_API) {
                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    if (CONSTRUCTUSING_NAME.equals(name) && SCALALOADER_DESERIALIZATIONMETHOD_DESCRIPTOR.equals(descriptor)) {
                        if (DeserializationMethod.MAP_CONSTRUCTOR.name().equals(value)) {
                            throw new ConfigurationSerializableError("Can't use the MAP_CONSTRUCTOR deserialization method when using @DelegateSerialization");
                        }
                    }
                }
            };
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

        return new FieldVisitor(ASM_API) {
            boolean include;
            boolean exclude;

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (SCALALOADER_INCLUDEPROPERTY_DESCRIPTOR.equals(descriptor)) {
                    fieldProperties = true;
                    include = true;
                } else if (SCALALOADER_EXCLUDEPROPERTY_DESCRIPTOR.equals(descriptor)) {
                    fieldProperties = true;
                    exclude = true;
                }

                return null;
            };

            @Override
            public void visitEnd() {
                if (include && exclude) {
                    throw new ConfigurationSerializableError("Field " + name + " in class " + className.replace('/', '.') + " is annotated by both "
                        + IncludeProperty.class.getSimpleName() + " and " + ExcludeProperty.class.getSimpleName() + ". Please remove one of the two.");
                }
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        String returnTypeName = Type.getReturnType(descriptor).getInternalName();
        if ("apply".equals(name) && (access & ACC_STATIC) == ACC_STATIC && (access & ACC_PUBLIC) == ACC_PUBLIC && className.equals(returnTypeName)) {
            hasStaticApply = true;
            applyParamCount = Type.getArgumentTypes(descriptor).length;
        }

        Type[] argumentTypes = Type.getArgumentTypes(descriptor);
        if ("unapply".equals(name) && (access & ACC_STATIC) == ACC_STATIC && (access & ACC_PUBLIC) == ACC_PUBLIC
                && argumentTypes.length == 1 && className.equals(argumentTypes[0].getInternalName())) {
            hasStaticUnapply = true;
            if (signature == null) {
                unapplyParamCount = 1;  //just a boolean or Option<this>
            } else {
                UnapplyParamCounter counter = new UnapplyParamCounter();
                SignatureReader reader = new SignatureReader(signature);
                reader.accept(counter);
                unapplyParamCount = counter.getParamCount();
            }
        }

        return new MethodVisitor(ASM_API) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (SCALALOADER_INCLUDEPROPERTY_DESCRIPTOR.equals(descriptor)) {
                    methodProperties = true;
                } //can't have @ExcludeProperty because that annotation is only has target type Fields!

                return null;
            }
        };
    }

    @Override
    public void visitEnd() {
        if (scanType == Scan.Type.SINGLETON_OBJECT && !hasModule$) {
            result.annotatedByConfigurationSerializable = false; //override! don't generate serialization methods for companion classes of singleton objects!
            result.annotatedByDelegateSerialization = false;
        } else if (scanType != Scan.Type.SINGLETON_OBJECT && hasModule$) {
            result.annotatedByConfigurationSerializable = false;    //the reversed is also true. we don't want to use any of the other serialization methods for object singletons!
            result.annotatedByDelegateSerialization = false;
        }
        //all of this is a workaround for the scala compiler generating annotations on both the classes and companion objects.
    }


}
