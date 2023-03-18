package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;

import java.util.Map;
import java.util.Objects;

@SerializableAs("BigDecimal")
public class BigDecimal implements Adapter<java.math.BigDecimal> {
    public static void registerWithConfigurationSerialization() {
        ConfigurationSerialization.registerClass(BigInteger.class, "BigDecimal");
    }

    public final java.math.BigDecimal value;

    public BigDecimal(java.math.BigDecimal value) {
        this.value = value;
    }

    @Override
    public Map<String, Object> serialize() {
        return Compat.singletonMap("value", value == null ? null : value.toString());
    }

    public static BigDecimal deserialize(Map<String, Object> map) {
        Object value = map.get("value");
        return new BigDecimal(value == null ? null : new java.math.BigDecimal(value.toString()));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof BigDecimal)) return false;

        BigDecimal that = (BigDecimal) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public java.math.BigDecimal getValue() {
        return value;
    }
}
