package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
import xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions;
import xyz.janboerman.scalaloader.configurationserializable.runtime.types.ScalaCollection.ScalaSeq;
import xyz.janboerman.scalaloader.configurationserializable.runtime.types.ScalaCollection.ScalaSet;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterType;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterizedParameterType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

public abstract class ScalaCollection {

    static final String SCALA_SEQ = "scala.collection.Seq";
    static final String SCALA_SET = "scala.collection.Set";
    static final String SCALA_IMMUTABLE_SEQ = "scala.collection.immutable.Seq";
    static final String SCALA_IMMUTABLE_SET = "scala.collection.immutable.Set";
    static final String SCALA_MUTABLE_SEQ = "scala.collection.mutable.Seq";
    static final String SCALA_MUTABLE_SET = "scala.collection.mutable.Set";
    static final String SCALA_IMMUTABLE_RANGE = "scala.collection.immutable.Range";
    static final String SCALA_IMMUTABLE_WRAPPED_STRING = "scala.collection.immutable.WrappedString";
    static final String SCALA_IMMUTABLE_ARRAY_SEQ = "scala.collection.immutable.ArraySeq";
    static final String SCALA_MUTABLE_ARRAY_SEQ = "scala.collection.mutable.ArraySeq";
    static final String SCALA_IMMUTABLE_LIST = "scala.collection.immutable.List";
    //TODO static final String SCALA_IMMUTABLE_NUMERIC_RANGE = "scala.collection.immutable.NumericRange";    //can't handle this (yet), because we need to obtain the Integral instances first.
    //TODO also need to special-case the 'sorted' collections: TreeSet etc

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

    public static ScalaCollection serialize(Object live, ParameterType type, ClassLoader plugin) {
        assert isCollection(live, plugin) : "Not a " + SCALA_SEQ + " or " + SCALA_SET;

        final ParameterType elementType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
        final Class<?> ourCollectionClass = live.getClass();
        final String alias = ourCollectionClass.getName();
        final String generatedClassName = PREFIX_USING_DOTS + "$ScalaCollection$" + alias;


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
    public abstract static class ScalaSeq extends ScalaCollection implements Adapter  {
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
    public abstract static class ScalaSet extends ScalaCollection implements Adapter {
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
}

//TODO do I still need this?
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

@SerializableAs(ScalaCollection.SCALA_IMMUTABLE_SET + ".SetN") //in bytecode replace N by the set size
/*public*/ final class SetNAdapter extends ScalaSet {
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
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, /*IScalaPlugin ClassLoader*/ null));
            //gen ParameterType and gen ScalaPluginClassLoader
        }

        return Collections.singletonMap("set", serialized);
    }

    public static SetNAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.Set<Object> serialized = (java.util.Set<Object>) map.get("set");

        java.util.Iterator<Object> iterator = serialized.iterator();
        //for (int k = 1; k <= N; k++) {
            RuntimeConversions.deserialize(iterator.next(), (ParameterType) null, /*IScalaPlugin classLoader*/ null);
            //in bytecode, don't pop this, just leave it on the stack.

            //k++
        //}

        return new SetNAdapter(new scala.collection.immutable.Set.Set1(null)); //SetN(elem1, ..., elemN)
    }
}