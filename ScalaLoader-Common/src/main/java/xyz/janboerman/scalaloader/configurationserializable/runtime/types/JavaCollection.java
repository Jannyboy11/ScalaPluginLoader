package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.bytecode.OperandStack;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;

import java.lang.reflect.Constructor;
import java.util.*;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

@Called
public abstract class JavaCollection<T> implements Adapter<Collection<? extends T>> {

    private static final String FOR_GENERIC = PREFIX_USING_DOTS + "JavaCollection$ForGeneric";
    private static final String FOR_ENUMSET = PREFIX_USING_DOTS + "JavaCollection$ForEnumSet";
    private static final String FOR_ENUMSET_ALIAS = "java.util.EnumSet";

    private static final Map<Class<? extends Collection>, Class<? extends JavaCollection<?>>> ForGenericAdapters = new WeakHashMap<>();

    @Called
    public JavaCollection() {}

    public static final boolean isRawtypeCollection(Object live, ParameterType type) {
        return live instanceof Collection && ParameterType.class.equals(type.getClass());
    }

    public static <T> JavaCollection<T> serialize(Object live, ParameterType type, IScalaPluginClassLoader plugin) {

        ParameterType elementType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);

        if (live instanceof EnumSet) {
            //ForEnumSet
            ClassDefineResult classDefineResult = plugin.getOrDefineClass(FOR_ENUMSET,
                    name -> makeForEnumSet(name, elementType, plugin),
                    true);
            Class<? extends JavaCollection> ForEnumSetClass = (Class<? extends JavaCollection>) classDefineResult.getClassDefinition();
            if (classDefineResult.isNew()) {
                ConfigurationSerialization.registerClass(ForEnumSetClass, FOR_ENUMSET_ALIAS);
            }

            try {
                Constructor<? extends JavaCollection> constructor = ForEnumSetClass.getConstructor(EnumSet.class);
                return constructor.newInstance(live);
            } catch (Exception shouldNotOccur) {
                throw new Error("Malformed generated class", shouldNotOccur);
            }
        } else if (live instanceof Collection) {
            //ForGeneric

            Class<? extends Collection> collClass = (Class<? extends Collection>) live.getClass();
            Class<? extends JavaCollection<?>> ForGenericClass = ForGenericAdapters.get(collClass);

            if (ForGenericClass == null) {
                final String className = FOR_GENERIC + "$" + collClass.getName();
                final String alias = collClass.getName();
                ClassDefineResult classDefineResult = plugin.getOrDefineClass(className,
                        name -> makeForGeneric(alias, name, collClass, elementType, plugin),
                        true);
                ForGenericClass = (Class<? extends JavaCollection<?>>) classDefineResult.getClassDefinition();
                if (classDefineResult.isNew()) {
                    ConfigurationSerialization.registerClass(ForGenericClass, alias);
                }
                ForGenericAdapters.put(collClass, ForGenericClass);
            }

            try {
                Constructor<? extends JavaCollection> constructor = ForGenericClass.getConstructor(Collection.class);
                return constructor.newInstance(live);
            } catch (Exception shouldNotOccur) {
                throw new Error("Malformed generated class", shouldNotOccur);
            }
        }

        throw new RuntimeException("Could not serialize java collection: " + live + " of type: " + live.getClass().getName());
    }

    private static byte[] makeForGeneric(final String alias, String generatedClassName, Class<? extends Collection> wrappedCollectionType, final ParameterType elementType, IScalaPluginClassLoader plugin) {
        final String classNameUsingDots = generatedClassName;
        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String generatedClassSignature = "L" + generatedClassName + "<TT;>;";


        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, "<T:Ljava/lang/Object;>Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection<TT;>;", "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection", null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection.java", null);

        {
        annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
        annotationVisitor0.visit("value", alias);
        annotationVisitor0.visitEnd();
        }

        classWriter.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        {
        fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "value", "Ljava/util/Collection;", "Ljava/util/Collection<TT;>;", null);
        fieldVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/Collection;)V", "(Ljava/util/Collection<TT;>;)V", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection", "<init>", "()V", false);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "value", "Ljava/util/Collection;");
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitInsn(RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label3, 0);
        methodVisitor.visitLocalVariable("value", "Ljava/util/Collection;", "Ljava/util/Collection<TT;>;", label0, label3, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()Ljava/util/Collection;", "()Ljava/util/Collection<TT;>;", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", "Ljava/util/Collection;");
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
        methodVisitor.visitLdcInsn("value");
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", "Ljava/util/Collection;");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "stream", "()Ljava/util/stream/Stream;", true);
        methodVisitor.visitInvokeDynamicInsn("apply", "()Ljava/util/function/Function;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false), new Object[]{Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;"), new Handle(Opcodes.H_INVOKESTATIC, generatedClassName, "lambda$serialize$0", "(Ljava/lang/Object;)Ljava/lang/Object;", false), Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;")});
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/stream/Stream", "map", "(Ljava/util/function/Function;)Ljava/util/stream/Stream;", true);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/stream/Collectors", "toList", "()Ljava/util/stream/Collector;", false);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/stream/Stream", "collect", "(Ljava/util/stream/Collector;)Ljava/lang/Object;", true);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label4, 0);
        methodVisitor.visitMaxs(3, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "<T:Ljava/lang/Object;>(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassSignature, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitTypeInsn(NEW, Type.getType(wrappedCollectionType).getInternalName());
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, Type.getType(wrappedCollectionType).getInternalName(), "<init>", "()V", false);
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitLdcInsn("value");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Collection", "java/util/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        Label label3 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label3);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitVarInsn(ALOAD, 3);
        genParameterType(methodVisitor, elementType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);
        methodVisitor.visitInsn(POP);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitJumpInsn(GOTO, label2);
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
        methodVisitor.visitTypeInsn(NEW, generatedClassName);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(Ljava/util/Collection;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLocalVariable("serialized", "Ljava/lang/Object;", null, label4, label5, 3);
        methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label6, 0);
        methodVisitor.visitLocalVariable("resColl", "Ljava/util/Collection;", "Ljava/util/Collection<TT;>;", label1, label6, 1);
        methodVisitor.visitMaxs(4, 4);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Collection;", false);
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
        methodVisitor.visitTypeInsn(INSTANCEOF, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection");
        Label label2 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitInsn(IRETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection");
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Collection;", false);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection", "getValue", "()Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        methodVisitor.visitInsn(IRETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label4, 0);
        methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
        methodVisitor.visitLocalVariable("that", "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection;", null, label3, label4, 2);
        methodVisitor.visitMaxs(2, 3);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Collection;", false);
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
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/Collection;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$serialize$0", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        genParameterType(methodVisitor, elementType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("t", "Ljava/lang/Object;", null, label0, label1, 0);
        methodVisitor.visitMaxs(3, 1);
        methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

    private static byte[] makeForEnumSet(String generatedClassName, final ParameterType elementType, IScalaPluginClassLoader plugin) {

        final String generatedClassNameUsingDots = generatedClassName;
        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String generatedClassSignature = "L" + generatedClassName + "<TT;>" + ";";

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        RecordComponentVisitor recordComponentVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_FINAL | ACC_SUPER, generatedClassName, "<T:Ljava/lang/Enum<TT;>;>Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection<TT;>;", "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection", null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection.java", null);

        {
        annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
        annotationVisitor0.visit("value", FOR_ENUMSET_ALIAS);
        annotationVisitor0.visitEnd();
        }

        classWriter.visitInnerClass("java/lang/invoke/MethodHandles$Lookup", "java/lang/invoke/MethodHandles", "Lookup", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        {
        fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "value", "Ljava/util/EnumSet;", "Ljava/util/EnumSet<TT;>;", null);
        fieldVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/EnumSet;)V", "(Ljava/util/EnumSet<TT;>;)V", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection", "<init>", "()V", false);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "value", "Ljava/util/EnumSet;");
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitInsn(RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label3, 0);
        methodVisitor.visitLocalVariable("value", "Ljava/util/EnumSet;", "Ljava/util/EnumSet<TT;>;", label0, label3, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()Ljava/util/EnumSet;", "()Ljava/util/EnumSet<TT;>;", null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", "Ljava/util/EnumSet;");
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
        methodVisitor.visitLdcInsn("value");
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "value", "Ljava/util/EnumSet;");
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/EnumSet", "stream", "()Ljava/util/stream/Stream;", false);
        methodVisitor.visitInvokeDynamicInsn("apply", "()Ljava/util/function/Function;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false), new Object[]{Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;"), new Handle(Opcodes.H_INVOKESTATIC, generatedClassName, "lambda$serialize$0", "(Ljava/lang/Enum;)Ljava/lang/Object;", false), Type.getType("(Ljava/lang/Enum;)Ljava/lang/Object;")});
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/stream/Stream", "map", "(Ljava/util/function/Function;)Ljava/util/stream/Stream;", true);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/stream/Collectors", "toList", "()Ljava/util/stream/Collector;", false);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/stream/Stream", "collect", "(Ljava/util/stream/Collector;)Ljava/lang/Object;", true);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label4, 0);
        methodVisitor.visitMaxs(3, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "<T:Ljava/lang/Enum<TT;>;>(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassSignature, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitTypeInsn(NEW, generatedClassName);
        methodVisitor.visitInsn(DUP);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitLdcInsn("value");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "stream", "()Ljava/util/stream/Stream;", true);
        methodVisitor.visitInvokeDynamicInsn("apply", "()Ljava/util/function/Function;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false), new Object[]{Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;"), new Handle(Opcodes.H_INVOKESTATIC, generatedClassName, "lambda$deserialize$1", "(Ljava/lang/Object;)Ljava/lang/Object;", false), Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;")});
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/stream/Stream", "map", "(Ljava/util/function/Function;)Ljava/util/stream/Stream;", true);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/stream/Collectors", "toList", "()Ljava/util/stream/Collector;", false);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/stream/Stream", "collect", "(Ljava/util/stream/Collector;)Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/EnumSet", "copyOf", "(Ljava/util/Collection;)Ljava/util/EnumSet;", false);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(Ljava/util/EnumSet;)V", false);
        methodVisitor.visitInsn(ARETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("map", "Ljava/util/Map;", "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", label0, label4, 0);
        methodVisitor.visitMaxs(4, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/EnumSet;", false);
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
        methodVisitor.visitTypeInsn(INSTANCEOF, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection");
        Label label2 = new Label();
        methodVisitor.visitJumpInsn(IFNE, label2);
        methodVisitor.visitInsn(ICONST_0);
        methodVisitor.visitInsn(IRETURN);
        methodVisitor.visitLabel(label2);
        methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection");
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/EnumSet;", false);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection", "getValue", "()Ljava/lang/Object;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        methodVisitor.visitInsn(IRETURN);
        Label label4 = new Label();
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label4, 0);
        methodVisitor.visitLocalVariable("o", "Ljava/lang/Object;", null, label0, label4, 1);
        methodVisitor.visitLocalVariable("that", "Lxyz/janboerman/scalaloader/configurationserializable/runtime/types/JavaCollection;", null, label3, label4, 2);
        methodVisitor.visitMaxs(2, 3);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/EnumSet;", false);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        { //override Adapter#getValue
        methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()Ljava/util/EnumSet;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", generatedClassDescriptor, generatedClassSignature, label0, label1, 0);
        methodVisitor.visitMaxs(1, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$deserialize$1", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        genParameterType(methodVisitor, elementType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Enum");
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("u", "Ljava/lang/Object;", null, label0, label1, 0);
        methodVisitor.visitMaxs(3, 1);
        methodVisitor.visitEnd();
        }
        {
        methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$serialize$0", "(Ljava/lang/Enum;)Ljava/lang/Object;", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        genParameterType(methodVisitor, elementType, new OperandStack());
        genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
        methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
        methodVisitor.visitInsn(ARETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("t", "Ljava/lang/Enum;", null, label0, label1, 0);
        methodVisitor.visitMaxs(3, 1);
        methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }

}

// code used to generate the above bytecode instructions

/*
@SerializableAs("ALIAS_OF_THE_WRAPPED_CLASS") //in bytecode set to the fully qualified name of the wrapped collection's class
public static final class ForGeneric<T> extends JavaCollection<T> {

    private final Collection<T> value;

    public ForGeneric(Collection<T> value) {
        this.value = value;
    }

    @Override
    public Collection<T> getValue() {
        return value;
    }

    @Override
    public Map<String, Object> serialize() {
        return Collections.singletonMap("value", value.stream()
                .map(t -> RuntimeConversions.serialize(t, (ParameterType) null, (ScalaPluginClassLoader) null))
                .collect(Collectors.toList()));
    }

    public static <T> ForGeneric<T> deserialize(Map<String, Object> map) {
        Collection<T> resColl = new ArrayList<T>(); //in bytecode replace this with the actual collection type
        for (Object serialized : (Collection) map.get("value")) {
            resColl.add((T) RuntimeConversions.deserialize(serialized, (ParameterType) null, (ScalaPluginClassLoader) null));
        }
        return new ForGeneric<>(resColl);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaCollection)) return false;
        JavaCollection that = (JavaCollection) o;
        return Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public String toString() {
        return Objects.toString(getValue());
    }
}
*/

/*
@SerializableAs(FOR_ENUMSET_ALIAS)
public static final class ForEnumSet<T extends Enum<T>> extends JavaCollection<T> {

    private final EnumSet<T> value;

    public ForEnumSet(EnumSet<T> value) {
        this.value = value;
    }

    @Override
    public EnumSet<T> getValue() {
        return value;
    }

    @Override
    public Map<String, Object> serialize() {
        return Collections.singletonMap("value", value.stream()
                .map(t -> RuntimeConversions.serialize(t, (ParameterType) null, (ScalaPluginClassLoader) null))
                .collect(Collectors.toList()));
    }

    public static <T extends Enum<T>> ForEnumSet<T> deserialize(Map<String, Object> map) {
        return new ForEnumSet<>(EnumSet.copyOf((Collection<T>) ((Collection) map.get("value")).stream()
                .map(u -> (T) RuntimeConversions.deserialize(u, (ParameterType) null, (ScalaPluginClassLoader) null))
                .collect(Collectors.toList())));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof JavaCollection)) return false;
        JavaCollection that = (JavaCollection) o;
        return Objects.equals(this.getValue(), that.getValue());
    }

    @Override
    public String toString() {
        return Objects.toString(getValue());
    }
}
*/
