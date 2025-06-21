package xyz.janboerman.scalaloader.plugin.description;

import static xyz.janboerman.scalaloader.compat.Compat.*;

import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of some common recent versions of Scala.
 */
public enum ScalaVersion {

    //2.11.x
    /** @deprecated provided for those who wish to use it, but the Scala 2.11 series has been unsupported for a while now. */
    @Deprecated
    v2_11_12("2.11.12"),

    //2.12.x
    v2_12_6("2.12.6"),
    v2_12_7("2.12.7"),
    v2_12_8("2.12.8"),
    v2_12_9("2.12.9"),
    v2_12_10("2.12.10"),
    v2_12_11("2.12.11"),
    v2_12_12("2.12.12"),
    v2_12_13("2.12.13"),
    v2_12_14("2.12.14"),
    v2_12_15("2.12.15"),
    v2_12_16("2.12.16"),
    v2_12_17("2.12.17"),
    v2_12_18("2.12.18"),
    v2_12_19("2.12.19"),
    v2_12_20("2.12.20"),

    //2.13.x
    v2_13_0("2.13.0"),
    v2_13_1("2.13.1"),
    v2_13_2("2.13.2"),
    v2_13_3("2.13.3"),
    v2_13_4("2.13.4"),
    v2_13_5("2.13.5"),
    v2_13_6("2.13.6"),
    v2_13_7("2.13.7"),
    v2_13_8("2.13.8"),
    v2_13_9("2.13.9", false), //https://github.com/scala/bug/issues/12650
    v2_13_10("2.13.10"),
    v2_13_11("2.13.11"),
    v2_13_12("2.13.12"),
    v2_13_13("2.13.13"),
    v2_13_14("2.13.14"),
    v2_13_15("2.13.15"),
    v2_13_16("2.13.16"),

    //3.0.x
    v3_0_0("3.0.0"),
    v3_0_1("3.0.1"),
    v3_0_2("3.0.2"),

    //3.1.x
    v3_1_0("3.1.0"),
    v3_1_1("3.1.1"),
    v3_1_2("3.1.2"),
    v3_1_3("3.1.3"),

    //3.2.x
    v3_2_0("3.2.0"),
    v3_2_1("3.2.1"),
    v3_2_2("3.2.2"),

    //3.3.x (LTS)
    v3_3_0("3.3.0"),
    v3_3_1("3.3.1"),
    v3_3_2("3.3.2", false),
    v3_3_3("3.3.3"),

    //3.4.x
    v3_4_0("3.4.0"),
    v3_4_1("3.4.1"),
    v3_4_2("3.4.2"),
    v3_4_3("3.4.3"),

    //3.5.x
    v3_5_0("3.5.0"),
    v3_5_1("3.5.1"),
    v3_5_2("3.5.2"),

    //3.6.x
    v3_6_0("3.6.0", false),
    v3_6_1("3.6.1", false),
    v3_6_2("3.6.2"),
    v3_6_3("3.6.3"),
    v3_6_4("3.6.4"),

    //3.7.x
    v3_7_0("3.7.0"),
    v3_7_1("3.7.1");

    // When adding new entries here, don't forget to update ScalaHashes.

    private static Map<String, ScalaVersion> byVersion = new HashMap<>();
    private static final ScalaVersion latest_2_13;
    static {
        ScalaVersion latest_2_13_version = null;
        for (ScalaVersion version : ScalaVersion.values()) {
            String ver = version.getVersion();
            byVersion.put(ver, version);
            if (version.getVersion().startsWith("2.13.")) {
                latest_2_13_version = version;
            }
        }
        assert latest_2_13_version != null : "latest Scala 2.13 version cannot be null";
        latest_2_13 = latest_2_13_version;
    }

    private final String version;
    private final boolean stable;
    private Map<String, String> urls;

    ScalaVersion(String version) {
        this(version, true);
    }

    ScalaVersion(String version, boolean stable) {
        this.version = version;
        this.stable = stable;
    }

    private static Map<String, String> urls(String scalaVersion) {
        if (scalaVersion.startsWith("2.")) {
            return mapOf(
                    mapEntry(PluginScalaVersion.SCALA2_REFLECT_URL, mavenCentralSearchScalaReflect(scalaVersion)),
                    mapEntry(PluginScalaVersion.SCALA2_LIBRARY_URL, mavenCentralSearchScalaLibrary(scalaVersion))
            );
        } else if (scalaVersion.startsWith("3.")) {
            return mapOf(
                    mapEntry(PluginScalaVersion.SCALA2_LIBRARY_URL, mavenCentralSearchScalaLibrary(latest_2_13.getVersion())),
                    mapEntry(PluginScalaVersion.SCALA2_REFLECT_URL, mavenCentralSearchScalaReflect(latest_2_13.getVersion())),
                    mapEntry(PluginScalaVersion.SCALA3_LIBRARY_URL, mavenCentralScala3LibraryAdditions(scalaVersion)),
                    mapEntry(PluginScalaVersion.TASTY_CORE_URL, mavenCentralScala3TastyCoreAdditions(scalaVersion))
            );
            //TODO if Scala 3 ever stops depending on Scala 2.13, then we need to update this code!
            //TODO as of Scala 3.4, the standard library might get new additions. we need to check whether this logic still works then.
        } else {
            assert false : "Scala 4 not yet supported";
            return null;
        }
    }

    public static ScalaVersion fromVersionString(String string) {
        return byVersion.get(string);
    }

    /**
     * Get the version of Scala.
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    public static ScalaVersion getLatest_2_13() {
        return latest_2_13;
    }

    /**
     * <p>
     *     Get the download urls for some of the Scala runtime jar files.
     *     Possible keys in this map are a subset of the following constants:
     * </p>
     * <p>
     *     <ul>
     *         <li>{@link PluginScalaVersion#SCALA2_LIBRARY_URL}</li>
     *         <li>{@link PluginScalaVersion#SCALA2_REFLECT_URL}</li>
     *         <li>{@link PluginScalaVersion#SCALA3_LIBRARY_URL}</li>
     *         <li>{@link PluginScalaVersion#TASTY_CORE_URL}</li>
     *     </ul>
     * </p>
     * @return a map containing the urls as values
     */
    public Map<String, String> getUrls() {
        if (this.urls == null) this.urls = urls(getVersion());
        return Collections.unmodifiableMap(urls);
    }

    /**
     * Get a url on which the standard library is hosted.
     * @return a url, usually to some maven repository
     * @deprecated Starting from Scala 3, the runtime is larger than just a standard and reflection library. Use {@link #getUrls()} instead.
     */
    @Deprecated
    public String getScalaLibraryUrl() {
        return getUrls().get(PluginScalaVersion.SCALA2_LIBRARY_URL);
    }

    /**
     * Get a url on which the reflection library is hosted.
     * @return a url, usually to some maven repository
     * @deprecated Starting from Scala 3, the runtime is larger than just a standard and reflection library. Use {@link #getUrls()} instead.
     */
    @Deprecated
    public String getScalaReflectUrl() {
        return getUrls().get(PluginScalaVersion.SCALA2_REFLECT_URL);
    }

    /**
     * Get a string representation of this ScalaVersion.
     * @return the scala version string
     */
    @Override
    public String toString() {
        return getVersion();
    }

    /**
     * Whether this version is a stable version, or a development/snapshot version.
     * @return true if the version is stable, otherwise false.
     */
    public boolean isStable() {
        return stable;
    }


    //
    // helper methods
    //

    private static String mavenCentralSearchScalaLibrary(String scalaVersion) {
        return "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-library/" + scalaVersion + "/scala-library-" + scalaVersion + ".jar";
    }

    private static String mavenCentralSearchScalaReflect(String scalaVersion) {
        return "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-reflect/" + scalaVersion + "/scala-reflect-" + scalaVersion + ".jar";
    }

    private static String mavenCentralScala3LibraryAdditions(String scalaVersion) {
        if ("3.0.0".equals(scalaVersion)) {
            return "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala3-library_" + scalaVersion + "-nonbootstrapped/" + scalaVersion + "/scala3-library_" + scalaVersion + "-nonbootstrapped-" + scalaVersion + ".jar";
        } else {
            return "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala3-library_3/" + scalaVersion + "/scala3-library_3-" + scalaVersion + ".jar";
        }
    }

    private static String mavenCentralScala3TastyCoreAdditions(String scalaVersion) {
        if ("3.0.0".equals(scalaVersion)) {
            return "https://search.maven.org/remotecontent?filepath=org/scala-lang/tasty-core_" + scalaVersion + "-nonbootstrapped/" + scalaVersion + "/tasty-core_" + scalaVersion + "-nonbootstrapped-" + scalaVersion + ".jar";
        } else {
            return "https://search.maven.org/remotecontent?filepath=org/scala-lang/tasty-core_3/" + scalaVersion + "/tasty-core_3-" + scalaVersion + ".jar";
        }
    }

    //other possible artifacts:
    // scala3-staging
    // scala3-tasty-inspector
    // scala3-compiler

}
