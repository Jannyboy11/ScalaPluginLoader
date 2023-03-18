package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.*;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;

import java.lang.reflect.*;
import java.util.Map;
import java.util.Objects;

public abstract class Option<T> implements ConfigurationSerializable {

    private static final String OPTION = "scala.Option";
    private static final String SOME = "scala.Some";
    private static final String NONE = "scala.None$";

    Option() {}

    public static void registerWithConfigurationSerialization() {
        Some.register();
        None.register();
    }
    
    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> boolean isOption(Object live, ScalaPluginClassLoader classLoader) {
        try {
            Class<?> optionClass = Class.forName(OPTION, false, classLoader);
            return optionClass.isInstance(live);
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }

    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> ConfigurationSerializable serialize(Object scalaOption, ParameterType type, ScalaPluginClassLoader plugin) {
        assert isOption(scalaOption, plugin) : "Not a " + OPTION;

        final RuntimeException ex = new RuntimeException("Could not serialize option: " + scalaOption + ", of type: " + type);

        try {
            Class<?> someClass = Class.forName(SOME, false, plugin);
            if (someClass.isInstance(scalaOption)) {
                Method get = someClass.getMethod("get");
                Object containedValue = get.invoke(scalaOption);
                ParameterType containedValueType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
                Object serializedValue = RuntimeConversions.serialize(containedValue, containedValueType, plugin);
                return new Some(serializedValue);
            }

            Class<?> noneClass = Class.forName(NONE, false, plugin);
            if (noneClass.isInstance(scalaOption)) {
                return None.INSTANCE;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ex.addSuppressed(e);
        }

        throw ex;
    }

    public static boolean isSerializedOption(Object o) {
        return o instanceof Some || o instanceof None;
    }

    public static <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> Object deserialize(Object serializedOption, ParameterType type, ScalaPluginClassLoader plugin) {
        if (serializedOption instanceof Some) {
            Some some = (Some) serializedOption;

            Object serializedValue = some.getValue();
            ParameterType containedValueType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
            Object containedValue = RuntimeConversions.deserialize(serializedValue, containedValueType, plugin);

            try {
                Class<?> someClass = Class.forName(SOME, true, plugin);
                Constructor<?> constructor = someClass.getConstructor(Object.class);
                return constructor.newInstance(containedValue);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("Can't use reflection to return new scala.Some(deserializedValue)", e);
            }
        }

        else if (serializedOption instanceof None) {
            try {
                Class<?> noneClass = Class.forName(NONE, true, plugin);
                Field field = noneClass.getField("MODULE$");
                return field.get(null);
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Can't use reflection to get the scala.None$ singleton instance!", e);
            }
        }

        throw new RuntimeException("Could not deserialize option: " + serializedOption + ", to type: " + type);
    }


    @SerializableAs("Some")
    public static final class Some<T> extends Option<T> {
        private static void register() {
            ConfigurationSerialization.registerClass(Some.class, "Some");
        }

        private final T serializedValue;

        public Some(T value) {
            this.serializedValue = value;
        }

        public T getValue() {
            return serializedValue;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", serializedValue);
        }

        public static <T> Some<T> deserialize(Map<String, Object> map) {
            return new Some<>((T) map.get("value"));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(serializedValue);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Some)) return false;

            Some that = (Some) obj;
            return Objects.equals(this.serializedValue, that.serializedValue);
        }

        @Override
        public String toString() {
            return "Some(" + serializedValue + ")";
        }
    }

    @SerializableAs("None")
    public static final class None extends Option {
        private static void register() {
            ConfigurationSerialization.registerClass(None.class, "None");
        }

        public static final None INSTANCE = new None();

        private None() {}

        @Override
        public Map<String, Object> serialize() {
            return Compat.emptyMap();
        }

        public static None deserialize(Map<String, Object> map) {
            return INSTANCE;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof None;
        }

        @Override
        public String toString() {
            return "None";
        }
    }
}



