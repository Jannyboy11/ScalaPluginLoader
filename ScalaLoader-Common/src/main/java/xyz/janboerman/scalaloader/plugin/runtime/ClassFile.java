package xyz.janboerman.scalaloader.plugin.runtime;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import static xyz.janboerman.scalaloader.compat.Compat.*;
import xyz.janboerman.scalaloader.util.Base64;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@SerializableAs("ClassFile")
public final class ClassFile implements ConfigurationSerializable {
    public static void register() {
        ConfigurationSerialization.registerClass(ClassFile.class, "ClassFile");
    }

    private final String className;
    private final byte[] byteCode;

    public ClassFile(String className, byte[] byteCode) {
        assert className != null : "className cannot be null";
        assert byteCode != null : "byteCode cannot be null";

        this.className = className;
        this.byteCode = byteCode;
    }

    public String getClassName() {
        return className;
    }

    public byte[] getByteCode() {
        return getByteCode(true);
    }

    public byte[] getByteCode(boolean copy) {
        return copy ? Arrays.copyOf(byteCode, byteCode.length) : byteCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ClassFile)) return false;

        ClassFile that = (ClassFile) o;
        return Objects.equals(this.className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(className);
    }

    @Override
    public String toString() {
        return className;
    }

    @Override
    public Map<String, Object> serialize() {
        return mapOf(
                mapEntry("class-name", className),
                mapEntry("bytecode", Base64.encode(byteCode)));
    }

    public static ClassFile deserialize(Map<String, Object> map) {
        String className = (String) map.get("class-name");
        byte[] byteCode = Base64.decode((String) map.get("bytecode"));

        return new ClassFile(className, byteCode);
    }
}
