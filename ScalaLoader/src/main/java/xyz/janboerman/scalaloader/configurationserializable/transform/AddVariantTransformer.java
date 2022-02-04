package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.compat.Compat;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;
import xyz.janboerman.scalaloader.bytecode.TransformerRegistry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is NOT part of the public API!
 */
public class AddVariantTransformer extends ClassVisitor {

    private String className;           //e.g.  com/example/Foo
    private String classDescriptor;     //e.g.  Lcom/example/Foo;
    private String classSignature;      //e.g.  Lcom/example/Foo<L/com/example/Bar;>;
    private String alias;               //e.g.  "com.example.Foo" or "Foo"
    private boolean isInterface;
    private String superClassName;      //e.g.  com/example/Quz
    private Set<String> notSerializableInterfaces;

    private boolean hasSerialize;
    private int lastLevel = -1;

    AddVariantTransformer(ClassVisitor delegate) {
        super(ASM_API, delegate);
    }

    public static void addTo(TransformerRegistry transformerRegistry, GlobalScanResult scanResult) {
        if (scanResult.sumAlternatives != null) {
            for (Type type : scanResult.sumAlternatives) {
                String className = type.getInternalName().replace('/', '.');
                transformerRegistry.addClassTransformer(className, AddVariantTransformer::new);
            }
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.classDescriptor = 'L' + name + ';';
        this.classSignature = signature;
        this.alias = name.replace('/', '.');
        this.superClassName = superName;
        this.isInterface = (access & ACC_INTERFACE) == ACC_INTERFACE;

        if (interfaces == null) {
            this.notSerializableInterfaces = Compat.emptySet();
        } else {
            this.notSerializableInterfaces = new HashSet<>(2);
            Collections.addAll(this.notSerializableInterfaces, interfaces);
            this.notSerializableInterfaces.remove(BUKKIT_CONFIGURATIONSERIALIZABLE_NAME);
            this.notSerializableInterfaces.remove("java/io/Serializable");
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor superVisitor = super.visitAnnotation(descriptor, visible);

        if (SCALALOADER_CONFIGURATIONSERIALIZABLE_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(ASM_API, superVisitor) {
                @Override
                public void visit(String name, Object value) {
                    if (AS_NAME.equals(name)) {
                        alias = (String) value;
                    }
                    super.visit(name, value);
                }
            };
        }

        else if (BUKKIT_SERIALIZABLEAS_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(ASM_API, superVisitor) {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        alias = (String) value;
                    }
                    super.visit(name, value);
                }
            };
        }

        else if (SCALALOADER_DELEGATESERIALIZATION_DESCRIPTOR.equals(descriptor)) {
            return new AnnotationVisitor(ASM_API, superVisitor) {
                @Override
                public void visit(String name, Object value) {
                    if ("value".equals(name)) {
                        alias = (String) value;
                    }
                    super.visit(name, value);
                }
            };
        }

        return superVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        //detect the serialize() method and rename it, put a dollar in front.
        //note that there may already be a dollar in front, because of previous transformations!
        if ((access & ACC_STATIC) == 0 && SERIALIZE_DESCRIPTOR.equals(descriptor) && name.endsWith(SERIALIZE_NAME)) {
            hasSerialize = true;

            String prefix = name.substring(0, name.length() - SERIALIZE_NAME.length());
            String dollarPrefix = Compat.stringRepeat("$", prefix.length());

            if (prefix.equals(dollarPrefix)) {
                lastLevel = Math.max(prefix.length(), lastLevel);

                //make it private so that is not accidentally called
                access = (access | ACC_PRIVATE) & ~(ACC_PUBLIC | ACC_PROTECTED);
                return new MethodVisitor(ASM_API, super.visitMethod(access, '$' + name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (opcode != INVOKESTATIC
                                && className.equals(owner)
                                && name.endsWith(SERIALIZE_NAME)
                                && SERIALIZE_DESCRIPTOR.equals(descriptor)) {
                            //prevent infinite recursion - call the one-level-older method!
                            super.visitMethodInsn(opcode, owner, '$' + name, descriptor, isInterface);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }
                    }
                };
            }
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        //generate the new serialize method
        if (lastLevel != -1) {

            MethodVisitor methodVisitor = super //use super because we don't want to add another dollar!
                    .visitMethod(ACC_PUBLIC, SERIALIZE_NAME, SERIALIZE_DESCRIPTOR, SERIALIZE_SIGNATURE, null);
            methodVisitor.visitCode();
            final Label startLabel = new Label();
            methodVisitor.visitLabel(startLabel);

            //Map map = new HashMap();
            methodVisitor.visitTypeInsn(NEW, HASHMAP_NAME);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, HASHMAP_NAME, CONSTRUCTOR_NAME, "()V", false);
            methodVisitor.visitVarInsn(ASTORE, 1);
            final Label mapInitializedLabel = new Label();
            methodVisitor.visitLabel(mapInitializedLabel);

            //map.putAll(this.$serialize());
            methodVisitor.visitVarInsn(ALOAD, 1);   //load the map
            methodVisitor.visitVarInsn(ALOAD, 0);   //load 'this'
            if (hasSerialize) {
                //the serialize method was transformed, call the transformed method.
                methodVisitor.visitMethodInsn(INVOKESPECIAL, className, '$' + SERIALIZE_NAME, SERIALIZE_DESCRIPTOR, isInterface); //call this.$serialize()
            } else if (!"java/lang/Object".equals(superClassName)) {
                methodVisitor.visitMethodInsn(INVOKESPECIAL, superClassName, SERIALIZE_NAME, SERIALIZE_DESCRIPTOR, false);  //call super.serialize()
            } else if (!notSerializableInterfaces.isEmpty()) {
                //best effort, if there is only one super interface, then call the serialize method on that interface
                String theInterface = notSerializableInterfaces.iterator().next();
                methodVisitor.visitMethodInsn(INVOKESPECIAL, theInterface, SERIALIZE_NAME, SERIALIZE_DESCRIPTOR, true);   //call superInterface.serialize()
            } else {
                throw new ConfigurationSerializableError("Could not generate a call to this.serialize(). Make sure the class has got a serialize() method! Did you forget the @ConfigurationSerializable annotation?");
            }
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_PUTALL_NAME, MAP_PUTALL_DESCRIPTOR, true);
            //no need to POP because putAll returns void!
            final Label previousSerializeInvokedAndStoredLabel = new Label();
            methodVisitor.visitLabel(previousSerializeInvokedAndStoredLabel);

            //map.put("$variant", "com.example.Foo");
            methodVisitor.visitVarInsn(ALOAD, 1);   //load the map
            methodVisitor.visitLdcInsn(VARIANT_NAME);   //load "$variant"
            methodVisitor.visitLdcInsn(alias);          //load "com.example.Foo" or "Foo"
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_PUT_NAME, MAP_PUT_DESCRIPTOR, true);
            methodVisitor.visitInsn(POP);   //discard there result of Map.put
            final Label variantStoredLabel = new Label();
            methodVisitor.visitLabel(variantStoredLabel);

            //return map;
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitInsn(ARETURN);
            final Label endLabel = new Label();
            methodVisitor.visitLabel(endLabel);

            methodVisitor.visitLocalVariable("this", classDescriptor, classSignature, startLabel, endLabel, 0);
            methodVisitor.visitLocalVariable("map", MAP_DESCRIPTOR, MAP_SIGNATURE, mapInitializedLabel, endLabel, 1);
            methodVisitor.visitMaxs(3, 2);
            methodVisitor.visitEnd();
        }

        else {
            throw new ConfigurationSerializableError("Tried to generate a modified version of the serialize() method, but serialize() did not exist for class "
                    + className.replace('/', '.'));
        }
    }

}
