package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.bytecode.LocalCounter;
import xyz.janboerman.scalaloader.bytecode.LocalVariable;
import xyz.janboerman.scalaloader.bytecode.LocalVariableTable;
import xyz.janboerman.scalaloader.bytecode.OperandStack;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;

import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.lang.reflect.*;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

@Called
public abstract class ScalaMap implements Adapter/*<scala.collection.Map>*/ {

    static final String SCALA_MAP = "scala.collection.Map";
    static final String SCALA_IMMUTABLE_MAP = "scala.collection.immutable.Map";
    static final String SCALA_MUTABLE_MAP = "scala.collection.mutable.Map";

    @Called
    public ScalaMap() {}

    private static boolean isMapN(String mapClassName, int N) {
        return ("scala.collection.immutable.Map$Map" + N).equals(mapClassName);
    }

    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> Class<?> getScalaMapClass(ScalaPluginClassLoader plugin) throws ClassNotFoundException {
        return Class.forName(SCALA_MAP, false, plugin);
    }


    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> boolean isImmutableMap(Object live, ScalaPluginClassLoader plugin) {
        try {
            return Class.forName(SCALA_IMMUTABLE_MAP, false, plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> boolean isMutableMap(Object live, ScalaPluginClassLoader plugin) {
        try {
            return Class.forName(SCALA_MUTABLE_MAP, false, plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> boolean isMap(Object live, ScalaPluginClassLoader plugin) {
        try {
            return getScalaMapClass(plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> ScalaMap serialize(Object live, ParameterType type, ScalaPluginClassLoader plugin) {
        assert isMap(live, plugin) : "Not a " + SCALA_MAP;

        final ParameterType keyType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
        final ParameterType valueType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(1) : ParameterType.from(Object.class);

        final Class<?> ourMapClass = live.getClass();
        final String alias = ourMapClass.getName();
        final String generatedClassName = PREFIX_USING_DOTS + "ScalaMap$" + alias;

        final OptionalInt isMapN = IntStream.rangeClosed(1, 4).filter(N -> isMapN(alias, N)).findAny();
        if (isMapN.isPresent()) {
            final int N = isMapN.getAsInt();

            ClassDefineResult classDefineResult = plugin.getOrDefineClass(generatedClassName,
                    name -> makeMapN(N, generatedClassName, ourMapClass, alias, keyType, valueType, plugin),
                    true);
            Class<? extends ScalaMap> wrapperClass = (Class<? extends ScalaMap>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                ConfigurationSerialization.registerClass(wrapperClass, alias);
            }

            try {
                Constructor<? extends ScalaMap> constructor = wrapperClass.getConstructor(ourMapClass);
                return constructor.newInstance(live);
            } catch (Exception shouldNotOccur) {
                throw new RuntimeException("Could not serialize scala map: " + live + ", of type: " + type, shouldNotOccur);
            }
        }

        //TODO if the keyType is not java.lang.Object, we might be able to handle SortedMaps (both immutable and mutable)
        //TODO because only then we could generate a meaningful Ordering instance.

        else if (isImmutableMap(live, plugin)) {
            ClassDefineResult classDefineResult = plugin.getOrDefineClass(generatedClassName,
                    name -> makeImmutableMap(generatedClassName, ourMapClass, alias, keyType, valueType, plugin),
                    true);
            Class<? extends ScalaMap> wrapperClass = (Class<? extends ScalaMap>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                ConfigurationSerialization.registerClass(wrapperClass, alias);
            }

            try {
                Constructor<? extends ScalaMap> constructor = wrapperClass.getConstructor(getScalaMapClass(plugin));
                return constructor.newInstance(live);
            } catch (Exception shouldNotOccur) {
                throw new RuntimeException("Could not serialize scala map: " + live + ", of type: " + type, shouldNotOccur);
            }
        }

        else if (isMutableMap(live, plugin)) {
            ClassDefineResult classDefineResult = plugin.getOrDefineClass(generatedClassName,
                    name -> makeMutableMap(generatedClassName, ourMapClass, alias, keyType, valueType, plugin),
                    true);
            Class<? extends ScalaMap> wrapperClass = (Class<? extends ScalaMap>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                ConfigurationSerialization.registerClass(wrapperClass, alias);
            }

            try {
                Constructor<? extends ScalaMap> constructor = wrapperClass.getConstructor(getScalaMapClass(plugin));
                return constructor.newInstance(live);
            } catch (Exception shouldNotOccur) {
                throw new RuntimeException("Could not serialize scala map: " + live + ", of type: " + type, shouldNotOccur);
            }
        }

        throw new RuntimeException("Could not serialize scala map: " + live + ", of type: " + type);
    }


    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] makeImmutableMap(
            String generatedClassName, final Class<?> theMapType, final String alias,
            final ParameterType keyType, final ParameterType valueType, final ScalaPluginClassLoader plugin) {
        final String classNameUsingDots = generatedClassName;

        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String ourMapClassName = theMapType.getName().replace('.', '/');
        final String ourMapCompanionObjectName = ourMapClassName + "$";
        final String ourMapCompanionObjectDescriptor = "L" + ourMapCompanionObjectName + ";";


        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, null, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap.java", null);

        {
        annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
        annotationVisitor0.visit("value", alias);
        annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("java/util/Map$Entry", "java/util/Map", "Entry", ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

        {
        fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "map", "Lscala/collection/Map;", null, null);
        fieldVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Lscala/collection/Map;)V", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", "<init>", "()V", false);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "map", "Lscala/collection/Map;");
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitInsn(RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
        methodVisitor.visitLocalVariable("map", "Lscala/collection/Map;", null, label0, label3, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()Lscala/collection/Map;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "map", "Lscala/collection/Map;");
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
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
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "map", "Lscala/collection/Map;");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Map", "iterator", "()Lscala/collection/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Map", "scala/collection/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "hasNext", "()Z", true);
        Label label3 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label3);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "scala/Tuple2");
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Tuple2", "_1", "()Ljava/lang/Object;", false);
        genParameterType(methodVisitor, keyType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ALOAD, 3);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Tuple2", "_2", "()Ljava/lang/Object;", false);
        genParameterType(methodVisitor, valueType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitJumpInsn(GOTO, label2);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitLdcInsn("map");
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLocalVariable("entry", "Lscala/Tuple2;", null, label5, label8, 3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label9, 0);
        methodVisitor.visitLocalVariable("serialized", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label9, 1);
        methodVisitor.visitLocalVariable("iterator", "Lscala/collection/Iterator;", null, label2, label9, 2);
        methodVisitor.visitMaxs(5, 4);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassDescriptor, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitLdcInsn("map");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitFieldInsn(GETSTATIC, ourMapCompanionObjectName, "MODULE$", ourMapCompanionObjectDescriptor);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, ourMapCompanionObjectName, "newBuilder", "()Lscala/collection/mutable/ReusableBuilder;", false);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/util/Map", "scala/collection/mutable/Builder", "java/util/Iterator"}, 0, null);
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
        methodVisitor.visitTypeInsn(NEW, "scala/Tuple2");
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 4);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
        genParameterType(methodVisitor, keyType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ALOAD, 4);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
        genParameterType(methodVisitor, valueType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "scala/Tuple2", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/mutable/Builder", "addOne", "(Ljava/lang/Object;)Lscala/collection/mutable/Growable;", true);
        methodVisitor.visitInsn(POP);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitJumpInsn(GOTO, label3);
        methodVisitor.visitLabel(label4);
        methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, generatedClassName);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/mutable/Builder", "result", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "scala/collection/Map");
        methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(Lscala/collection/Map;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label10 = new Label();
        methodVisitor.visitLabel(label10);
        methodVisitor.visitLocalVariable("entry", "Ljava/util/Map$Entry;", null, label5, label9, 4);
        methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label10, 0);
        methodVisitor.visitLocalVariable("serialized", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label10, 1);
        methodVisitor.visitLocalVariable("builder", "Lscala/collection/mutable/Builder;", null, label2, label10, 2);
        methodVisitor.visitMaxs(7, 5);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
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
        methodVisitor.visitTypeInsn(INSTANCEOF, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap");
        Label label2 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitInsn(IRETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap");
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", "getValue", "()Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        methodVisitor.visitInsn(IRETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label4, 0);
        methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
        methodVisitor.visitLocalVariable("that", "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap;", null, label3, label4, 2);
        methodVisitor.visitMaxs(2, 3);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
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



    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] makeMutableMap(String generatedClassName, final Class<?> theMapType, final String alias,
                                         final ParameterType keyType, final ParameterType valueType, final ScalaPluginClassLoader plugin) {
        final String classNameUsingDots = generatedClassName;

        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String ourMapClassName = theMapType.getName().replace('.', '/');
        final String ourMapCompanionObjectName = ourMapClassName + "$";
        final String ourMapCompanionObjectDescriptor = "L" + ourMapCompanionObjectName + ";";


        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, null, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap.java", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
            annotationVisitor0.visit("value", alias);
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("java/util/Map$Entry", "java/util/Map", "Entry", ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

        {
            fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "map", "Lscala/collection/Map;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Lscala/collection/Map;)V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", "<init>", "()V", false);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "map", "Lscala/collection/Map;");
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitInsn(RETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
            methodVisitor.visitLocalVariable("map", "Lscala/collection/Map;", null, label0, label3, 1);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()Lscala/collection/Map;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "map", "Lscala/collection/Map;");
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
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
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "map", "Lscala/collection/Map;");
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Map", "iterator", "()Lscala/collection/Iterator;", true);
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Map", "scala/collection/Iterator"}, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "hasNext", "()Z", true);
            Label label3 = new Label();
            methodVisitor.visitJumpInsn(IFEQ, label3);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "next", "()Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "scala/Tuple2");
            methodVisitor.visitVarInsn(ASTORE, 3);
            Label label5 = new Label();
            methodVisitor.visitLabel(label5);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 3);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Tuple2", "_1", "()Ljava/lang/Object;", false);
            genParameterType(methodVisitor, keyType, new OperandStack());
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            methodVisitor.visitVarInsn(ALOAD, 3);
            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Tuple2", "_2", "()Ljava/lang/Object;", false);
            genParameterType(methodVisitor, valueType, new OperandStack());
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            Label label7 = new Label();
            methodVisitor.visitLabel(label7);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitInsn(POP);
            Label label8 = new Label();
            methodVisitor.visitLabel(label8);
            methodVisitor.visitJumpInsn(GOTO, label2);
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitLdcInsn("map");
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
            methodVisitor.visitInsn(ARETURN);
            Label label9 = new Label();
            methodVisitor.visitLabel(label9);
            methodVisitor.visitLocalVariable("entry", "Lscala/Tuple2;", null, label5, label8, 3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label9, 0);
            methodVisitor.visitLocalVariable("serialized", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label9, 1);
            methodVisitor.visitLocalVariable("iterator", "Lscala/collection/Iterator;", null, label2, label9, 2);
            methodVisitor.visitMaxs(5, 4);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassDescriptor, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("map");
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
            methodVisitor.visitVarInsn(ASTORE, 1);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitFieldInsn(GETSTATIC, ourMapCompanionObjectName, "MODULE$", ourMapCompanionObjectDescriptor);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, ourMapCompanionObjectName, "newBuilder", "()Lscala/collection/mutable/Builder;", false);
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
            methodVisitor.visitVarInsn(ASTORE, 3);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_APPEND,3, new Object[] {"java/util/Map", "scala/collection/mutable/Builder", "java/util/Iterator"}, 0, null);
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
            methodVisitor.visitTypeInsn(NEW, "scala/Tuple2");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 4);
            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
            genParameterType(methodVisitor, keyType, new OperandStack());
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            methodVisitor.visitVarInsn(ALOAD, 4);
            Label label7 = new Label();
            methodVisitor.visitLabel(label7);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
            genParameterType(methodVisitor, valueType, new OperandStack());
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "scala/Tuple2", "<init>", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
            Label label8 = new Label();
            methodVisitor.visitLabel(label8);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/mutable/Builder", "addOne", "(Ljava/lang/Object;)Lscala/collection/mutable/Growable;", true);
            methodVisitor.visitInsn(POP);
            Label label9 = new Label();
            methodVisitor.visitLabel(label9);
            methodVisitor.visitJumpInsn(GOTO, label3);
            methodVisitor.visitLabel(label4);
            methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
            methodVisitor.visitTypeInsn(NEW, generatedClassName);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/mutable/Builder", "result", "()Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "scala/collection/Map");
            methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(Lscala/collection/Map;)V", false);
            methodVisitor.visitInsn(ARETURN);
            Label label10 = new Label();
            methodVisitor.visitLabel(label10);
            methodVisitor.visitLocalVariable("entry", "Ljava/util/Map$Entry;", null, label5, label9, 4);
            methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label10, 0);
            methodVisitor.visitLocalVariable("serialized", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label10, 1);
            methodVisitor.visitLocalVariable("builder", "Lscala/collection/mutable/Builder;", null, label2, label10, 2);
            methodVisitor.visitMaxs(7, 5);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
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
            methodVisitor.visitTypeInsn(INSTANCEOF, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap");
            Label label2 = new Label();
            methodVisitor.visitJumpInsn(IFNE, label2);
            methodVisitor.visitInsn(ICONST_0);
            methodVisitor.visitInsn(IRETURN);
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap");
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", "getValue", "()Ljava/lang/Object;", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            methodVisitor.visitInsn(IRETURN);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label4, 0);
            methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
            methodVisitor.visitLocalVariable("that", "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap;", null, label3, label4, 2);
            methodVisitor.visitMaxs(2, 3);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/collection/Map;", false);
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

    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] makeMapN(final int N, String generatedClassName, final Class<?> theMapType, final String alias,
                                   final ParameterType keyType, final ParameterType valueType, final ScalaPluginClassLoader plugin) {
        final String classNameUsingDots = generatedClassName;   //TODO shouldn't this be used? or is it redundant?
        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String mapNClassName = theMapType.getName().replace('.', '/');
        final String mapNClassDescriptor = "L" + mapNClassName + ";";


        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, null, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap.java", null);

        {
        annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
        annotationVisitor0.visit("value", alias);
        annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass(mapNClassName, "scala/collection/immutable/Map", "Map" + N, ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        classWriter.visitInnerClass("java/util/Map$Entry", "java/util/Map", "Entry", ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

        {
        fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "map", mapNClassDescriptor, null, null);
        fieldVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(" + mapNClassDescriptor + ")V", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", "<init>", "()V", false);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "map", mapNClassDescriptor);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitInsn(RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
        methodVisitor.visitLocalVariable("map", mapNClassDescriptor, null, label0, label3, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()" + mapNClassDescriptor, null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "map", mapNClassDescriptor);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
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
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "map", mapNClassDescriptor);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, mapNClassName, "iterator", "()Lscala/collection/Iterator;", false);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Map", "scala/collection/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "hasNext", "()Z", true);
        Label label3 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label3);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "scala/Tuple2");
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 3);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Tuple2", "_1", "()Ljava/lang/Object;", false);
        genParameterType(methodVisitor, keyType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitVarInsn(ALOAD, 3);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Tuple2", "_2", "()Ljava/lang/Object;", false);
        genParameterType(methodVisitor, valueType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        Label label7 = new Label();
        methodVisitor.visitLabel(label7);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitInsn(POP);
        Label label8 = new Label();
        methodVisitor.visitLabel(label8);
        methodVisitor.visitJumpInsn(GOTO, label2);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitLdcInsn("map");
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label9 = new Label();
        methodVisitor.visitLabel(label9);
        methodVisitor.visitLocalVariable("entry", "Lscala/Tuple2;", null, label5, label8, 3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label9, 0);
        methodVisitor.visitLocalVariable("serialized", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;", label1, label9, 1);
        methodVisitor.visitLocalVariable("iterator", "Lscala/collection/Iterator;", null, label2, label9, 2);
        methodVisitor.visitMaxs(5, 4);
        methodVisitor.visitEnd();
        }
        {
        final OperandStack operandStack = new OperandStack();
        final LocalVariableTable localVariableTable = new LocalVariableTable();
        final LocalCounter localCounter = new LocalCounter();

        final Label startLabel = new Label();
        final Label endLabel = new Label();

        final int argumentMapIndex = localCounter.getSlotIndex(), argumentMapFrameIndex = localCounter.getFrameIndex();
        final LocalVariable argumentMapVariable = new LocalVariable("map", "Ljava/util/Map;", null, startLabel, endLabel, argumentMapIndex, argumentMapFrameIndex);
        localVariableTable.add(argumentMapVariable); localCounter.add(Type.getType(java.util.Map.class));


        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassDescriptor, null);
        methodVisitor.visitCode();

        methodVisitor.visitLabel(startLabel);

        //java.util.Map serialized = (java.util.Map) map.get("map");
        methodVisitor.visitVarInsn(ALOAD, argumentMapIndex);                                    operandStack.push(Type.getType(java.util.Map.class));
        methodVisitor.visitLdcInsn("map");                                                  operandStack.push(Type.getType(java.lang.Object.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);     operandStack.replaceTop(2, Type.getType(Object.class));
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");                            operandStack.replaceTop(Type.getType(java.util.Map.class));
        final int serializedMapIndex = localCounter.getSlotIndex(), serializedMapFrameIndex = localCounter.getFrameIndex();
        methodVisitor.visitVarInsn(ASTORE, serializedMapIndex);                                 operandStack.pop();
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        final LocalVariable serializedMapVariable = new LocalVariable("serialized", "Ljava/util/Map;", null, label1, endLabel, serializedMapIndex, serializedMapFrameIndex);
        localVariableTable.add(serializedMapVariable); localCounter.add(Type.getType(java.util.Map.class));

        //java.util.Iterator<java.util.Map.Entry> iterator = serialized.entrySet().iterator();
        methodVisitor.visitVarInsn(ALOAD, serializedMapIndex);                                  operandStack.push(Type.getType(java.util.Map.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);                     operandStack.replaceTop(Type.getType(java.util.Set.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);                operandStack.replaceTop(Type.getType(java.util.Iterator.class));
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                                      operandStack.pop();
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        final LocalVariable iteratorVariable = new LocalVariable("iterator", "Ljava/util/Iterator;", null, label3, endLabel, iteratorIndex, iteratorFrameIndex);
        localVariableTable.add(iteratorVariable); localCounter.add(Type.getType(java.util.Iterator.class));

        //prepare for return: new Adapter, new MapN, ...
        methodVisitor.visitTypeInsn(NEW, generatedClassName);                                   operandStack.push(Type.getType(generatedClassDescriptor));
        methodVisitor.visitInsn(DUP);                                                           operandStack.push(Type.getType(generatedClassDescriptor));
        methodVisitor.visitTypeInsn(NEW, mapNClassName);                                        operandStack.push(Type.getType(mapNClassDescriptor));
        methodVisitor.visitInsn(DUP);                                                           operandStack.push(Type.getType(mapNClassDescriptor));

        //prepare arguments
        for (int k = 1; k <= N; k++) {
            //just call iterator.next() unsafely because we know how many elements there are!

            //java.util.Map.Entry entryK = iterator.next();
            methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                   operandStack.push(Type.getType(java.util.Iterator.class));
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);             operandStack.replaceTop(Type.getType(Object.class));
            methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");                  operandStack.replaceTop(Type.getType(java.util.Map.Entry.class));
            final int entryKIndex = localCounter.getSlotIndex(), entryKFrameIndex = localCounter.getFrameIndex();
            methodVisitor.visitVarInsn(ASTORE, entryKIndex);                                    operandStack.pop();
            final Label entryKLabel = new Label();
            methodVisitor.visitLabel(entryKLabel);
            final LocalVariable entryKVariable = new LocalVariable("entry" + k, "Ljava/util/Map$Entry;", null, entryKLabel, endLabel, entryKIndex, entryKFrameIndex);
            localVariableTable.add(entryKVariable); localCounter.add(Type.getType(java.util.Map.Entry.class));

            //RuntimeConversions.deserialize(entry.getKey(), keyType, pluginClassLoader);
            methodVisitor.visitVarInsn(ALOAD, entryKIndex);                                     operandStack.push(Type.getType(java.util.Map.Entry.class));
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);          operandStack.replaceTop(Type.getType(Object.class));
            genParameterType(methodVisitor, keyType, operandStack);
            genScalaPluginClassLoader(methodVisitor, plugin, operandStack);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            /*just leave it on top of the stack!*/                                              operandStack.replaceTop(3, Type.getType(Object.class));

            //RuntimeConversions.deserialize(entry.getValue(), valueType, pluginClassLoader);
            methodVisitor.visitVarInsn(ALOAD, entryKIndex);                                     operandStack.push(Type.getType(java.util.Map.Entry.class));
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);        operandStack.replaceTop(Type.getType(Object.class));
            genParameterType(methodVisitor, valueType, operandStack);
            genScalaPluginClassLoader(methodVisitor, plugin, operandStack);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            /*just leave it on top of the stack!*/                                              operandStack.replaceTop(3, Type.getType(Object.class));
        }

        //now invoke the constructors: new MapNAdapter(new MapN(arguments));
        methodVisitor.visitMethodInsn(INVOKESPECIAL, mapNClassName, "<init>", "(" + Compat.stringRepeat("Ljava/lang/Object;Ljava/lang/Object;", N) + ")V", false);        operandStack.pop(N + 1); //arguments + the type itself
        methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(" + mapNClassDescriptor + ")V", false);                operandStack.pop(2);
        methodVisitor.visitInsn(ARETURN);

        methodVisitor.visitLabel(endLabel);
        for (LocalVariable local : localVariableTable) {
            methodVisitor.visitLocalVariable(local.name, local.descriptor, local.signature, local.startLabel, local.endLabel, local.tableSlot);
        }
        methodVisitor.visitMaxs(operandStack.maxStack(), localVariableTable.maxLocals());
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + mapNClassDescriptor, false);
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
        methodVisitor.visitTypeInsn(INSTANCEOF, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap");
        Label label2 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitInsn(IRETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap");
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + mapNClassDescriptor, false);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap", "getValue", "()Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        methodVisitor.visitInsn(IRETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label4, 0);
        methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
        methodVisitor.visitLocalVariable("that", "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaMap;", null, label3, label4, 2);
        methodVisitor.visitMaxs(2, 3);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + mapNClassDescriptor, false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + mapNClassDescriptor, false);
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

/*
@SerializableAs("THE_MAP_IMPLEMENTATION_CLASS_NAME")    //in bytecode replace this by the map class name
final class MapAdapter extends ScalaMap {
    private final scala.collection.Map map;

    public MapAdapter(scala.collection.Map map) {
        this.map = map;
    }

    @Override
    public scala.collection.Map getValue() {
        return map;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<Object, Object> serialized = new java.util.LinkedHashMap<>();

        scala.collection.Iterator iterator = map.iterator();
        while (iterator.hasNext()) {
            scala.Tuple2 entry = (scala.Tuple2) iterator.next();
            serialized.put(RuntimeConversions.serialize(entry._1(), (ParameterType) null, (ScalaPluginClassLoader) null),
                    RuntimeConversions.serialize(entry._2(), (ParameterType) null, (ScalaPluginClassLoader) null));
        }

        return Collections.singletonMap("map", serialized);
    }

    public static MapAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.Map<Object, Object> serialized = (java.util.Map<Object, Object>) map.get("map");

        scala.collection.mutable.Builder builder = scala.collection.immutable.HashMap$.MODULE$.newBuilder();
        //in bytecode use the companion object of the actual map type to get the builder

        for (java.util.Map.Entry entry : serialized.entrySet()) {
            builder.addOne(new scala.Tuple2(
                    RuntimeConversions.deserialize(entry.getKey(), (ParameterType) null, (ScalaPluginClassLoader) null),
                    RuntimeConversions.deserialize(entry.getValue(), (ParameterType) null, (ScalaPluginClassLoader) null)
            ));
        }

        return new MapAdapter((scala.collection.Map) builder.result());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ScalaMap)) return false;
        ScalaMap that = (ScalaMap) o;
        return Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public String toString() {
        return Objects.toString(getValue());
    }
}
*/

/*
@SerializableAs(ScalaMap.SCALA_IMMUTABLE_MAP + ".MapN") //in bytecode replace N by the map size
final class MapNAdapter extends ScalaMap {
    private final scala.collection.immutable.Map.Map1 map;  //in bytecode make this dependent on the map size.

    public MapNAdapter(scala.collection.immutable.Map.Map1 map) { //MapN
        this.map = map;
    }

    @Override
    public scala.collection.immutable.Map.Map1 getValue() { //MapN
        return map;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<Object, Object> serialized = new java.util.LinkedHashMap<>();

        scala.collection.Iterator iterator = map.iterator();
        while (iterator.hasNext()) {
            scala.Tuple2 entry = (scala.Tuple2) iterator.next();
            serialized.put(RuntimeConversions.serialize(entry._1(), (ParameterType) null, (ScalaPluginClassLoader) null),
                    RuntimeConversions.serialize(entry._2(), (ParameterType) null, (ScalaPluginClassLoader) null));
        }

        return Collections.singletonMap("map", serialized);
    }

    public static MapNAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.Map<Object, Object> serialized = (java.util.Map<Object, Object>) map.get("map");

        final java.util.Iterator<java.util.Map.Entry<Object, Object>> entrySetIterator = serialized.entrySet().iterator();

        //for (int k = 1; k <= N; k++) {
            java.util.Map.Entry<Object, Object> entry = entrySetIterator.next();

            RuntimeConversions.deserialize(entry.getKey(), (ParameterType) null, (ScalaPluginClassLoader) null);
            //in bytecode, don't pop this, just leave it on the stack.
            RuntimeConversions.deserialize(entry.getValue(), (ParameterType) null, (ScalaPluginClassLoader) null);
            //in bytecode, don't pop this, just leave it on the stack.

            //k++
        //}

        return new MapNAdapter(new scala.collection.immutable.Map.Map1(null, null)); //MapN(key1, value1 ... keyN, valueN)
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ScalaMap)) return false;
        ScalaMap that = (ScalaMap) o;
        return Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public String toString() {
        return Objects.toString(getValue());
    }
}
*/
