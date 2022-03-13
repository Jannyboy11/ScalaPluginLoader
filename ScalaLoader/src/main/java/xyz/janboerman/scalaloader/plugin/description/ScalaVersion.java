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
    v2_13_x("2.13.8"), //latest

    //3.0.x
    v3_0_0("3.0.0"),
    v3_0_1("3.0.1"),
    v3_0_2("3.0.2"),

    //3.1.x
    v3_1_0("3.1.0"),
    v3_1_1("3.1.1");

    //TODO include hashes of the jars! so that the loader can verify the integrity of the jars!

    private static final Map<String, ScalaVersion> byVersion = new HashMap<>();
    static {
        for (ScalaVersion version : ScalaVersion.values()) {
            String ver = version.getVersion();
            byVersion.put(ver, version);
        }
    }

    private final String version;
    private final boolean stable;
    private final Map<String, String> urls;

    ScalaVersion(String version) {
        this(version, true, urls(version));
    }

    ScalaVersion(String version, boolean stable, Map<String, String> urls) {
        this.version = version;
        this.stable = stable;
        this.urls = urls;
    }

    private static Map<String, String> urls(String scalaVersion) {
        if (scalaVersion.startsWith("2.")) {
            return mapOf(
                    mapEntry(PluginScalaVersion.SCALA2_REFLECT_URL, mavenCentralSearchScalaReflect(scalaVersion)),
                    mapEntry(PluginScalaVersion.SCALA2_LIBRARY_URL, mavenCentralSearchScalaLibrary(scalaVersion))
            );
        } else if (scalaVersion.startsWith("3.0.") || scalaVersion.startsWith("3.1.")) {
            return mapOf(
                    mapEntry(PluginScalaVersion.SCALA2_LIBRARY_URL, mavenCentralSearchScalaLibrary(ScalaVersion.v2_13_x.getVersion())),
                    mapEntry(PluginScalaVersion.SCALA2_REFLECT_URL, mavenCentralSearchScalaReflect(ScalaVersion.v2_13_x.getVersion())),
                    mapEntry(PluginScalaVersion.SCALA3_LIBRARY_URL, mavenCentralScala3LibraryAdditions(scalaVersion)),
                    mapEntry(PluginScalaVersion.TASTY_CORE_URL, mavenCentralScala3TastyCoreAdditions(scalaVersion))
            );
        } else {
            assert false : "Scala 3.2+ not yet supported";
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
        return Collections.unmodifiableMap(urls);
    }

    /**
     * Get a url on which the standard library is hosted.
     * @return a url, usually to some maven repository
     * @deprecated Starting from Scala 3, the runtime is larger than just a standard and reflection library. Use {@link #getUrls()} instead.
     */
    @Deprecated
    public String getScalaLibraryUrl() {
        return urls.get(PluginScalaVersion.SCALA2_LIBRARY_URL);
    }

    /**
     * Get a url on which the reflection library is hosted.
     * @return a url, usually to some maven repository
     * @deprecated Starting from Scala 3, the runtime is larger than just a standard and reflection library. Use {@link #getUrls()} instead.
     */
    @Deprecated
    public String getScalaReflectUrl() {
        return urls.get(PluginScalaVersion.SCALA2_REFLECT_URL);
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
