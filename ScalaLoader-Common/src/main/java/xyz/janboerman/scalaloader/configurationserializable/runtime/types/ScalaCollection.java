package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;

import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.bytecode.LocalCounter;
import xyz.janboerman.scalaloader.bytecode.LocalVariable;
import xyz.janboerman.scalaloader.bytecode.LocalVariableTable;
import xyz.janboerman.scalaloader.bytecode.OperandStack;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
import xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.ScalaCollection.ScalaSet;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.ScalaCollection.ScalaSeq;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterType;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterizedParameterType;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.stream.IntStream;

@SuppressWarnings("rawtypes")
public abstract class ScalaCollection {

    static final String SCALA_SEQ = "scala.collection.Seq";
    static final String SCALA_SET = "scala.collection.Set";
    static final String SCALA_IMMUTABLE_SEQ = "scala.collection.immutable.Seq";
    static final String SCALA_IMMUTABLE_SET = "scala.collection.immutable.Set";
    static final String SCALA_MUTABLE_SEQ = "scala.collection.mutable.Seq";
    static final String SCALA_MUTABLE_SET = "scala.collection.mutable.Set"; //TODO do we also have mutable SetN variants? that does not make sense, right?
    static final String SCALA_IMMUTABLE_RANGE = "scala.collection.immutable.Range";
    static final String SCALA_IMMUTABLE_WRAPPED_STRING = "scala.collection.immutable.WrappedString";
    static final String SCALA_IMMUTABLE_ARRAY_SEQ = "scala.collection.immutable.ArraySeq";
    static final String SCALA_MUTABLE_ARRAY_SEQ = "scala.collection.mutable.ArraySeq";
    static final String SCALA_IMMUTABLE_LIST = "scala.collection.immutable.List";
    //TODO static final String SCALA_IMMUTABLE_NUMERIC_RANGE = "scala.collection.immutable.NumericRange";    //can't handle this (yet), because we need to obtain the Integral instances first.
    //TODO but we have Explicit now. does this help us?
    //TODO also need to special-case the 'sorted' collections: TreeSet etc

    static final String SCALA_SEQ_WRAPPER_WITH_SLASHES = ScalaSeq.class.getName().replace('.', '/');
    static final String SCALA_SET_WRAPPER_WITH_SLASHES = ScalaSet.class.getName().replace('.', '/');

    public ScalaCollection() {}

    private static boolean isSetN(String setClassName, int N) {
        return ("scala.collection.immutable.Set$Set" + N).equals(setClassName);
    }

    private static boolean is(Object live, String className, ClassLoader pluginClassLoader) {
        try {
            return Class.forName(className, false, pluginClassLoader).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isWrappedString(Object live, ClassLoader plugin) {
        return is(live, SCALA_IMMUTABLE_WRAPPED_STRING, plugin);
    }

    private static boolean isRange(Object live, ClassLoader plugin) {
        return is(live, SCALA_IMMUTABLE_RANGE, plugin);
    }

    private static boolean isImmutableArraySeq(Object live, ClassLoader plugin) {
        return is(live, SCALA_IMMUTABLE_ARRAY_SEQ, plugin);
    }

    private static boolean isMutableArraySeq(Object live, ClassLoader plugin) {
        return is(live, SCALA_MUTABLE_ARRAY_SEQ, plugin);
    }

    private static boolean isList(Object live, ClassLoader plugin) {
        return is(live, SCALA_IMMUTABLE_LIST, plugin);
    }

    private static Class<?> getScalaSeqClass(ClassLoader plugin) throws ClassNotFoundException {
        return Class.forName(SCALA_SEQ, false, plugin);
    }

    private static Class<?> getScalaSetClass(ClassLoader plugin) throws ClassNotFoundException {
        return Class.forName(SCALA_SET, false, plugin);
    }

    private static boolean isSeq(Object live, ClassLoader plugin) {
        try {
            return getScalaSeqClass(plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isSet(Object live, ClassLoader plugin) {
        try {
            return getScalaSetClass(plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isCollection(Object live, ClassLoader plugin) {
        return isSeq(live, plugin) || isSet(live, plugin);
    }

    @SuppressWarnings("unchecked")
    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> ScalaCollection instantiateWrapper(
            Object live, ParameterType liveType, String generatedClassName, ClassGenerator generator, ScalaPluginClassLoader plugin) {
        Class<?> liveClass = live.getClass();

        ClassDefineResult result = plugin.getOrDefineClass(generatedClassName, generator, true);
        Class<? extends ScalaCollection> wrapperClass = (Class<? extends ScalaCollection>)result.getClassDefinition();
        if (result.isNew()) {
            ConfigurationSerialization.registerClass((Class<? extends ConfigurationSerializable>) wrapperClass, liveClass.getName());
        }

        try {
            Constructor<? extends ScalaCollection> constructor = wrapperClass.getConstructor(liveClass);
            return constructor.newInstance(live);
        } catch (Exception shouldNotOccur) {
            throw new RuntimeException("Could not serialize scala collection: " + live + ", of type: " + liveType, shouldNotOccur);
        }
    }

    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> ScalaCollection serialize(
            Object live, ParameterType type, ScalaPluginClassLoader plugin) {
        assert isCollection(live, plugin) : "Not a " + SCALA_SEQ + " or " + SCALA_SET;

        final ParameterType elementType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
        final Class<?> ourCollectionClass = live.getClass();
        final String alias = ourCollectionClass.getName();
        final String generatedClassName = PREFIX_USING_DOTS + "$ScalaCollection$" + alias;

        final OptionalInt isSetN = IntStream.rangeClosed(1, 4).filter(N -> isSetN(alias, N)).findAny();
        if (isSetN.isPresent()) {
            //TODO test this:
            return instantiateWrapper(live, type, generatedClassName, name -> makeSetN(isSetN.getAsInt(), generatedClassName, ourCollectionClass, alias, elementType, plugin), plugin);
        }

        //TODO
        /*
         if (live instanceof SomeSortedSetImplementation) {
            return new SortedSetAdapter((SortedSet) live);
         } else if (live instanceof Set) {
            return new GenericSetAdapter((Set) live);
         } else if (live instanceof Range) {
            return new RangeAdapter((Range) live));
         } else if (live instanceof NumericRange) {
            return new NumericRangeRangeAdapter((NumericRange) live);
         } else if (live instanceof WrappedString) {
            return new WrappedStringAdapter((WrappedString) live);
         } else if (live instanceof ArraySeq) {
            return new ArraySeqAdapter((ArraySeq) live);
         } else if (live instanceof Seq) {
            return new GenericSeqAdapter((Seq) live);
         }


         */
        //TODO do I need to take mutable/immutable into account?
        //TODO I DO need to take Set1-Set4 into account, as well as empty Seq (Nil).
        //TODO $colon$colon (::) does not have a companion object! use the scala.collection.immutable.List$ companion object!
        /*TODO some REPL experimenting gave me this knowledge:
        scala> val clazz: java.lang.Class[_] = Seq().getClass()
        val clazz: Class[_] = class scala.collection.immutable.Nil$

        scala> val clazz: java.lang.Class[_] = Seq(0).getClass()
        val clazz: Class[_] = class scala.collection.immutable.$colon$colon

        scala> val clazz: java.lang.Class[_] = Seq(0, 1).getClass()
        val clazz: Class[_] = class scala.collection.immutable.$colon$colon

        scala> val clazz: java.lang.Class[_] = Seq(0, 2).getClass()
        val clazz: Class[_] = class scala.collection.immutable.$colon$colon

        scala> val clazz: java.lang.Class[_] = Set().getClass()
        val clazz: Class[_] = class scala.collection.immutable.Set$EmptySet$

        scala> val clazz: java.lang.Class[_] = Set(0).getClass()
        val clazz: Class[_] = class scala.collection.immutable.Set$Set1

        scala> val clazz: java.lang.Class[_] = Set(0, 1).getClass()
        val clazz: Class[_] = class scala.collection.immutable.Set$Set2

        scala> val clazz: java.lang.Class[_] = Set(0, 1, 2).getClass()
        val clazz: Class[_] = class scala.collection.immutable.Set$Set3

        scala> val clazz: java.lang.Class[_] = Set(0, 1, 2, 4).getClass()
        val clazz: Class[_] = class scala.collection.immutable.Set$Set4

        scala> val clazz: java.lang.Class[_] = Set(0, 1, 2, 3, 4).getClass()
        val clazz: Class[_] = class scala.collection.immutable.HashSet
        */

        throw new RuntimeException("Could not serialize scala collection: " + live + ", of type: " + type);
    }

    @Called
    public abstract static class ScalaSeq extends ScalaCollection implements Adapter/*<scala.collection.Seq>*/ {
        @Called
        public ScalaSeq() {}

        @Override
        public int hashCode() {
            return Objects.hashCode(getValue());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ScalaSeq)) return false;
            ScalaSeq that = (ScalaSeq) o;
            return Objects.equals(this.getValue(), that.getValue());
        }

        @Override
        public String toString() {
            return Objects.toString(getValue());
        }
    }

    @Called
    public abstract static class ScalaSet extends ScalaCollection implements  Adapter/*<scala.collection.Set>*/ {
        @Called
        public ScalaSet() {}

        @Override
        public int hashCode() {
            return Objects.hashCode(getValue());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ScalaSet)) return false;
            ScalaSet that = (ScalaSet) o;
            return Objects.equals(this.getValue(), that.getValue());
        }

        @Override
        public String toString() {
            return Objects.toString(getValue());
        }
    }

    private static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] makeSetN(
            final int N, String generatedClassName, final Class<?> theSetType, final String alias,
            final ParameterType elementType, final ScalaPluginClassLoader plugin) {

        generatedClassName = generatedClassName.replace('.', '/');
        final String generatedClassDescriptor = "L" + generatedClassName + ";";
        final String setNClassName = theSetType.getName().replace('.', '/');
        final String setNClassDescriptor = "L" + setNClassName + ":";

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        // @SerialiableAs("scala.collection.Set$SetN");
        // public class SetN extends ScalaCollection.ScalaSet { ... }
        classWriter.visit(V1_8, ACC_FINAL | ACC_SUPER, generatedClassName, null, SCALA_SET_WRAPPER_WITH_SLASHES, null);

        classWriter.visitSource("xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaCollection.java", null);

        {
            annotationVisitor0 = classWriter.visitAnnotation("Lorg/bukkit/configuration/serialization/SerializableAs;", true);
            annotationVisitor0.visit("value", alias);
            annotationVisitor0.visitEnd();
        }
        classWriter.visitInnerClass("xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaCollection$ScalaSet", "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaCollection", "ScalaSet", ACC_PUBLIC | ACC_STATIC | ACC_ABSTRACT);

        classWriter.visitInnerClass(setNClassName, "scala/collection/immutable/Set", "Set" + N, ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        {   // private final scala.collection.Set$SetN set;
            fieldVisitor = classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "set", setNClassDescriptor, null, null);
            fieldVisitor.visitEnd();
        }
        {   // public SetN(scala.collection.Set$SetN set) { this.set = set }
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "(" + setNClassDescriptor + ")V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "xyz/janboerman/scalaloader/configurationserializable/runtime/types/ScalaCollection$ScalaSet", "<init>", "()V", false);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitFieldInsn(PUTFIELD, generatedClassName, "set", setNClassDescriptor);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitInsn(RETURN);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label3, 0);
            methodVisitor.visitLocalVariable("set", setNClassDescriptor, null, label0, label3, 1);
            methodVisitor.visitMaxs(2, 2);
            methodVisitor.visitEnd();
        }
        {   // @Override public scala.collection.Set$SetN getValue() { return this.set; }
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "getValue", "()" + setNClassDescriptor, null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "set", setNClassDescriptor);
            methodVisitor.visitInsn(ARETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {   // @Override public Map<String, Object> serialize() { ... }
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "serialize", "()Ljava/util/Map;", "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);

            // java.util.Set serialized = new java.util.LinkedHashSet();
            methodVisitor.visitTypeInsn(NEW, "java/util/LinkedHashSet");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashSet", "<init>", "()V", false);
            methodVisitor.visitVarInsn(ASTORE, 1);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);

            // scala.collection.Iterator = this.set.iterator();
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitFieldInsn(GETFIELD, generatedClassName, "set", setNClassDescriptor);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, setNClassName, "iterator", "()Lscala/collection/Iterator;", false);
            methodVisitor.visitVarInsn(ASTORE, 2);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);

            // while (iterator.hasNext()) {
            methodVisitor.visitFrame(Opcodes.F_APPEND,2, new Object[] {"java/util/Set", "scala/collection/Iterator"}, 0, null);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "hasNext", "()Z", true);
            Label label3 = new Label();
            methodVisitor.visitJumpInsn(IFEQ, label3);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);

            // serialized.add(RuntimeConversions.serialize(iterator.next()));
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitVarInsn(ALOAD, 2);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "scala/collection/Iterator", "next", "()Ljava/lang/Object;", true);
            genParameterType(methodVisitor, elementType, new OperandStack());
            genScalaPluginClassLoader(methodVisitor, plugin, new OperandStack());
            methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "serialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
            methodVisitor.visitInsn(POP);
            methodVisitor.visitJumpInsn(GOTO, label2);

            // } // end while
            methodVisitor.visitLabel(label3);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

            // return Collections.singletonMap("set", serialized);
            methodVisitor.visitLdcInsn("set");
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonMap", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;", false);
            methodVisitor.visitInsn(ARETURN);
            Label label5 = new Label();
            methodVisitor.visitLabel(label5);

            methodVisitor.visitLocalVariable("this", generatedClassDescriptor, null, label0, label5, 0);
            methodVisitor.visitLocalVariable("serialized", "Ljava/util/Set;", "Ljava/util/Set<Ljava/lang/Object;>;", label1, label5, 1);
            methodVisitor.visitLocalVariable("iterator", "Lscala/collection/Iterator;", null, label2, label5, 2);
            methodVisitor.visitMaxs(4, 3);
            methodVisitor.visitEnd();
        }
        {   // public static SetN deserialize(Map<String, Object> map) { ... }

            // set up book keeping
            final OperandStack operandStack = new OperandStack();
            final LocalVariableTable localVariableTable = new LocalVariableTable();
            final LocalCounter localCounter = new LocalCounter();

            final Label startLabel = new Label(), endLabel = new Label();

            final int argumentSetIndex = localCounter.getSlotIndex(), argumentSetFrameIndex = localCounter.getFrameIndex();

            final LocalVariable argumentSetVariable = new LocalVariable("set", "Ljava/util/Set;", null, startLabel, endLabel, argumentSetIndex, argumentSetFrameIndex);
            localVariableTable.add(argumentSetVariable); localCounter.add(Type.getType(java.util.Set.class));

            // write the bytecode!
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "deserialize", "(Ljava/util/Map;)" + generatedClassDescriptor, "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)" + generatedClassDescriptor, null);
            methodVisitor.visitCode();

            methodVisitor.visitLabel(startLabel);   //let's a go!

            // java.util.Set serialized = (java.util.Set) map.get("set");
            methodVisitor.visitVarInsn(ALOAD, argumentSetIndex);                        operandStack.push(Type.getType(java.util.Set.class));
            methodVisitor.visitLdcInsn("set");                                     operandStack.push(Type.getType(java.lang.String.class));
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);     operandStack.replaceTop(2, Type.getType(Object.class));
            methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Set");                operandStack.push(Type.getType(java.util.Set.class));
            final int serializedSetIndex = localCounter.getSlotIndex(), serializedSetFrameIndex = localCounter.getFrameIndex();
            methodVisitor.visitVarInsn(ASTORE, serializedSetIndex);                     operandStack.pop();
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            final LocalVariable serializedSetVariable = new LocalVariable("serialized", "Ljava/util/Set;", null, label1, endLabel, serializedSetIndex, serializedSetFrameIndex);
            localVariableTable.add(serializedSetVariable); localCounter.add(Type.getType(java.util.Set.class));

            // java.util.Iterator<java.lang.Object> iterator = serialized.iterator();
            methodVisitor.visitVarInsn(ALOAD, serializedSetIndex);                      operandStack.push(Type.getType(java.util.Set.class));
            methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;", true);    operandStack.replaceTop(Type.getType(java.util.Iterator.class));
            final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
            methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                          operandStack.pop();
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            final LocalVariable iteratorVariable = new LocalVariable("iterator", "Ljava/util/Iterator;", null, label2, endLabel, iteratorIndex, iteratorFrameIndex);
            localVariableTable.add(iteratorVariable); localCounter.add(Type.getType(java.util.Iterator.class));

            // prepare for return; new scalaLoader.SetN(new scala.SetN(...
            methodVisitor.visitTypeInsn(NEW, generatedClassName);                           operandStack.push(Type.getType(generatedClassDescriptor));
            methodVisitor.visitInsn(DUP);                                                   operandStack.push(Type.getType(generatedClassDescriptor));
            methodVisitor.visitTypeInsn(NEW, setNClassName);                                operandStack.push(Type.getType(setNClassDescriptor));
            methodVisitor.visitTypeInsn(NEW, setNClassName);                                operandStack.push(Type.getType(setNClassDescriptor));

            // prepare arguments
            for (int k = 1; k <= N; k++) {
                // just call iterator.next() unsafely because we know how many elements there are!

                // java.lang.Object elementK = RuntimeConversions.deserialize(iterator.next());
                methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                           operandStack.push(Type.getType(java.util.Iterator.class));
                methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);     operandStack.replaceTop(Type.getType(Object.class));
                genParameterType(methodVisitor, elementType, operandStack);
                genScalaPluginClassLoader(methodVisitor, plugin, operandStack);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "xyz/janboerman/scalaloader/configurationserializable/runtime/RuntimeConversions", "deserialize", "(Ljava/lang/Object;Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Ljava/lang/ClassLoader;)Ljava/lang/Object;", false);
                /* just leave it on top of the stack! */                                    operandStack.replaceTop(3, Type.getType(Object.class));
            }

            // now, invoke the constructors scala.SetN#<init>, scalaLoader.setN#<init>
            methodVisitor.visitMethodInsn(INVOKESPECIAL, setNClassName, "<init>", "(" + Compat.stringRepeat("Ljava/lang/Object;", N) + ")V", false);    operandStack.pop(N + 1);    // arguments + the type itself
            methodVisitor.visitMethodInsn(INVOKESPECIAL, generatedClassName, "<init>", "(" + setNClassDescriptor + ")V", false);                                operandStack.pop(2);
            methodVisitor.visitInsn(ARETURN);

            methodVisitor.visitLabel(endLabel);

            for (LocalVariable local : localVariableTable) {
                methodVisitor.visitLocalVariable(local.name, local.descriptor, local.signature, local.startLabel, local.endLabel, local.tableSlot);
            }
            methodVisitor.visitMaxs(operandStack.maxStack(), localVariableTable.maxLocals());
            methodVisitor.visitEnd();
        }
        {   // @Override public Object getValue() { return this.set; } // return type is Object because of type erasure :)
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC, "getValue", "()Ljava/lang/Object;", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, generatedClassName, "getValue", "()" + setNClassDescriptor, false);
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

//TODO do I still need this?
//TODO I think we want to generate this code, amiright?
@SerializableAs("THE_SEQ_IMPLEMENTATION_CLASS_NAME")
/*public*/ final class SeqAdapter extends ScalaSeq {
    private final scala.collection.Seq seq;

    public SeqAdapter(scala.collection.Seq seq) {
        super();
        this.seq = seq;
    }

    @Override
    public scala.collection.Seq getValue() {
        return seq;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.List<Object> serialized;
        if (seq instanceof scala.collection.IndexedSeq) {
            serialized = new java.util.ArrayList<>(seq.length());
        } else {
            serialized = new java.util.ArrayList<>();
        }

        scala.collection.Iterator iterator = seq.iterator();
        while (iterator.hasNext()) {
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, (ClassLoader & IScalaPluginClassLoader) null));
            //in bytecode we generate the ParameterType and ScalaPluginLoder using the genXXX methods.
        }

        return Collections.singletonMap("seq", serialized);
    }

    public static SeqAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.List<Object> serialized = (java.util.List<Object>) map.get("seq");

        scala.collection.mutable.Builder builder = scala.collection.immutable.Seq$.MODULE$.newBuilder();
        //in bytecode use the companion object of the actual seq type to get the builder

        for (Object element : serialized) {
            builder.addOne(RuntimeConversions.deserialize(element, (ParameterType) null, (ClassLoader & IScalaPluginClassLoader) null));
            //idem
        }

        return new SeqAdapter((scala.collection.Seq) builder.result());
    }
}

@SerializableAs("WRAPPED_STRING_CLASS_NAME")
/*public*/ final class WrappedStringAdapter extends ScalaSeq {
    private final scala.collection.immutable.WrappedString wrappedString;

    public WrappedStringAdapter(scala.collection.immutable.WrappedString wrappedString) {
        super();
        this.wrappedString = wrappedString;
    }

    @Override
    public scala.collection.immutable.WrappedString getValue() {
        return wrappedString;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        //return Collections.singletonMap("string", scala.collection.immutable.WrappedString$.MODULE$.UnwrapOp(wrappedString).unwrap());
        return Collections.singletonMap("string", wrappedString.toString());
    }

    public static WrappedStringAdapter deserialize(java.util.Map<String, Object> map) {
        return new WrappedStringAdapter(new scala.collection.immutable.WrappedString((String) map.get("string")));
    }
}

@SerializableAs("RANGE_CLASS_NAME")
/*public*/ final class RangeAdapter extends ScalaSeq {
    private final scala.collection.immutable.Range range;

    public RangeAdapter(scala.collection.immutable.Range range) {
        super();
        this.range = range;
    }

    @Override
    public scala.collection.immutable.Range getValue() {
        return range;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        return Collections.singletonMap("range", new NumericRange.OfInteger(range.start(), range.step(), range.end(), range.isInclusive()));
    }

    public static RangeAdapter deserialize(java.util.Map<String, Object> map) {
        NumericRange.OfInteger serialized = (NumericRange.OfInteger) map.get("range");
        scala.collection.immutable.Range range;
        if (serialized.isInclusive()) {
            range = scala.collection.immutable.Range$.MODULE$.inclusive(serialized.start(), serialized.end(), serialized.step());
        } else {
            range = scala.collection.immutable.Range$.MODULE$.apply(serialized.start(), serialized.end(), serialized.step());
        }
        return new RangeAdapter(range);
    }
}

//TODO NumericRange, but requires us to know the Integral instances.... (same problem as sorted sets)

@SerializableAs("IMMUTABLE_ARRAY_SEQ_CLASS_NAME")
/*public*/ final class ImmutableArraySeqAdapter extends ScalaSeq {
    private final scala.collection.immutable.ArraySeq arraySeq;

    public ImmutableArraySeqAdapter(scala.collection.immutable.ArraySeq arraySeq) {
        super();
        this.arraySeq = arraySeq;
    }

    @Override
    public scala.collection.immutable.ArraySeq getValue() {
        return arraySeq;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.List<Object> serialized = new java.util.ArrayList<>(arraySeq.length());
        for (int i = 0; i < arraySeq.length(); ++i) {
            serialized.add(RuntimeConversions.serialize(arraySeq.apply(i), (ParameterType) null, /*ScalaPluginClassLoader*/ null));
        }
        Class<?> elemClazz = arraySeq.elemTag().runtimeClass();

        java.util.Map<String, Object> result = new LinkedHashMap<>();
        result.put("tag", elemClazz.getName());
        result.put("array", serialized);
        return result;
    }

    public static ImmutableArraySeqAdapter deserialize(java.util.Map<String, Object> map) throws ClassNotFoundException {
        scala.collection.mutable.Builder builder = scala.collection.immutable.ArraySeq$.MODULE$.newBuilder(scala.reflect.ClassTag$.MODULE$.apply(Class.forName((String) map.get("tag"), false, (ClassLoader & IScalaPluginLoader) null)));
        //in bytecode generate the ScalaPluginClassLoader instance
        for (Object serializedElement : (java.util.List<Object>) map.get("array")) {
            builder.addOne(RuntimeConversions.deserialize(serializedElement, (ParameterType) null, /*ScalaPluginClassLoader*/ null));
        }
        return new ImmutableArraySeqAdapter((scala.collection.immutable.ArraySeq) builder.result());
    }
}

@SerializableAs("MUTABLE_ARRAY_SEQ_CLASS_NAME")
/*public*/ final class MutableArraySeqAdapter extends ScalaSeq {
    private final scala.collection.mutable.ArraySeq arraySeq;

    public MutableArraySeqAdapter(scala.collection.mutable.ArraySeq arraySeq) {
        super();
        this.arraySeq = arraySeq;
    }

    @Override
    public scala.collection.mutable.ArraySeq getValue() {
        return arraySeq;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.List<Object> serialized = new java.util.ArrayList<>(arraySeq.length());
        for (int i = 0; i < arraySeq.length(); ++i) {
            serialized.add(RuntimeConversions.serialize(arraySeq.apply(i), (ParameterType) null, /*ClassLoader*/ null));
        }
        Class<?> elemClazz = arraySeq.elemTag().runtimeClass();

        java.util.Map<String, Object> result = new LinkedHashMap<>();
        result.put("tag", elemClazz.getName());
        result.put("array", serialized);
        return result;
    }

    public static MutableArraySeqAdapter deserialize(java.util.Map<String, Object> map) throws ClassNotFoundException {
        scala.collection.mutable.Builder builder = scala.collection.mutable.ArraySeq$.MODULE$.newBuilder(scala.reflect.ClassTag$.MODULE$.apply(Class.forName((String) map.get("tag"), false, (ClassLoader & IScalaPluginClassLoader) null)));
        //in bytecode generate the ScalaPluginClassLoader instance
        for (Object serializedElement : (java.util.List<Object>) map.get("array")) {
            builder.addOne(RuntimeConversions.deserialize(serializedElement, (ParameterType) null, /*ClassLoader*/ null));
        }
        return new MutableArraySeqAdapter((scala.collection.mutable.ArraySeq) builder.result());
    }
}

@SerializableAs("LIST_CLASS_NAME")
/*public*/ final class ListAdapter extends ScalaSeq {
    private final scala.collection.immutable.List list;

    public ListAdapter(scala.collection.immutable.List list) {
        super();
        this.list = list;
    }

    @Override
    public scala.collection.immutable.List getValue() {
        return list;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.List<Object> serialized = new java.util.ArrayList<>();
        for (scala.collection.Iterator iterator = list.iterator(); iterator.hasNext(); ) {
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, /*IScalaPlugin ClassLoader*/ null));
        }
        return Collections.singletonMap("list", serialized);
    }

    public static ListAdapter deserialize(java.util.Map<String, Object> map) {
        scala.collection.mutable.Builder builder = scala.collection.immutable.List$.MODULE$.newBuilder();
        for (Object serializedElement : (java.util.List<Object>) map.get("list")) {
            builder.addOne(RuntimeConversions.deserialize(serializedElement, (ParameterType) null, /*IScalaPlugin ClassLoader*/ null));
        }
        return new ListAdapter((scala.collection.immutable.List) builder.result());
    }
}

@SerializableAs("THE_SET_IMPLEMENTATION_CLASS_NAME")
/*public*/ final class SetAdapter extends ScalaSet {
    private final scala.collection.Set set;

    public SetAdapter(scala.collection.Set set) {
        super();
        this.set = set;
    }

    @Override
    public scala.collection.Set getValue() {
        return set;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Set<Object> serialized = new java.util.LinkedHashSet<>();

        scala.collection.Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, /*IScalaPlugin ClassLoader*/ null));
            //use genXXX methods to get the ParameterType and ScalaPluginClassLoader in the bytecode
        }

        return Collections.singletonMap("set", serialized);
    }

    public static SetAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.Set<Object> serialized = (java.util.Set<Object>) map.get("set");

        scala.collection.mutable.Builder builder = scala.collection.immutable.Set$.MODULE$.newBuilder();
        //in bytecode use the companion object of the actual set type to get the builder

        for (Object element : serialized) {
            builder.addOne(RuntimeConversions.deserialize(element, (ParameterType) null, /*IScalaPlugin ClassLoader*/ null));
            //idem for ParameterType and ScalaPluginClassLoader
        }

        return new SetAdapter((scala.collection.Set) builder.result());
    }
}

/*
@SerializableAs(ScalaCollection.SCALA_IMMUTABLE_SET + ".SetN") //in bytecode replace N by the set size
public final class SetNAdapter extends ScalaSet {
    private final scala.collection.immutable.Set.Set1 set;  //in bytecode make this dependent on the set size

    public SetNAdapter(scala.collection.immutable.Set.Set1 set) { //SetN
        this.set = set;
    }

    @Override
    public scala.collection.immutable.Set.Set1 getValue() { //SetN
        return set;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Set<Object> serialized = new java.util.LinkedHashSet<>();

        scala.collection.Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, (ScalaPluginClassLoader) null));
            //gen ParameterType and gen ScalaPluginClassLoader
        }

        return Collections.singletonMap("set", serialized);
    }

    public static SetNAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.Set<Object> serialized = (java.util.Set<Object>) map.get("set");

        java.util.Iterator<Object> iterator = serialized.iterator();
        //for (int k = 1; k <= N; k++) {
            RuntimeConversions.deserialize(iterator.next(), (ParameterType) null, (ScalaPluginClassLoader) null);
            //in bytecode, don't pop this, just leave it on the stack.

            //k++
        //}

        return new SetNAdapter(new scala.collection.immutable.Set.Set1(null)); //SetN(elem1, ..., elemN)
    }
}
*/
