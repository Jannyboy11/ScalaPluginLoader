package xyz.janboerman.scalaloader.plugin;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import xyz.janboerman.scalaloader.ScalaRelease;
import static xyz.janboerman.scalaloader.compat.Compat.*;
import xyz.janboerman.scalaloader.compat.IScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.util.ScalaHashes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@SerializableAs("ScalaVersion")
public final class PluginScalaVersion implements ConfigurationSerializable, IScalaVersion {
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
    private final Map<String, String> sha1Hashes;

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
        this.sha1Hashes = emptyMap();
    }

    /** @deprecated use {@linkplain #PluginScalaVersion(String, Map, Map)} instead.*/
    @Deprecated
    public PluginScalaVersion(String scalaVersion, Map<String, String> urls) {
        this(scalaVersion, urls, emptyMap());
    }

    public PluginScalaVersion(String scalaVersion, Map<String, String> urls, Map<String, String> sha1hashes) {
        Objects.requireNonNull(scalaVersion, "scalaVersion cannot be null!");
        Objects.requireNonNull(urls, "urls cannot be null!");
        Objects.requireNonNull(sha1hashes, "sha1hashes cannot be null!");

        this.scalaVersion = scalaVersion;
        this.urls = mapCopy(urls);
        this.sha1Hashes = mapCopy(sha1hashes);
    }


    public String getScalaVersion() {
        return scalaVersion;
    }

    public Map<String, String> getUrls() {
        return Collections.unmodifiableMap(urls);
    }

    public Map<String, String> getSha1Hashes() {
        return Collections.unmodifiableMap(sha1Hashes);
    }

    /**
     * @deprecated use {@linkplain #getUrls()} instead.
     * @see #SCALA2_LIBRARY_URL
     */
    @Deprecated
    public String getScalaLibraryUrl() {
        return urls.get(SCALA2_LIBRARY_URL);
    }

    /**
     * @deprecated use {@linkplain #getUrls()} instead.
     * @see #SCALA2_REFLECT_URL
     */
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
        map.put(SCALA_VERSION, getScalaVersion());

        for (Map.Entry<String, String> urlEntry : urls.entrySet()) {
            map.put(urlEntry.getKey(), urlEntry.getValue());
        }
        for (Map.Entry<String, String> hashEntry : sha1Hashes.entrySet()) {
            map.put(hashEntry.getKey() + "-sha1", hashEntry.getValue());
        }

        return map;
    }

    public static PluginScalaVersion deserialize(Map<String, Object> map) {
        map.remove(ConfigurationSerialization.SERIALIZED_TYPE_KEY);   //bukkit leaks the type information in its abstraction!

        String scalaVersion = map.remove(SCALA_VERSION).toString();

        Map<String, String> urls = new HashMap<>();
        Map<String, String> sha1hashes = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("-sha1")) {
                sha1hashes.put(key, entry.getValue().toString());
            } else {
                urls.put(key, entry.getValue().toString());
            }
        }

        return new PluginScalaVersion(scalaVersion, urls, sha1hashes);
    }

    public static PluginScalaVersion fromScalaVersion(ScalaVersion scalaVersion) {
        String version = scalaVersion.getVersion();
        Map<String, String> urls = scalaVersion.getUrls();
        Map<String, String> sha1Hashes = urls.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), urlKey -> ScalaHashes.getSha1(version, urlKey)));

        return new PluginScalaVersion(version, urls, sha1Hashes);
    }

    @Override
    public String getVersionString() {
        return getScalaVersion();
    }

}
