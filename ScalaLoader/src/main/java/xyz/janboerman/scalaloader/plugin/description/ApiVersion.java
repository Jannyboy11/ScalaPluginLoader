package xyz.janboerman.scalaloader.plugin.description;

import org.bukkit.Bukkit;

/**
 * Representions for different versions of bukkit's API.
 *
 * @see Api
 * @see <a href="https://hub.spigotmc.org/javadocs/spigot/org/bukkit/plugin/PluginDescriptionFile.html#getAPIVersion--">PluginDescriptionFile#getApiVersion()</a>
 * @see <a href="https://www.spigotmc.org/threads/bukkit-craftbukkit-spigot-bungeecord-1-13-development-builds.328883/">Bukkit 1.13 development builds announcement</a>
 */
public enum ApiVersion {

    //I probably don't want to create a 1.7 compatability layer xD
    /** Signals that ScalaPlugin was based on Bukkit 1.12.2 or earlier */
    LEGACY(null),
    /** Signals that the ScalaPlugin was created for Bukkit 1.13 */
    v1_13("1.13"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.14 */
    v1_14("1.14"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.15 */
    v1_15("1.15"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.16 */
    v1_16("1.16"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.17 */
    v1_17("1.17"),
    /** Signals that the ScalaPlugin was created for Bukkit 1.18 */
    v1_18("1.18");

    private static final ApiVersion LATEST_VERSION;
    static {
        ApiVersion runningOn;
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion.contains("1.18")) {
            runningOn = v1_18;
        } else if (bukkitVersion.contains("1.17")) {
            runningOn = v1_17;
        } else if (bukkitVersion.contains("1.16")) {
            runningOn = v1_16;
        } else if (bukkitVersion.contains("1.15")) {
            runningOn = v1_15;
        } else if (bukkitVersion.contains("1.14")) {
            runningOn = v1_14;
        } else if (bukkitVersion.contains("1.13")) {
            runningOn = v1_13;
        } else {
            runningOn = LEGACY;
        }
        LATEST_VERSION = runningOn;
    }

    private final String versionString;

    ApiVersion(String versionString) {
        this.versionString = versionString;
    }

    public String getVersionString() {
        return versionString;
    }

    /**
     * Get the latest version that is supported by the server that ScalaLoader currently runs on.
     * @return the latest supported Bukkit API version
     */
    public static ApiVersion latest() {
        return LATEST_VERSION;
    }

}
