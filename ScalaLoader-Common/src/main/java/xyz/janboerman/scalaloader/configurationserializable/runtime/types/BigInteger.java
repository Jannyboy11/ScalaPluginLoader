package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;

import java.util.Map;
import java.util.Objects;

@SerializableAs("BigInteger")
public class BigInteger implements Adapter<java.math.BigInteger> {
    public static void registerWithConfigurationSerialization() {
        ConfigurationSerialization.registerClass(BigInteger.class, "BigInteger");
    }

    public final java.math.BigInteger value;

    public BigInteger(java.math.BigInteger value) {
        this.value = value;
    }

    @Override
    public Map<String, Object> serialize() {
        return Compat.singletonMap("value", value == null ? null : value.toString());
    }

    public static BigInteger deserialize(Map<String, Object> map) {
        Object value = map.get("value");
        return new BigInteger(value == null ? null : new java.math.BigInteger(value.toString()));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof BigInteger)) return false;

        BigInteger that = (BigInteger) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public java.math.BigInteger getValue() {
        return value;
    }
}
