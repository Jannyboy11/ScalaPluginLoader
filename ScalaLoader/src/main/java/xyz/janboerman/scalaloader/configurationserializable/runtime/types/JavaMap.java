package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.bytecode.OperandStack;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;

import java.lang.reflect.Constructor;
import java.util.*;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

@Called
public abstract class JavaMap<K, V> implements Adapter<Map<? extends K, ? extends V>> {

    private static final String FOR_GENERIC = PREFIX_USING_DOTS + "JavaMap$ForGeneric";

    private static final Map<Class<? extends Map>, Class<? extends JavaMap<?, ?>>> ForGenericAdapters = new WeakHashMap<>();

    @Called
    public JavaMap() {}

    public static boolean isRawtypeMap(Object o, ParameterType paramType) {
        return o instanceof Map && ParameterType.class.equals(paramType.getClass());
    }

    public static <K, V> JavaMap<K, V> serialize(Object live, ParameterType type, ScalaPluginClassLoader plugin) {

        ParameterType keyType, valueType;
        if (type instanceof ParameterizedParameterType) {
            ParameterizedParameterType ppt = (ParameterizedParameterType) type;
            keyType = ppt.getTypeParameter(0);
            valueType = ppt.getTypeParameter(1);
        } else {
            keyType = valueType = ParameterType.from(Object.class);
        }

        //no need to specialcase, every map can be serialized through the generic map adapter.

        Class<? extends Map<K, V>> mapClass = (Class<? extends Map<K, V>>) live.getClass();
        Class<? extends JavaMap<K, V>> ForGenericClass = (Class<? extends JavaMap<K, V>>) ForGenericAdapters.get(mapClass);

        if (ForGenericClass == null) {
            final String className = FOR_GENERIC + "$" + mapClass.getName();
            final String alias = mapClass.getName();
            ClassDefineResult classDefineResult = plugin.getOrDefineClass(className,
                    name -> makeForGeneric(alias, name, mapClass, keyType, valueType, plugin),
                    true);
            ForGenericClass = (Class<? extends JavaMap<K, V>>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                ConfigurationSerialization.registerClass(ForGenericClass, alias);
            }
            ForGenericAdapters.put(mapClass, ForGenericClass);
        }

        try {
            Constructor<? extends JavaMap<K, V>> constructor = ForGenericClass.getConstructor(Map.class);
            return constructor.newInstance(live);
        } catch (Exception shouldNotOccur) {
            throw new Error("Malformed generated class", shouldNotOccur);
        }
    }


    private static byte[] makeForGeneric(final String alias, String generatedClassName, final Class<? extends Map> mapClass,
                                         final ParameterType keyType, final ParameterType valueType, final ScalaPluginClassLoader plugin) {
        final String classNameUsingDots = generatedClassName;
        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String generatedClassSignature = "L" + generatedClassName + "<TK;TV;>;";


        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap<TK;TV;>;", "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap", null);

        classWriter.visitSource("JavaMap.java", null);

        {
        annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
        annotationVisitor0.visit("value", alias);
        annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("java/util/Map$Entry", "java/util/Map", "Entry", ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

        {
        fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "value", "Ljava/util/Map;", "Ljava/util/Map<+TK;+TV;>;", null);
        fieldVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/Map;)V", "(Ljava/util/Map<TK;TV;>;)V", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap", "<init>", "()V", false);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "value", "Ljava/util/Map;");
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitInsn(RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label3, 0);
        methodVisitor.visitLocalVariable("value", "Ljava/util/Map;", "Ljava/util/Map<TK;TV;>;", label0, label3, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()Ljava/util/Map;", "()Ljava/util/Map<+TK;+TV;>;", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", "Ljava/util/Map;");
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "serialize", "()Ljava/util/Map;", "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitTypeInsn(NEW, "java/util/LinkedHashMap");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false);
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", "Ljava/util/Map;");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Map", "java/util/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        Label label3 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label3);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 3);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
        genParameterType(methodVisitor, keyType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ALOAD, 3);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
        genParameterType(methodVisitor, valueType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)Ljava/lang/Object;", false);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitJumpInsn(GOTO, label2);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
        methodVisitor.visitLdcInsn("value");
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLocalVariable("entry", "Ljava/util/Map$Entry;", "Ljava/util/Map$Entry<+TK;+TV;>;", label4, label8, 3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label9, 0);
        methodVisitor.visitLocalVariable("result", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label9, 1);
        methodVisitor.visitMaxs(5, 4);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "<K:Ljava/lang/Object;V:Ljava/lang/Object;>(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassSignature, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitLdcInsn("value");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitTypeInsn(NEW, Type.getType(mapClass).getInternalName());
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getType(mapClass).getInternalName(), "<init>", "()V", false);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/util/Map", "java/util/Map", "java/util/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        Label label4 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label4);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");
        methodVisitor.visitVarInsn(ASTORE, 4);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitVarInsn(ALOAD, 4);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
        genParameterType(methodVisitor, keyType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ALOAD, 4);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
        genParameterType(methodVisitor, valueType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)Ljava/lang/Object;", false);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitJumpInsn(GOTO, label3);
        methodVisitor.visitLabel(label4);
        methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, generatedClassName);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(Ljava/util/Map;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLocalVariable("entry", "Ljava/util/Map$Entry;", "Ljava/util/Map$Entry<Ljava/lang/Object;Ljava/lang/Object;>;", label5, label8, 4);
        methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label9, 0);
        methodVisitor.visitLocalVariable("serializedValue", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label9, 1);
        methodVisitor.visitLocalVariable("value", "Ljava/util/Map;", "Ljava/util/Map<TK;TV;>;", label2, label9, 2);
        methodVisitor.visitMaxs(5, 5);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Map;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hashCode", "(Ljava/lang/Object;)I", false);
        methodVisitor.visitInsn(IRETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
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
        methodVisitor.visitTypeInsn(INSTANCEOF, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap");
        Label label2 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitInsn(IRETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap");
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Map;", false);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap", "getValue", "()Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        methodVisitor.visitInsn(IRETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label4, 0);
        methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
        methodVisitor.visitLocalVariable("that", "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaMap;", null, label3, label4, 2);
        methodVisitor.visitMaxs(2, 3);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Map;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        {   //overrides Adapter#getValue
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Map;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}

/*
@SerializableAs("MAP_CLASS_NAME")
final class MapAdapter<K, V> extends JavaMap<K, V> {
    
    private final Map<? extends K, ? extends V> value;

    public MapAdapter(Map<K, V> value) {
        this.value = value;
    }

    @Override
    public Map<? extends K, ? extends V> getValue() {
        return value;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<? extends K, ? extends V> entry : value.entrySet()) {
            result.put(
                    RuntimeConversions.serialize(entry.getKey(), (ParameterType) null, (ScalaPluginClassLoader) null),
                    RuntimeConversions.serialize(entry.getValue(), (ParameterType) null, (ScalaPluginClassLoader) null));
        }
        return Collections.singletonMap("value", result);
    }

    public static <K, V> MapAdapter<K, V> deserialize(Map<String, Object> map) {
        Map<Object, Object> serializedValue = (Map<Object, Object>) map.get("value");
        Map<K, V> value = new HashMap<>();  //TODO in bytecode replace this by implementation class
        for (Map.Entry<Object, Object> entry : serializedValue.entrySet()) {
            value.put((K) RuntimeConversions.deserialize(entry.getKey(), (ParameterType) null, (ScalaPluginClassLoader) null),
                    (V) RuntimeConversions.deserialize(entry.getValue(), (ParameterType) null, (ScalaPluginClassLoader) null));
        }
        return new MapAdapter<>(value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaMap)) return false;
        JavaMap that = (JavaMap) o;
        return Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public String toString() {
        return Objects.toString(getValue());
    }
}
*/