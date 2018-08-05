package xyz.janboerman.scalaloader.plugin.description;

import java.util.HashMap;
import java.util.Map;

public enum ScalaVersion {
    v2_12_6("2.12.6", true,
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.12.6%2Fscala-reflect-2.12.6.jar",
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.12.6%2Fscala-library-2.12.6.jar"
    ),
    v2_13_0_M4("2.13.0-M4", false,
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.13.0-M4%2Fscala-reflect-2.13.0-M4.jar",
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.13.0-M4%2Fscala-library-2.13.0-M4.jar"
    );

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

    ScalaVersion(String version, boolean stable, String reflect, String library) {
        this.version = version;
        this.scalaLibraryUrl = library;
        this.scalaReflectUrl = reflect;
        this.stable = stable;
    }

    public static ScalaVersion fromVersionString(String string) {
        return byVersion.get(string);
    }

    public String getVersion() {
        return version;
    }

    public String getScalaLibraryUrl() {
        return scalaLibraryUrl;
    }

    public String getScalaReflectUrl() {
        return scalaReflectUrl;
    }

    @Override
    public String toString() {
        return getVersion();
    }

    public boolean isStable() {
        return stable;
    }

}
