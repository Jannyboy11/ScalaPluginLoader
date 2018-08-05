package xyz.janboerman.scalaloader.plugin.description;

public enum ApiVersion {

    //i probably don't want to create a 1.7 compatability layer xd
    LEGACY(null),
    v1_13("1.13");

    private final String versionString;

    ApiVersion(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

}
