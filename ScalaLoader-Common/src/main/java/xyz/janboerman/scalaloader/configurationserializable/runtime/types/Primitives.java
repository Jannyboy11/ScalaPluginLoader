package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;

import java.util.Map;
import java.util.Objects;

public class Primitives {

    private Primitives() {
    }

    public static void registerWithConfigurationSerialization() {
        Byte.register();
        Short.register();
        Integer.register();
        Long.register();
        Float.register();
        Double.register();
        Boolean.register();
        Character.register();
    }

    @SerializableAs("Byte")
    public static class Byte implements Adapter<java.lang.Byte> {
        private static void register() {
            ConfigurationSerialization.registerClass(Byte.class, "Byte");
        }

        public final java.lang.Byte value;

        public Byte(java.lang.Byte value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Byte deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Byte(value == null ? null : java.lang.Byte.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Byte)) return false;

            Byte that = (Byte) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Byte getValue() {
            return value;
        }
    }

    @SerializableAs("Short")
    public static class Short implements Adapter<java.lang.Short> {
        private static void register() {
            ConfigurationSerialization.registerClass(Short.class, "Short");
        }

        public final java.lang.Short value;

        public Short(java.lang.Short value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Short deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Short(value == null ? null : java.lang.Short.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Short)) return false;

            Short that = (Short) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Short getValue() {
            return value;
        }
    }

    @SerializableAs("Integer")
    public static class Integer implements Adapter<java.lang.Integer> {
        private static void register() {
            ConfigurationSerialization.registerClass(Integer.class, "Integer");
        }

        public final java.lang.Integer value;

        public Integer(java.lang.Integer value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Integer deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Integer(value == null ? null : java.lang.Integer.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Integer)) return false;

            Integer that = (Integer) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Integer getValue() {
            return value;
        }
    }

    @SerializableAs("Long")
    public static class Long implements Adapter<java.lang.Long> {
        private static void register() {
            ConfigurationSerialization.registerClass(Long.class, "Long");
        }

        public final java.lang.Long value;

        public Long(java.lang.Long value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Long deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Long(value == null ? null : java.lang.Long.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Long)) return false;

            Long that = (Long) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Long getValue() {
            return value;
        }
    }

    @SerializableAs("Float")
    public static class Float implements Adapter<java.lang.Float> {
        private static void register() {
            ConfigurationSerialization.registerClass(Float.class, "Float");
        }

        public final java.lang.Float value;

        public Float(java.lang.Float value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Float deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Float(value == null ? null : java.lang.Float.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Float)) return false;

            Float that = (Float) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Float getValue() {
            return value;
        }
    }

    @SerializableAs("Double")
    public static class Double implements Adapter<java.lang.Double> {
        private static void register() {
            ConfigurationSerialization.registerClass(Double.class, "Double");
        }

        public final java.lang.Double value;

        public Double(java.lang.Double value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Double deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Double(value == null ? null : java.lang.Double.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Double)) return false;

            Double that = (Double) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Double getValue() {
            return value;
        }
    }

    @SerializableAs("Boolean")
    public static class Boolean implements Adapter<java.lang.Boolean> {
        private static void register() {
            ConfigurationSerialization.registerClass(Boolean.class, "Boolean");
        }

        public final java.lang.Boolean value;

        public Boolean(java.lang.Boolean value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Boolean deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Boolean(value == null ? null : java.lang.Boolean.valueOf(value.toString()));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Boolean)) return false;

            Boolean that = (Boolean) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Boolean getValue() {
            return value;
        }
    }

    @SerializableAs("Character")
    public static class Character implements Adapter<java.lang.Character> {
        private static void register() {
            ConfigurationSerialization.registerClass(Character.class, "Character");
        }

        public final java.lang.Character value;

        public Character(java.lang.Character value) {
            this.value = value;
        }

        @Override
        public Map<String, Object> serialize() {
            return Compat.singletonMap("value", value == null ? null : value.toString());
        }

        public static Character deserialize(Map<String, Object> map) {
            Object value = map.get("value");
            return new Character(value == null ? null : value.toString().charAt(0));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Character)) return false;

            Character that = (Character) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }

        @Override
        public java.lang.Character getValue() {
            return value;
        }
    }

}


