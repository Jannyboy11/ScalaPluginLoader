package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions;
import xyz.janboerman.scalaloader.configurationserializable.runtime.types.ScalaCollection.ScalaSeq;
import xyz.janboerman.scalaloader.configurationserializable.runtime.types.ScalaCollection.ScalaSet;
import static xyz.janboerman.scalaloader.configurationserializable.runtime.types.Types.*;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterType;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterizedParameterType;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.util.Collections;
import java.util.Objects;

public abstract class ScalaCollection implements Adapter/*<scala.collection.Iterable>*/ {

    //TODO special cases needed for a few collections: Range, NumericRange, WrappedString, ArraySeq (because it needs ClassTag)
    //TODO also need to special-case the 'sorted' collections: TreeSet etc

    static final String SCALA_SEQ = "scala.collection.Seq";
    static final String SCALA_SET = "scala.collection.Set";
    static final String SCALA_IMMUTABLE_SEQ = "scala.collection.immutable.Seq";
    static final String SCALA_IMMUTABLE_SET = "scala.collection.immutable.Set";
    static final String SCALA_MUTABLE_SEQ = "scala.collection.mutable.Seq";
    static final String SCALA_MUTABLE_SET = "scala.collection.mutable.Set";

    public ScalaCollection() {}

    private static Class<?> getScalaSeqClass(ScalaPluginClassLoader plugin) throws ClassNotFoundException {
        return Class.forName(SCALA_SEQ, false, plugin);
    }

    private static Class<?> getScalaSetClass(ScalaPluginClassLoader plugin) throws ClassNotFoundException {
        return Class.forName(SCALA_SET, false, plugin);
    }

    private static boolean isSeq(Object live, ScalaPluginClassLoader plugin) {
        try {
            return getScalaSeqClass(plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isSet(Object live, ScalaPluginClassLoader plugin) {
        try {
            return getScalaSetClass(plugin).isInstance(live);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isCollection(Object live, ScalaPluginClassLoader plugin) {
        return isSeq(live, plugin) || isSet(live, plugin);
    }

    public static ScalaCollection serialize(Object live, ParameterType type, ScalaPluginClassLoader plugin) {
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

        throw new RuntimeException("Could not serialize scala collection: " + live + ", of type: " + type);
    }

    @Called
    public abstract static class ScalaSeq extends ScalaCollection {
        @Called
        public ScalaSeq() {}
    }

    @Called
    public abstract static class ScalaSet extends ScalaCollection {
        @Called
        public ScalaSet() {}
    }
}

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
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, (ScalaPluginClassLoader) null));
            //in bytecode we generate the ParameterType and ScalaPluginLoder using the genXXX methods.
        }

        return Collections.singletonMap("seq", serialized);
    }

    public static SeqAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.List<Object> serialized = (java.util.List<Object>) map.get("seq");

        scala.collection.mutable.Builder builder = scala.collection.immutable.Seq$.MODULE$.newBuilder();
        //in bytecode use the companion object of the actual seq type to get the builder

        for (Object element : serialized) {
            builder.addOne(RuntimeConversions.deserialize(element, (ParameterType) null, (ScalaPluginClassLoader) null));
            //idem
        }

        return new SeqAdapter((scala.collection.Seq) builder.result());
    }

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
            serialized.add(RuntimeConversions.serialize(iterator.next(), (ParameterType) null, (ScalaPluginClassLoader) null));
            //use genXXX methods to get the ParameterType and ScalaPluginClassLoader in the bytecode
        }

        return Collections.singletonMap("set", serialized);
    }

    public static SetAdapter deserialize(java.util.Map<String, Object> map) {
        java.util.Set<Object> serialized = (java.util.Set<Object>) map.get("set");

        scala.collection.mutable.Builder builder = scala.collection.immutable.Set$.MODULE$.newBuilder();
        //in bytecode use the companion object of the actual set type to get the builder

        for (Object element : serialized) {
            builder.addOne(RuntimeConversions.deserialize(element, (ParameterType) null, (ScalaPluginClassLoader) null));
            //idem for ParameterType and ScalaPluginClassLoader
        }

        return new SetAdapter((scala.collection.Set) builder.result());
    }

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