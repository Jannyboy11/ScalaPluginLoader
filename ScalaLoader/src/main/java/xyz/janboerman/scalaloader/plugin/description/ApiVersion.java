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

    //I probably don't want to create a 1.7 compatibility layer xD
    /** Signals that the ScalaPlugin was based on Bukkit 1.12.2 or earlier */
    LEGACY(null),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.13 */
    v1_13("1.13"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.14 */
    v1_14("1.14"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.15 */
    v1_15("1.15"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.16 */
    v1_16("1.16"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.17 */
    v1_17("1.17"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.18 */
    v1_18("1.18"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.19 */
    v1_19("1.19"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.20 */
    v1_20("1.20"),
    /** Signals that the ScalaPlugin was created to be compatible with Bukkit 1.21 */
    v1_21("1.21");

    private static final ApiVersion LATEST_VERSION;
    static {
        ApiVersion runningOn;
        String bukkitVersion = Bukkit.getBukkitVersion();
        if (bukkitVersion.startsWith("1.21")) {
            runningOn = v1_21;
        } else if (bukkitVersion.startsWith("1.20")) {
            runningOn = v1_20;
        } else if (bukkitVersion.startsWith("1.19")) {
            runningOn = v1_19;
        } else if (bukkitVersion.startsWith("1.18")) {
            runningOn = v1_18;
        } else if (bukkitVersion.startsWith("1.17")) {
            runningOn = v1_17;
        } else if (bukkitVersion.startsWith("1.16")) {
            runningOn = v1_16;
        } else if (bukkitVersion.startsWith("1.15")) {
            runningOn = v1_15;
        } else if (bukkitVersion.startsWith("1.14")) {
            runningOn = v1_14;
        } else if (bukkitVersion.startsWith("1.13")) {
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

    /**
     * Return the value for the api-version key.
     * @return the api-version string, or null if this ApiVersion is {@link #LEGACY}.
     */
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
