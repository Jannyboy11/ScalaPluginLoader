package xyz.janboerman.scalaloader.plugin;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.version.ScalaVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SerializableAs("ScalaVersion")
public final class PluginScalaVersion implements ConfigurationSerializable {

    final String scalaVersion;
    final String scalaLibraryUrl;
    final String scalaReflectUrl;

    PluginScalaVersion(String scalaVersion, String libraryUrl, String reflectUrl) {
        this.scalaVersion = Objects.requireNonNull(scalaVersion, "scala scalaVersion cannot be null!");
        this.scalaLibraryUrl = Objects.requireNonNull(libraryUrl, "scala standard library url cannot be null!");
        this.scalaReflectUrl = Objects.requireNonNull(reflectUrl, "scala reflection library url cannot be null!");
    }

    //TODO move this method to the plugin loader?
    static String packagePrefix(String scalaVersion) {
        return scalaVersion.replaceAll("\\W", "_");
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other == null) return false;
        if (!(other instanceof PluginScalaVersion)) return false;
        PluginScalaVersion that = (PluginScalaVersion) other;
        return Objects.equals(this.scalaVersion, that.scalaVersion);
    }

    @Override
    public int hashCode() {
        return this.scalaVersion.hashCode();
    }

    @Override
    public String toString() {
        return scalaVersion;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("scala-version", scalaVersion);
        map.put("scala-library-url", scalaLibraryUrl);
        map.put("scala-reflect-url", scalaReflectUrl);
        return map;
    }

    public static PluginScalaVersion deserialize(Map<String, Object> map) {
        String scalaVersion = map.get("scala-version").toString();
        String scalaLibraryUrl = map.get("scala-library-url").toString();
        String scalaReflectUrl = map.get("scala-reflect-url").toString();
        return new PluginScalaVersion(scalaVersion, scalaLibraryUrl, scalaReflectUrl);
    }

    public static PluginScalaVersion fromScalaVersion(ScalaVersion scalaVersion) {
        return new PluginScalaVersion(
                scalaVersion.getName(),
                scalaVersion.getScalaLibraryUrl(),
                scalaVersion.getScalaReflectUrl());
    }
}
