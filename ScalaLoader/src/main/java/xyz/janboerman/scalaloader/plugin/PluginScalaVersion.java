package xyz.janboerman.scalaloader.plugin;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.ScalaRelease;
import static xyz.janboerman.scalaloader.compat.Compat.*;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SerializableAs("ScalaVersion")
public final class PluginScalaVersion implements ConfigurationSerializable {
    public static void register() {
        ConfigurationSerialization.registerClass(PluginScalaVersion.class, "ScalaVersion");
    }

    private static final String SCALA_VERSION = "scala-version";
    public static final String SCALA2_REFLECT_URL = "scala-reflect-url";
    public static final String SCALA2_LIBRARY_URL = "scala-library-url";
    public static final String SCALA3_LIBRARY_URL = "scala3-library-url";
    public static final String TASTY_CORE_URL = "tasty-core-url";

    //private static final String SCALA3_STAGING_URL = "scala3-staging-url";
    //private static final String TASTY_INSPECTOR_URL = "tasty-inspector-url";


    private final String scalaVersion;
    private final Map<String, String> urls;

    /**
     * @deprecated since Scala 3 there are more artifacts than just the scala standard library and the scala reflection library
     */
    @Deprecated
    public PluginScalaVersion(String scalaVersion, String libraryUrl, String reflectUrl) {
        Objects.requireNonNull(scalaVersion, "scalaVersion cannot be null!");
        Objects.requireNonNull(libraryUrl, "scala standard library url cannot be null!");
        Objects.requireNonNull(reflectUrl, "scala reflection library url cannot be null!");

        this.scalaVersion = scalaVersion;
        this.urls = mapOf(mapEntry(SCALA2_LIBRARY_URL, libraryUrl), mapEntry(SCALA2_REFLECT_URL, reflectUrl));
    }

    public PluginScalaVersion(String scalaVersion, Map<String, String> urls) {
        Objects.requireNonNull(scalaVersion, "scalaVersion cannot be null!");
        Objects.requireNonNull(urls, "urls cannot be null!");

        this.scalaVersion = scalaVersion;
        this.urls = mapCopy(urls);
    }


    public String getScalaVersion() {
        return scalaVersion;
    }

    public Map<String, String> getUrls() {
        return Collections.unmodifiableMap(urls);
    }

    @Deprecated
    public String getScalaLibraryUrl() {
        return urls.get(SCALA2_LIBRARY_URL);
    }

    @Deprecated
    public String getScalaReflectUrl() {
        return urls.get(SCALA2_REFLECT_URL);
    }

    public ScalaRelease getCompatRelease() {
        return ScalaRelease.fromScalaVersion(getScalaVersion());
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
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
        map.put("scala-version", getScalaVersion());

        for (Map.Entry<String, String> urlEntry : urls.entrySet()) {
            map.put(urlEntry.getKey(), urlEntry.getValue());
        }

        return map;
    }

    public static PluginScalaVersion deserialize(Map<String, Object> map) {
        map.remove(ConfigurationSerialization.SERIALIZED_TYPE_KEY);   //bukkit leaks the type information in its abstraction!

        String scalaVersion = map.remove("scala-version").toString();

        Map<String, String> urls = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            urls.put(entry.getKey(), entry.getValue().toString());
        }

        return new PluginScalaVersion(scalaVersion, urls);
    }

    public static PluginScalaVersion fromScalaVersion(ScalaVersion scalaVersion) {
        return new PluginScalaVersion(
                scalaVersion.getVersion(),
                scalaVersion.getUrls());
    }
}
