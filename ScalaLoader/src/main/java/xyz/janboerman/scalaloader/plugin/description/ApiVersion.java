package xyz.janboerman.scalaloader.plugin.description;

/**
 * Representions for different versions of bukkit's API.
 *
 * @see Api
 * @see <a href="https://hub.spigotmc.org/javadocs/spigot/org/bukkit/plugin/PluginDescriptionFile.html#getAPIVersion--">PluginDescriptionFile#getApiVersion()</a>
 * @see <a href="https://www.spigotmc.org/threads/bukkit-craftbukkit-spigot-bungeecord-1-13-development-builds.328883/">Bukkit 1.13 development builds announcement</a>
 */
public enum ApiVersion {

    //i probably don't want to create a 1.7 compatability layer xd
    /** Signals that ScalaPlugin was based on Bukkit 1.12.2 or earlier */
    LEGACY(null),
    /** Signals that the ScalaPlugin was created for Bukkit 1.13 */
    v1_13("1.13"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.14 */
    v1_14("1.14"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.15 */
    v1_15("1.15");

    private static final ApiVersion LATEST_VERSION;
    static {
        ApiVersion[] versions = ApiVersion.values();
        LATEST_VERSION = versions[versions.length - 1];
    }

    private final String versionString;

    ApiVersion(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

    public static ApiVersion latest() {
        return LATEST_VERSION;
    }

}
