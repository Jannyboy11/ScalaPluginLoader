package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;

import java.lang.reflect.Constructor;


@Called
public abstract class Enum<E extends java.lang.Enum<E>> implements Adapter<E> {

    @Called
    protected Enum() {
    }

    /**
     * Get an instance that represents the enum value that is configuration-serializable.
     * If the enum itself is configuration-serializable, then than it is immediately returned.
     * Otherwise, a wrapper is used.
     *
     * @param enumValue a value in an enumeration
     * @param classLoader the ScalaPlugin's classloader
     * @return the configuration-serializable enum value.
     */
    //if we had sum types we would write: <E extends java.lang.Enum<E>> E | Enum<E>
    public static ConfigurationSerializable forEnum(java.lang.Enum<?> enumValue, ScalaPluginClassLoader classLoader) {
        Class<?> enumClazz = enumValue.getDeclaringClass();

        if (enumValue instanceof ConfigurationSerializable) {
            //assume it was registered already
            assert ConfigurationSerialization.getClassByAlias(ConfigurationSerialization
                    .getAlias((Class<? extends ConfigurationSerializable>) enumClazz)) != null : "Unregistered ConfigurationSerializable enum: " + enumClazz;

            return (ConfigurationSerializable) enumValue;
        }


        String enumClassName = enumClazz.getName();
        String generatedClassName = PREFIX_USING_DOTS + enumClassName;

        ClassDefineResult classDefineResult = classLoader.getOrDefineClass(generatedClassName, name -> make(name, enumClassName), true);
        Class<? extends Enum> wrapperClazz = (Class<? extends Enum>) classDefineResult.getClassDefinition();
        if (classDefineResult.isNew()) {
            ConfigurationSerialization.registerClass(wrapperClazz, enumClassName);      //use the original enum class name as the alias
        }

        try {
            Constructor<? extends ConfigurationSerializable> constructor = wrapperClazz.getConstructor(enumClazz);
            return constructor.newInstance(enumValue);
        } catch (Exception shouldNotOccur) {
            throw new Error("Malformed generated class", shouldNotOccur);
        }
    }

    private static byte[] make(String generatedClassName, String enumClassName) {
        final ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        final String enumClassUsingDots = enumClassName;

        enumClassName = enumClassName.replace('.', '/');
        generatedClassName = generatedClassName.replace('.', '/');
        final String enumClassDescriptor = "L" + enumClassName + ";";
        final String generatedClassDescriptor = "L" + generatedClassName + ";";

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, generatedClassName, "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/Enum<" + enumClassDescriptor + ">;", "xyz/janboerman/scalaloader/configurationserializable/runtime/types/Enum", null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/Enum.java", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
            annotationVisitor0.visit("value", enumClassUsingDots);                      //use the original enum class name as the alias
            annotationVisitor0.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_PUBLIC | ACC_FINAL, "value", enumClassDescriptor, null, null);
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(" + enumClassDescriptor + ")V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/Enum", "<init>", "()V", false);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "value", enumClassDescriptor);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitInsn(RETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
            methodVisitor.visitLocalVariable("value", enumClassDescriptor, null, label0, label3, 1);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "serialize", "()Ljava/util/Map;", "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLdcInsn("value");
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            Label label1 = new Label();
            methodVisitor.visitJumpInsn(IFNONNULL, label1);
            methodVisitor.visitInsn(ACONST_NULL);
            Label label2 = new Label();
            methodVisitor.visitJumpInsn(GOTO, label2);
            methodVisitor.visitLabel(label1);
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/String"});
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, enumClassName, "name", "()Ljava/lang/String;", false);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_FULL, 1, new Object[]{generatedClassName}, 2, new Object[]{"java/lang/String", "java/lang/Object"});
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
            methodVisitor.visitInsn(ARETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
            methodVisitor.visitMaxs(2, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassDescriptor, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("value");
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitVarInsn(ASTORE, 1);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitTypeInsn(NEW, generatedClassName);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 1);
            Label label2 = new Label();
            methodVisitor.visitJumpInsn(IFNONNULL, label2);
            methodVisitor.visitInsn(ACONST_NULL);
            Label label3 = new Label();
            methodVisitor.visitJumpInsn(GOTO, label3);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_FULL, 2, new Object[]{"java/util/Map", "java/lang/Object"}, 2, new Object[]{label1, label1});
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, enumClassName, "valueOf", "(Ljava/lang/String;)" + enumClassDescriptor, false);
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_FULL, 2, new Object[]{"java/util/Map", "java/lang/Object"}, 3, new Object[]{label1, label1, enumClassName});
            methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(" + enumClassDescriptor + ")V", false);
            methodVisitor.visitInsn(ARETURN);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label4, 0);
            methodVisitor.visitLocalVariable("value", "Ljava/lang/Object;", null, label1, label4, 1);
            methodVisitor.visitMaxs(3, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hashCode", "(Ljava/lang/Object;)I", false);
            methodVisitor.visitInsn(IRETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            Label label1 = new Label();
            methodVisitor.visitJumpInsn(IF_ACMPNE, label1);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(IRETURN);
            methodVisitor.visitLabel(label1);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(INSTANCEOF, generatedClassName);
            Label label2 = new Label();
            methodVisitor.visitJumpInsn(IFNE, label2);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitInsn(IRETURN);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, generatedClassName);
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            methodVisitor.visitInsn(IRETURN);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label4, 0);
            methodVisitor.visitLocalVariable("obj", "Ljava/lang/Object;", null, label0, label4, 1);
            methodVisitor.visitLocalVariable("that", generatedClassDescriptor, null, label3, label4, 2);
            methodVisitor.visitMaxs(2, 3);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()" + enumClassDescriptor, null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", enumClassDescriptor);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {   //overrides Adapter#getValue
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + enumClassDescriptor, false);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

}

//Code used to generate the above definition:
/*
enum ExampleEnum {
    FOO, Bar;
}

@SerializableAs("ExampleEnum")
class GeneratedExampleEnum extends Enum<ExampleEnum> {

    public final ExampleEnum value;

    public GeneratedExampleEnum(ExampleEnum value) {
        this.value = value;
    }

    @Override
    public Map<String, Object> serialize() {
        return Collections.singletonMap("value", value == null ? null : value.name());
    }

    public static GeneratedExampleEnum deserialize(Map<String, Object> map) {
        Object value = map.get("value");
        return new GeneratedExampleEnum(value == null ? null : ExampleEnum.valueOf(value.toString()));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof GeneratedExampleEnum)) return false;

        GeneratedExampleEnum that = (GeneratedExampleEnum) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public ExampleEnum getValue() {
        return value;
    }
}
*/