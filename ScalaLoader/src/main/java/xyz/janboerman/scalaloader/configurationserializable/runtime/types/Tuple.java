package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.*;
import xyz.janboerman.scalaloader.bytecode.*;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

public class Tuple {

    static final String TUPLE_XXL = "scala.runtime.TupleXXL";

    private Tuple() {}

    public static boolean isTuple(Object live) {
        if (live == null) return false;

        String rawTypeName = live.getClass().getName();

        for (int arity = 1; arity <= 22; arity++)
            if (("scala.Tuple" + arity).equals(rawTypeName))
                return true;

        if (TUPLE_XXL.equals(rawTypeName))
            return true;

        return false;
    }

    private static int getArity(Class<?> scalaTupleClass) {
        switch (scalaTupleClass.getName()) {
            case "scala.Tuple1": return 1;
            case "scala.Tuple2": return 2;
            case "scala.Tuple3": return 3;
            case "scala.Tuple4": return 4;
            case "scala.Tuple5": return 5;
            case "scala.Tuple6": return 6;
            case "scala.Tuple7": return 7;
            case "scala.Tuple8": return 8;
            case "scala.Tuple9": return 9;
            case "scala.Tuple10": return 10;
            case "scala.Tuple11": return 11;
            case "scala.Tuple12": return 12;
            case "scala.Tuple13": return 13;
            case "scala.Tuple14": return 14;
            case "scala.Tuple15": return 15;
            case "scala.Tuple16": return 16;
            case "scala.Tuple17": return 17;
            case "scala.Tuple18": return 18;
            case "scala.Tuple19": return 19;
            case "scala.Tuple20": return 20;
            case "scala.Tuple21": return 21;
            case "scala.Tuple22": return 22;
            case "scala.runtime.TupleXXL": return -1;
            default: return 0;
        }
    }

    public static ConfigurationSerializable serialize(Object scalaTuple, ParameterType type, ScalaPluginClassLoader pluginClassLoader) {
        final int arity = getArity(scalaTuple.getClass());
        assert arity != 0 : "Not a scala tuple";

        if (type instanceof ParameterizedParameterType || arity > 0) {
            List<? extends ParameterType> tupleTypeArguments;
            if (type instanceof ParameterizedParameterType) {
                ParameterizedParameterType ppt = (ParameterizedParameterType) type;
                tupleTypeArguments = ppt.getTypeParameters();
            } else {
                tupleTypeArguments = java.util.stream.Stream.generate(() -> ParameterType.from(Object.class))
                        .limit(arity)
                        .collect(Collectors.toList());
            }

            String serializedClassName = "scala.Tuple" + arity;
            String generatedClassName = PREFIX_USING_DOTS + serializedClassName;

            ClassDefineResult classDefineResult = pluginClassLoader.getOrDefineClass(generatedClassName,
                    className -> makeTupleN(className, tupleTypeArguments, pluginClassLoader),
                    true);
            Class<? extends ConfigurationSerializable> wrapperClazz = (Class<? extends ConfigurationSerializable>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                //put plugin name in the name of the generated class? to workaround a design flaw in bukkit?
                ConfigurationSerialization.registerClass(wrapperClazz, serializedClassName);
            }

            try {
                Constructor<? extends ConfigurationSerializable> constructor = wrapperClazz.getConstructor(type.getRawType());
                return constructor.newInstance(scalaTuple);
            } catch (Exception shouldNotOccur) {
                throw new Error("Malformed generated class", shouldNotOccur);
            }
        }

        else if (TUPLE_XXL.equals(type.getRawType().getName()) || TUPLE_XXL.equals(scalaTuple.getClass().getName())) {
            String serializedClassName = TUPLE_XXL;
            String generatedClassName = PREFIX_USING_DOTS + serializedClassName;

            ClassDefineResult classDefineResult = pluginClassLoader.getOrDefineClass(generatedClassName,
                    className -> makeTupleXXL(className, pluginClassLoader),
                    true);
            Class<? extends ConfigurationSerializable> wrapperClazz = (Class<? extends ConfigurationSerializable>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                //put plugin name in the name of the generated class? to work around a design flaw in bukkit?
                ConfigurationSerialization.registerClass(wrapperClazz, serializedClassName);
            }

            try {
                Constructor<? extends ConfigurationSerializable> constructor = wrapperClazz.getConstructor(type.getRawType());
                return constructor.newInstance(scalaTuple);
            } catch (Exception shouldNotOccur) {
                throw new Error("Malformed generated class", shouldNotOccur);
            }
        }

        throw new RuntimeException("Could not serialize tuple: " + scalaTuple + ", of type: " + type);
    }

    private static byte[] makeTupleN(String generatedClassName, final List<? extends ParameterType> tupleTypeArguments, final ScalaPluginClassLoader plugin) {
        final int arity = tupleTypeArguments.size();

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        final String classNameUsingDots = generatedClassName;

        generatedClassName = generatedClassName.replace('.', '/');
        final String classSignature = classSignature(arity);
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String generatedClassSignature = "L" + generatedClassName + typeArguments(arity) + ";";

        final String tupleName = tupleName(arity);
        final String tupleNameUsingDots = tupleName.replace('/', '.');
        final String tupleDescriptor = tupleDescriptor(arity);
        final String tupleSignature = tupleSignature(arity);
        final String tupleConstructorDescriptor = tupleConstructorDescriptor(arity);

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, generatedClassName, classSignature, "java/lang/Object", new String[] { Type.getInternalName(Adapter.class) });

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/Tuple.java", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
            annotationVisitor0.visit("value", tupleNameUsingDots);
            annotationVisitor0.visitEnd();
        }
        {
            fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "tuple", tupleDescriptor, tupleSignature, null);
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(" + tupleDescriptor + ")V", "(" + tupleSignature + ")V", null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "tuple", tupleDescriptor);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitInsn(RETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label3, 0);
            methodVisitor.visitLocalVariable("tuple", tupleDescriptor, tupleSignature, label0, label3, 1);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()" + tupleDescriptor, "()" + tupleSignature, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", tupleDescriptor);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {   //serialize
            final OperandStack operandStack = new OperandStack();
            final LocalVariableTable localVariableTable = new LocalVariableTable();

            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "serialize", "()Ljava/util/Map;", "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
            methodVisitor.visitCode();

            final Label beginLabel = new Label();
            final Label endLabel = new Label();
            methodVisitor.visitLabel(beginLabel);

            final LocalVariable thisVariable = new LocalVariable("this", generatedClassDescriptor, generatedClassSignature, beginLabel, endLabel, 0);
            localVariableTable.add(thisVariable);

            methodVisitor.visitTypeInsn(NEW, "java/util/HashMap");      operandStack.push(Type.getType(HashMap.class));
            methodVisitor.visitInsn(DUP);                                   operandStack.push(Type.getType(HashMap.class));
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);      operandStack.pop();
            methodVisitor.visitVarInsn(ASTORE, 1);                      operandStack.pop();

            final LocalVariable mapVariable = new LocalVariable("map", Type.getDescriptor(HashMap.class), "Ljava/util/HashMap<Ljava/lang/String;Ljava/lang/Object;>;", beginLabel, endLabel, 1);
            localVariableTable.add(mapVariable);

            for (int a = 1; a <= arity; a++) {
                methodVisitor.visitVarInsn(ALOAD, 1);                   operandStack.push(Type.getType(Map.class));
                methodVisitor.visitLdcInsn("_" + a);                    operandStack.push(Type.getType(String.class));
                methodVisitor.visitVarInsn(ALOAD, 0);                       operandStack.push(Type.getType(generatedClassDescriptor));
                methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", tupleDescriptor);                                   operandStack.replaceTop(Type.getType(tupleDescriptor));
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, tupleName, "_" + a, "()Ljava/lang/Object;", false);       operandStack.replaceTop(Type.getType(Object.class));
                genParameterType(methodVisitor, tupleTypeArguments.get(a - 1), operandStack);
                genScalaPluginClassLoader(methodVisitor, plugin, operandStack);
                methodVisitor.visitMethodInsn(INVOKESTATIC,
                        Type.getInternalName(RuntimeConversions.class),
                        "serialize",
                        "(" + AsmConstants.javaLangObject_DESCRIPTOR + Type.getDescriptor(ParameterType.class) + Type.getDescriptor(ScalaPluginClassLoader.class) + ")" + AsmConstants.javaLangObject_DESCRIPTOR,
                        false);                                         operandStack.replaceTop(3, Type.getType(Object.class));
                methodVisitor.visitMethodInsn(INVOKEINTERFACE,
                        Type.getInternalName(Map.class),
                        "put",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)),
                        true);                                          operandStack.replaceTop(3, Type.getType(Object.class));
                methodVisitor.visitInsn(POP);                                   operandStack.pop(); //pop result of Map.put
            }

            Label returnLabel = new Label();
            methodVisitor.visitLabel(returnLabel);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitInsn(ARETURN);

            methodVisitor.visitLabel(endLabel);
            for (LocalVariable local : localVariableTable) {
                methodVisitor.visitLocalVariable(local.name, local.descriptor, local.signature, local.startLabel, local.endLabel, local.tableIndex);
            }
            methodVisitor.visitMaxs(operandStack.maxStack(), localVariableTable.maxLocals());
            methodVisitor.visitEnd();
        }
        {   //deserialize
            StringBuilder sb = new StringBuilder();
            sb.append("<");
            for (int a = 1; a <= arity; a++) sb.append("T" + a + ":Ljava/lang/Object;");
            sb.append(">");
            sb.append("(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)");
            sb.append(generatedClassSignature);
            final String deserializeSignature = sb.toString();

            final OperandStack operandStack = new OperandStack();
            final LocalVariableTable localVariableTable = new LocalVariableTable();

            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, deserializeSignature, null);
            methodVisitor.visitCode();
            Label beginLabel = new Label();
            Label endLabel = new Label();
            methodVisitor.visitLabel(beginLabel);

            LocalVariable mapVariable = new LocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", beginLabel, endLabel, 0);
            localVariableTable.add(mapVariable);

            for (int a = 1; a <= arity; a++) {
                methodVisitor.visitVarInsn(ALOAD, 0);                       operandStack.push(Type.getType(Map.class));
                methodVisitor.visitLdcInsn("_" + a);                            operandStack.push(Type.getType(String.class));
                methodVisitor.visitMethodInsn(INVOKEINTERFACE,
                        "java/util/Map",
                        "get",
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        true);                                              operandStack.replaceTop(2, Type.getType(Object.class));
                genParameterType(methodVisitor, tupleTypeArguments.get(a - 1), operandStack);
                genScalaPluginClassLoader(methodVisitor, plugin, operandStack);
                methodVisitor.visitMethodInsn(INVOKESTATIC,
                        Type.getInternalName(RuntimeConversions.class),
                        "deserialize",
                        Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(ParameterType.class), Type.getType(ScalaPluginClassLoader.class)),
                        false);                                         operandStack.replaceTop(3, Type.getType(Object.class));
                methodVisitor.visitVarInsn(ASTORE, a);                              operandStack.pop();
                //methodVisitor.visitLocalVariable("_1", "Ljava/lang/Object;", "TT1;", label1, label3, 1);
                LocalVariable _variable = new LocalVariable("_" + a, "Ljava/lang/Object;", "TT" + a + ";", beginLabel, endLabel, a);
                localVariableTable.add(_variable);
            }

            Label returnLabel = new Label();
            methodVisitor.visitLabel(returnLabel);
            methodVisitor.visitTypeInsn(NEW, generatedClassName);                   operandStack.push(Type.getType(generatedClassDescriptor));
            methodVisitor.visitInsn(DUP);                                           operandStack.push(Type.getType(generatedClassDescriptor));
            methodVisitor.visitTypeInsn(NEW, tupleName);                            operandStack.push(Type.getObjectType(tupleName));
            methodVisitor.visitInsn(DUP);                                           operandStack.push(Type.getObjectType(tupleName));
            for (int a = 1; a <= arity; a++) {
                methodVisitor.visitVarInsn(ALOAD, a);                               operandStack.push(Type.getType(Object.class));
            }
            methodVisitor.visitMethodInsn(INVOKESPECIAL,
                    tupleName,
                    "<init>",
                    tupleConstructorDescriptor,
                    false);                                                 operandStack.pop(arity + 1);
            methodVisitor.visitMethodInsn(INVOKESPECIAL,
                    generatedClassName, "<init>", "(" + tupleDescriptor + ")V",
                    false);                                                         operandStack.pop(2);
            methodVisitor.visitInsn(ARETURN);

            methodVisitor.visitLabel(endLabel);
            for (LocalVariable local : localVariableTable){
                methodVisitor.visitLocalVariable(local.name, local.descriptor, local.signature, local.startLabel, local.endLabel, local.tableIndex);
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
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", tupleDescriptor);
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
            methodVisitor.visitLineNumber(113, label3);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", tupleDescriptor);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", tupleDescriptor);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            methodVisitor.visitInsn(IRETURN);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label4, 0);
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
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", tupleDescriptor);
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
            methodVisitor.visitLineNumber(74, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + tupleDescriptor, false);
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


    private static byte[] makeTupleXXL(String generatedClassName, ScalaPluginClassLoader plugin) {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        final String classNameUsingDots = generatedClassName;

        generatedClassName = generatedClassName.replace('.', '/');
        final String classSignature = "Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/Adapter<Lscala/runtime/TupleXXL;>;";
        final String generatedClassDescriptor = "L" + generatedClassName + ";";

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, generatedClassName, classSignature, "java/lang/Object", new String[] { Type.getInternalName(Adapter.class) });

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/Tuple.java", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
            annotationVisitor0.visit("value", TUPLE_XXL);
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("java/util/Map$Entry", "java/util/Map", "Entry", ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT | ACC_INTERFACE);

        {
            fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "tuple", "Lscala/runtime/TupleXXL;", null, null);
            fieldVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Lscala/runtime/TupleXXL;)V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitInsn(RETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
            methodVisitor.visitLocalVariable("tuple", "Lscala/runtime/TupleXXL;", null, label0, label3, 1);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()Lscala/runtime/TupleXXL;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {   //serialize
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
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitVarInsn(ISTORE, 2);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Map", Opcodes.INTEGER}, 0, null);
            methodVisitor.visitVarInsn(ILOAD, 2);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/runtime/TupleXXL", "productArity", "()I", false);
            Label label3 = new Label();
            methodVisitor.visitJumpInsn(IF_ICMPGT, label3);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLineNumber(760, label4);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            methodVisitor.visitLdcInsn("_");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitVarInsn(ILOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            methodVisitor.visitVarInsn(ILOAD, 2);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(ISUB);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/runtime/TupleXXL", "productElement", "(I)Ljava/lang/Object;", false);
            methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType", "from", "(Ljava/lang/reflect/Type;)Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;", false);
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)Ljava/lang/Object;", false);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitInsn(POP);
            Label label5 = new Label();
            methodVisitor.visitLabel(label5);
            methodVisitor.visitLineNumber(759, label5);
            methodVisitor.visitIincInsn(2, 1);
            methodVisitor.visitJumpInsn(GOTO, label2);
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLineNumber(762, label3);
            methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitInsn(ARETURN);
            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitLocalVariable("a", "I", null, label2, label3, 2);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label6, 0);
            methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label1, label6, 1);
            methodVisitor.visitMaxs(5, 3);
            methodVisitor.visitEnd();
        }
        {   //deserialize
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassDescriptor, null);
            methodVisitor.visitCode();
            Label labelNeg1 = new Label();
            methodVisitor.visitLabel(labelNeg1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn("==");
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            methodVisitor.visitInsn(POP);
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "size", "()I", true);
            methodVisitor.visitVarInsn(ISTORE, 1);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ILOAD, 1);
            methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;", true);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);
            methodVisitor.visitVarInsn(ASTORE, 3);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_APPEND,3, new Object[] {Opcodes.INTEGER, "[Ljava/lang/Object;", "java/util/Iterator"}, 0, null);
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
            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;", true);
            methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitInsn(ISUB);
            methodVisitor.visitVarInsn(ISTORE, 5);
            Label label6 = new Label();
            methodVisitor.visitLabel(label6);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitVarInsn(ILOAD, 5);
            methodVisitor.visitVarInsn(ALOAD, 4);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;", true);
            methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType", "from", "(Ljava/lang/reflect/Type;)Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;", false);
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)Ljava/lang/Object;", false);
            methodVisitor.visitInsn(AASTORE);
            Label label7 = new Label();
            methodVisitor.visitLabel(label7);
            methodVisitor.visitJumpInsn(GOTO, label3);
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLineNumber(803, label4);
            methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
            methodVisitor.visitTypeInsn(NEW, generatedClassName);
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "scala/runtime/TupleXXL", "fromIArray", "([Ljava/lang/Object;)Lscala/runtime/TupleXXL;", false);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(Lscala/runtime/TupleXXL;)V", false);
            methodVisitor.visitInsn(ARETURN);
            Label label8 = new Label();
            methodVisitor.visitLabel(label8);
            methodVisitor.visitLocalVariable("index", "I", null, label6, label7, 5);
            methodVisitor.visitLocalVariable("entry", "Ljava/util/Map$Entry;", "Ljava/util/Map$Entry<Ljava/lang/String;Ljava/lang/Object;>;", label5, label7, 4);
            methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label8, 0);
            methodVisitor.visitLocalVariable("size", "I", null, label1, label8, 1);
            methodVisitor.visitLocalVariable("elements", "[Ljava/lang/Object;", null, label2, label8, 2);
            methodVisitor.visitMaxs(5, 6);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
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
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
            methodVisitor.visitInsn(IRETURN);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label4, 0);
            methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
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
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "tuple", "Lscala/runtime/TupleXXL;");
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
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
            methodVisitor.visitLineNumber(742, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Lscala/runtime/TupleXXL;", false);
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



    private static String classSignature(final int arity) {
        assert 1 <= arity && arity <= 22 : "Invalid arity: " + arity;

        StringBuilder sb = new StringBuilder();

        //class type parameters
        sb.append("<");
        for (int a = 1; a <= arity; a++) {
            sb.append("T" + a + ":" + AsmConstants.javaLangObject_DESCRIPTOR);
        }
        sb.append(">");

        //supertype
        sb.append(AsmConstants.javaLangObject_DESCRIPTOR);

        //interfaces
        sb.append("Lxyz/janboerman/scalaloader/configurationserializable/runtime/Adapter<");
        sb.append(tupleSignature(arity));
        sb.append(">;"); //close 'Adapter'

        return sb.toString();
    }

    private static String tupleSignature(final int arity) {
        assert 1 <= arity && arity <= 22 : "Invalid arity: " + arity;

        StringBuilder sb = new StringBuilder();
        sb.append("Lscala/Tuple" + arity + "<");
        for (int a = 1; a <= arity; a++) {
            sb.append("TT" + a + ";");
        }
        sb.append(">;"); //close 'TupleX'
        return sb.toString();
    }

    private static String tupleName(final int arity) {
        assert 1 <= arity && arity <= 22 : "Invalid arity: " + arity;

        return "scala/Tuple" + arity;
    }

    private static String tupleDescriptor(final int arity) {
        assert 1 <= arity && arity <= 22 : "Invalid arity: " + arity;

        return "Lscala/Tuple" + arity + ";";
    }

    private static String tupleConstructorDescriptor(final int arity) {
        assert 1 <= arity && arity <= 22 : "Invalid arity: " + arity;

        return "(" + Compat.stringRepeat("Ljava/lang/Object;", arity) + ")V";
    }

    private static String typeArguments(final int arity) {
        assert 1 <= arity && arity <= 22 : "Invalid arity: " + arity;

        StringJoiner sj = new StringJoiner("", "<", ">");
        for (int a = 1; a <= arity; a++)
            sj.add("TT" + a + ";");
        return sj.toString();
    }

}

// Code used to generate the bytecode:

/*
@SerializableAs("scala.Tuple2")
class Tuple2Adapter<T1, T2> implements Adapter<Tuple2<T1, T2>> {

    private final Tuple2<T1, T2> tuple;

    Tuple2Adapter(Tuple2<T1, T2> tuple) {
        this.tuple = tuple;
    }

    @Override
    public Tuple2<T1, T2> getValue() {
        return tuple;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("_1", RuntimeConversions.serialize(tuple._1(), (ParameterType) null, (ScalaPluginClassLoader) null));
        map.put("_2", RuntimeConversions.serialize(tuple._2(), (ParameterType) null, (ScalaPluginClassLoader) null));
        return map;
    }

    public static <T1, T2> Tuple2Adapter<T1, T2> deserialize(Map<String, Object> map) {
        T1 _1 = (T1) RuntimeConversions.deserialize(map.get("_1"), (ParameterType) null, (ScalaPluginClassLoader) null);
        T2 _2 = (T2) RuntimeConversions.deserialize(map.get("_2"), (ParameterType) null, (ScalaPluginClassLoader) null);
        return new Tuple2Adapter<>(new Tuple2<>(_1, _2));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tuple);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Tuple2Adapter)) return false;

        Tuple2Adapter that = (Tuple2Adapter) obj;
        return Objects.equals(this.tuple, that.tuple);
    }

    @Override
    public String toString() {
        return Objects.toString(tuple);
    }

}
*/

/*
@SerializableAs(Tuple.TUPLE_XXL)
class TupleXXLAdapter implements Adapter<TupleXXL> {

    private final TupleXXL tuple;

    TupleXXLAdapter(TupleXXL tuple) {
        this.tuple = tuple;
    }

    @Override
    public TupleXXL getValue() {
        return tuple;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int a = 1; a <= tuple.productArity(); a++) {
            map.put("_" + a, RuntimeConversions.serialize(tuple.productElement(a-1), ParameterType.from(Object.class), (ScalaPluginClassLoader) null));
        }
        return map;
    }

    public static TupleXXLAdapter deserialize(Map<String, Object> map) {
        map.remove(ConfigurationSerialization.SERIALIZED_TYPE_KEY);
        int size = map.size();
        Object[] elements = new Object[size];
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            int index = Integer.parseInt(entry.getKey().substring(1)) - 1;
            elements[index] = RuntimeConversions.deserialize(entry.getValue(), ParameterType.from(Object.class), (ScalaPluginClassLoader) null);
        }
        return new TupleXXLAdapter(TupleXXL.fromIArray(elements));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tuple);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TupleXXLAdapter)) return false;

        TupleXXLAdapter that = (TupleXXLAdapter) o;
        return Objects.equals(this.tuple, that.tuple);
    }

    @Override
    public String toString() {
        return Objects.toString(tuple);
    }
}
*/
