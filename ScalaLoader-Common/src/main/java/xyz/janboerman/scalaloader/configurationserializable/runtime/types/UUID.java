package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.Adapter;

import java.util.Map;
import java.util.Objects;

@SerializableAs("UUID")
public class UUID implements Adapter<java.util.UUID> {
    public static void registerWithConfigurationSerialization() {
        ConfigurationSerialization.registerClass(UUID.class, "UUID");
    }

    public final java.util.UUID value;

    public UUID(java.util.UUID value) {
        this.value = value;
    }

    @Override
    public Map<String, Object> serialize() {
        return Compat.singletonMap("value", value == null ? null : value.toString());
    }

    public static UUID deserialize(Map<String, Object> map) {
        Object value = map.get("value");
        return new UUID(value == null ? null : java.util.UUID.fromString(value.toString()));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof UUID)) return false;

        UUID that = (UUID) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }

    @Override
    public java.util.UUID getValue() {
        return value;
    }
}
