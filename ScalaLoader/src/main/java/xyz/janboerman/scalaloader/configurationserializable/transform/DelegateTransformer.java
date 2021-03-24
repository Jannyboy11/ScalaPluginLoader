package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.configurationserializable.DelegateSerialization;
import xyz.janboerman.scalaloader.configurationserializable.DeserializationMethod;
import xyz.janboerman.scalaloader.configurationserializable.InjectionPoint;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

public class DelegateTransformer extends ClassVisitor {

    private final LocalScanResult scanResult;

    private String className;       //uses slashes, not dots:                   foo/bar/SomeClass
    private String classDescriptor; //uses the nominal descriptor notation:     Lfoo/bar/SomeClass;
    private String superType;       //uses slashes:                             java/lang/Object
    private String classSignature;  //includes generics                         Lfoo/bar/Seq<Lfoo/bar/Quz;>;
    private boolean classIsInterface;

    private boolean alreadyHasDeserializeMethod;
    private boolean alreadyHasValueOfMethod;
    private boolean alreadyHasClassInitializer;

    private String serializableAs;
    private DeserializationMethod constructUsing = DeserializationMethod.DESERIALIZE;
    private InjectionPoint registerAt = InjectionPoint.PLUGIN_ONENABLE;

    DelegateTransformer(ClassVisitor delegate, LocalScanResult scanResult) {
        super (ASM_API, delegate);
        this.scanResult = scanResult;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (scanResult.annotatedByDelegateSerialization) {
            this.className = name;
            this.classDescriptor = 'L' + name + ';';
            this.classSignature = signature;
            this.superType = superName;
            this.classIsInterface = (access & ACC_INTERFACE) == ACC_INTERFACE;

            //make the class public
            access = (access | ACC_PUBLIC) & ~(ACC_PRIVATE | ACC_PROTECTED);

            if (!scanResult.implementsConfigurationSerializable) {
                String[] newInterfaces = new String[interfaces.length + 1];
                System.arraycopy(interfaces, 0, newInterfaces, 0, interfaces.length);
                newInterfaces[interfaces.length] = BUKKIT_CONFIGURATIONSERIALIZABLE_NAME;
                interfaces = newInterfaces;
            }
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor superVisitor = super.visitAnnotation(descriptor, visible);

        if (SCALALOADER_DELEGATESERIALIZATION_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(ASM_API, superVisitor) {
                boolean setAlias = false;

                @Override
                public void visit(String name, Object value) {
                    if (AS_NAME.equals(name)) {
                        serializableAs = (String) value;
                        setAlias = true;
                    }
                    super.visit(name, value);
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    if (CONSTRUCTUSING_NAME.equals(name) && SCALALOADER_DESERIALIZATIONMETHOD_DESCRIPTOR.equals(descriptor)) {
                        constructUsing = DeserializationMethod.valueOf(value);
                        assert constructUsing != DeserializationMethod.MAP_CONSTRUCTOR : "attempted to set DeserializationMethod MAP_CONSTRUCTOR for sum types!";
                    } else if (REGISTERAT_NAME.equals(name) && SCALALOADER_INJECTIONPOINT_DESCRIPTOR.equals(descriptor)) {
                        registerAt = InjectionPoint.valueOf(value);
                    }
                    super.visit(name, value);
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    if (setAlias && !scanResult.annotatedBySerializableAs) { //generate @SerialiableAs(<the alias>) if it wasn't present
                        AnnotationVisitor av = DelegateTransformer.this.visitAnnotation(BUKKIT_SERIALIZABLEAS_DESCRIPTOR, true);
                        av.visit("value", serializableAs);
                        av.visitEnd();
                    }
                }
            };
        }

        else if (BUKKIT_SERIALIZABLEAS_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(ASM_API, superVisitor) {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        serializableAs = (String) value;
                    }
                    super.visit(name, value);
                }
            };
        }

        return superVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String methodSignature, String[] exceptions) {
        if (scanResult.annotatedByDelegateSerialization) {
            boolean isStatic = (access & ACC_STATIC) == ACC_STATIC;

            if (isStatic && DESERIALIZE_NAME.equals(methodName) && deserializationDescriptor(classDescriptor).equals(methodDescriptor)) {
                alreadyHasDeserializeMethod = true;
                //make deserialize(Map<String, Object>) public in case it wasn't.
                access = (access | ACC_PUBLIC) & ~(ACC_PRIVATE | ACC_PROTECTED);
            }

            else if (isStatic && VALUEOF_NAME.equals(methodName) && deserializationDescriptor(classDescriptor).equals(methodDescriptor)) {
                alreadyHasValueOfMethod = true;
                //make valueOf(Map<String, Object>) public in case it wasn't.
                access = (access | ACC_PUBLIC) & ~(ACC_PRIVATE | ACC_PROTECTED);
            }

            else if (isStatic && CLASS_INIT_NAME.equals(methodName) && "()V".equals(methodDescriptor)) {
                alreadyHasClassInitializer = true;

                if (registerAt == InjectionPoint.CLASS_INITIALIZER) {

                    return new MethodVisitor(ASM_API, super.visitMethod(access, methodName, methodDescriptor, methodSignature, exceptions)) {
                        @Override
                        public void visitCode() {
                            //call registerWithConfigurationSerialization$()
                            visitMethodInsn(INVOKESTATIC, className, REGISTER_NAME, REGISTER_DESCRIPTOR, classIsInterface);
                            super.visitCode();
                        }
                    };
                }
            }
        }

        return super.visitMethod(access, methodName, methodDescriptor, methodSignature, exceptions);
    }


    @Override
    public void visitEnd() {
        if (scanResult.annotatedByDelegateSerialization) {
            //generate deserialize method
            boolean generateDeserializationMethod;
            String deserializeMethodName;

            switch (constructUsing) {
                case MAP_CONSTRUCTOR:
                    throw new ConfigurationSerializableError("Can't use constructUsing " + DeserializationMethod.MAP_CONSTRUCTOR.name() + " when using the @" + DelegateSerialization.class.getSimpleName() + " annotation");

                case DESERIALIZE:
                    deserializeMethodName = DESERIALIZE_NAME;
                    generateDeserializationMethod = !alreadyHasDeserializeMethod;
                    break;
                case VALUE_OF:
                    deserializeMethodName = VALUEOF_NAME;
                    generateDeserializationMethod = !alreadyHasValueOfMethod;
                    break;

                default:
                    throw new RuntimeException("Unreachable, got constructUsing "
                            + DeserializationMethod.class.getSimpleName() + "."
                            + constructUsing.name());
            }

            if (generateDeserializationMethod) {
                //strategy:
                //  1. load the variant (subclass) from the map (as a string)
                //  2. get the corresponding class from bukkit's ConfigurationSerialization
                //  3. call ConfigurationSerialization.deserializeObject using the acquired class
                //  4. cast to make sure we are in fact a super type

                MethodVisitor methodVisitor = visitMethod(ACC_PUBLIC | ACC_STATIC, deserializeMethodName, deserializationDescriptor(classDescriptor), deserializationSignature(classDescriptor), null);
                methodVisitor.visitParameter("map", ACC_FINAL);
                methodVisitor.visitCode();
                final Label startLabel = new Label();
                methodVisitor.visitLabel(startLabel);

                methodVisitor.visitVarInsn(ALOAD, 0);   //load map onto the stack
                //stack: [map]
                methodVisitor.visitInsn(DUP);                //duplicate it
                //stack: [map, map]
                methodVisitor.visitLdcInsn(VARIANT_NAME);    //load "$variant"
                //stack: [map, map, "$variant"]
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_GET_NAME, MAP_GET_DESCRIPTOR, true);   //get the variant
                //stack: [map, object]
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String"); //make sure it is a string
                //stack: [map, alias]
                methodVisitor.visitMethodInsn(INVOKESTATIC, "org/bukkit/configuration/serialization/ConfigurationSerialization", "getClassByAlias", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                //stack: [map, class]
                methodVisitor.visitMethodInsn(INVOKESTATIC, "org/bukkit/configuration/serialization/ConfigurationSerialization", "deserializeObject", "(Ljava/util/Map;Ljava/lang/Class;)Lorg/bukkit/configuration/serialization/ConfigurationSerializable;", false);
                //stack: [deserializedInstance]
                methodVisitor.visitTypeInsn(CHECKCAST, className);
                //stack: [instanceOfOurClass]
                methodVisitor.visitInsn(ARETURN);

                final Label endLabel = new Label();
                methodVisitor.visitLabel(endLabel);
                methodVisitor.visitLocalVariable("map", MAP_DESCRIPTOR, MAP_SIGNATURE, startLabel, endLabel, 0);
                methodVisitor.visitMaxs(3, 1);
                methodVisitor.visitEnd();
            }

            //generate registration method

            boolean noAlias = serializableAs == null || serializableAs.isEmpty();
            MethodVisitor methodVisitor = this.visitMethod(ACC_PUBLIC | ACC_STATIC, REGISTER_NAME, REGISTER_DESCRIPTOR, null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLdcInsn(Type.getType(classDescriptor));
            if (noAlias) {
                methodVisitor.visitMethodInsn(INVOKESTATIC, "org/bukkit/configuration/serialization/ConfigurationSerialization", "registerClass", "(Ljava/lang/Class;)V", false);
            } else {
                methodVisitor.visitLdcInsn(serializableAs);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "org/bukkit/configuration/serialization/ConfigurationSerialization", "registerClass", "(Ljava/lang/Class;Ljava/lang/String;)V", false);
            }
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitInsn(RETURN);
            methodVisitor.visitMaxs(noAlias ? 1 : 2, 0);
            methodVisitor.visitEnd();

            //call registration method if the injectionpoint is CLASS_INITIALIZER
            if (!alreadyHasClassInitializer && registerAt == InjectionPoint.CLASS_INITIALIZER) {
                MethodVisitor mvStaticInit = this.visitMethod(ACC_STATIC, CLASS_INIT_NAME, "()V", null, null);
                mvStaticInit.visitCode();
                mvStaticInit.visitMethodInsn(INVOKESTATIC, className, REGISTER_NAME, REGISTER_DESCRIPTOR, classIsInterface);
                mvStaticInit.visitInsn(RETURN);
                mvStaticInit.visitMaxs(0, 0);
                mvStaticInit.visitEnd();
            }
        }

        super.visitEnd();
    }
}
