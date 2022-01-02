package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.*;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.lang.reflect.*;
import java.util.Map;
import java.util.Objects;

public abstract class Either<L, R> implements ConfigurationSerializable {

    private static final String EITHER = "scala.util.Either";
    private static final String LEFT = "scala.util.Left";
    private static final String RIGHT = "scala.util.Right";

    Either() {}

    public static void registerWithConfigurationSerialization() {
        Left.register();
        Right.register();
    }

    public static boolean isEither(Object live, ScalaPluginClassLoader plugin) {
        try {
            Class<?> eitherClazz = Class.forName(EITHER, false, plugin);
            return eitherClazz.isInstance(live);
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }

    public static ConfigurationSerializable serialize(Object scalaEither, ParameterType type, ScalaPluginClassLoader plugin) {
        assert isEither(scalaEither, plugin) : "Not a " + EITHER;

        final RuntimeException ex = new RuntimeException("Could not serialize either: " + scalaEither + ", of type: " + type);

        try {
            Class<?> leftClass = Class.forName(LEFT, false, plugin);
            if (leftClass.isInstance(scalaEither)) {
                Method method = leftClass.getMethod("value");
                Object liveValue = method.invoke(scalaEither);
                ParameterType elementType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
                Object serializedValue = RuntimeConversions.serialize(liveValue, elementType, plugin);
                return new Left(serializedValue);
            }

            Class<?> rightClass = Class.forName(RIGHT, false, plugin);
            if (rightClass.isInstance(scalaEither)) {
                Method method = rightClass.getMethod("value");
                Object liveValue = method.invoke(scalaEither);
                ParameterType elementType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(1) : ParameterType.from(Object.class);
                Object serializedValue = RuntimeConversions.serialize(liveValue, elementType, plugin);
                return new Right(serializedValue);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ex.addSuppressed(e);
        }

        throw ex;
    }

    public static boolean isSerializedEither(Object o) {
        return o instanceof Left || o instanceof Right;
    }

    public static Object deserialize(Object serializedEither, ParameterType type, ScalaPluginClassLoader plugin) {
        if (serializedEither instanceof Left) {
            Left left = (Left) serializedEither;

            Object serializedValue = left.getValue();
            ParameterType containedValueType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(0) : ParameterType.from(Object.class);
            Object containedValue = RuntimeConversions.deserialize(serializedValue, containedValueType, plugin);

            try {
                Class<?> leftClass = Class.forName(LEFT, true, plugin);
                Constructor<?> constructor = leftClass.getConstructor(Object.class);
                return constructor.newInstance(containedValue);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("Can't use reflection to return new scala.util.Left(deserializedValue)", e);
            }
        }

        else if (serializedEither instanceof Right) {
            Right right = (Right) serializedEither;

            Object serializedValue = right.getValue();
            ParameterType containedValueType = type instanceof ParameterizedParameterType ? ((ParameterizedParameterType) type).getTypeParameter(1) : ParameterType.from(Object.class);
            Object containedValue = RuntimeConversions.deserialize(serializedValue, containedValueType, plugin);

            try {
                Class<?> rightClass = Class.forName(RIGHT, true, plugin);
                Constructor<?> constructor = rightClass.getConstructor(Object.class);
                return constructor.newInstance(containedValue);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                throw new RuntimeException("Can't use reflfection to return new scala.util.Right(deserializedValue)", e);
            }
        }

        throw new RuntimeException("Could not deserialize either: " + serializedEither + ", to type: " + type);
    }


    @SerializableAs("Left")
    public static final class Left<L, R> extends Either<L, R> {
        private static void register() {
            ConfigurationSerialization.registerClass(Left.class, "Left");
        }

        private final L serializedValue;

        public Left(L serializedValue) {
            this.serializedValue = serializedValue;
        }

        public L getValue() {
            return serializedValue;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", serializedValue);
        }

        public static <L, R> Left<L, R> deserialize(Map<String, Object> map) {
            return new Left<>((L) map.get("value"));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(serializedValue);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Left)) return false;

            Left that = (Left) obj;
            return Objects.equals(this.serializedValue, that.serializedValue);
        }

        @Override
        public String toString() {
            return "Left(" + serializedValue + ")";
        }
    }

    @SerializableAs("Right")
    public static final class Right<L, R> extends Either<L, R> {
        private static void register() {
            ConfigurationSerialization.registerClass(Right.class, "Right");
        }

        private final R serializedValue;

        public Right(R serializedValue) {
            this.serializedValue = serializedValue;
        }

        public R getValue() {
            return serializedValue;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", serializedValue);
        }

        public static <L, R> Right<L, R> deserialize(Map<String, Object> map) {
            return new Right<>((R) map.get("value"));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(serializedValue);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Right)) return false;

            Right that = (Right) obj;
            return Objects.equals(this.serializedValue, that.serializedValue);
        }

        @Override
        public String toString() {
            return "Right(" + serializedValue + ")";
        }
    }

}
