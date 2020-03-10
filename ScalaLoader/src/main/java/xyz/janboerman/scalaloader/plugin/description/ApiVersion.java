package xyz.janboerman.scalaloader.plugin.description;

/**
 * Representions for different versions of bukkit's API.
 */
public enum ApiVersion {

    //i probably don't want to create a 1.7 compatability layer xd
    LEGACY(null),
    v1_13("1.13"),
    v1_14("1.14"),
    v1_15("1.15");

    private final String versionString;

    ApiVersion(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

}
