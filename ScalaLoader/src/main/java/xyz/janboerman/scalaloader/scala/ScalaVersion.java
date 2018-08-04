package xyz.janboerman.scalaloader.scala;

public enum ScalaVersion {
    v2_12_6("2.12.6", true,
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.12.6%2Fscala-reflect-2.12.6.jar",
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.12.6%2Fscala-library-2.12.6.jar"
    ),
    v2_13_0_M4("2.13.0-M4", false,
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-reflect%2F2.13.0-M4%2Fscala-reflect-2.13.0-M4.jar",
            "https://bintray.com/bintray/jcenter/download_file?file_path=org%2Fscala-lang%2Fscala-library%2F2.13.0-M4%2Fscala-library-2.13.0-M4.jar"
    ),
    ;

    private final String name;
    private final String scalaLibraryUrl;
    private final String scalaReflectUrl;
    private final boolean stable;

    ScalaVersion(String name, boolean stable, String reflect, String library) {
        this.name = name;
        this.scalaLibraryUrl = library;
        this.scalaReflectUrl = reflect;
        this.stable = stable;
    }

    public String getName() {
        return name;
    }

    public String getScalaLibraryUrl() {
        return scalaLibraryUrl;
    }

    public String getScalaReflectUrl() {
        return scalaReflectUrl;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isStable() {
        return stable;
    }

}
