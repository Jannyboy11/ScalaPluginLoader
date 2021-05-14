package xyz.janboerman.scalaloader.plugin.description;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of some common recent versions of Scala.
 */
public enum ScalaVersion {

    //2.11.x
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

    //2.13.x
    v2_13_0("2.13.0"),
    v2_13_1("2.13.1"),
    v2_13_2("2.13.2"),
    v2_13_3("2.13.3"),
    v2_13_4("2.13.4"),
    v2_13_5("2.13.5"),

    //3.0.x
    /** @deprecated not recommended to use anymore - Scala 3.0.0 has been released, but at the time of writing this comment it hasn't been published to Maven Central yet. */
    v3_0_0_RC3("3.0.0-RC3", false, mavenCentralSearchScalaReflect("3.0.0-RC3"), mavenCentralSearchScalaLibrary("3.0.0-RC3"));

    private static String mavenCentralSearchScalaLibrary(String scalaVersion) {
        return "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-library/" + scalaVersion + "/scala-library-" + scalaVersion + ".jar";
    }

    private static String mavenCentralSearchScalaReflect(String scalaVersion) {
        return "https://search.maven.org/remotecontent?filepath=org/scala-lang/scala-reflect/" + scalaVersion + "/scala-library-" + scalaVersion + ".jar";
    }

    //TODO include hashes of the jars! so that the loader can verify the integrity of the jars!

    private static Map<String, ScalaVersion> byVersion = new HashMap<>();
    static {
        for (ScalaVersion version : ScalaVersion.values()) {
            byVersion.put(version.getVersion(), version);
        }
    }

    private final String version;
    private final String scalaLibraryUrl;
    private final String scalaReflectUrl;
    private final boolean stable;

    ScalaVersion(String version) {
        this(version, true, mavenCentralSearchScalaReflect(version), mavenCentralSearchScalaLibrary(version));
    }

    ScalaVersion(String version, boolean stable, String reflect, String library) {
        this.version = version;
        this.scalaLibraryUrl = library;
        this.scalaReflectUrl = reflect;
        this.stable = stable;
    }

    public static ScalaVersion fromVersionString(String string) {
        return byVersion.get(string);
    }

    /**
     * Get the version of Scala
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get a url on which the standard library is hosted.
     * @return a url, usually to some maven repository
     */
    public String getScalaLibraryUrl() {
        return scalaLibraryUrl;
    }

    /**
     * Get a url on which the reflection library is hosted.
     * @return a url, usually to some maven repository
     */
    public String getScalaReflectUrl() {
        return scalaReflectUrl;
    }

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

}
