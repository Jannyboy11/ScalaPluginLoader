package xyz.janboerman.scalaloader.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

class ScalaCompatMap {

    private final Map<String, PluginScalaVersion> scalaMap = new HashMap<>();               //e.g. ["2.12.11"->PluginScalaVersion("2.12.11",stdlibUrl,stdRefelectUrl)]
    private final Map<String, String> compatReleaseToLatestVersionMap = new HashMap<>();    //e.g. ["2.12"->"2.12.11", "2.13"->"2.13.4"]

    ScalaCompatMap() {
    }

    void add(PluginScalaVersion scalaVersion) {
        final String versionString = scalaVersion.getScalaVersion();
        scalaMap.putIfAbsent(versionString, scalaVersion);
        final String compatVersion = compatVersion(versionString);
        compatReleaseToLatestVersionMap.compute(compatVersion, (cv, latest) -> {
            if (latest == null || latest.compareTo(versionString) < 0) return versionString;
            return latest;
        });

        //special-case for the transition to scala 3
        if ("3.0.0".equals(versionString)) {
            //if we detect that scala 3.0.0 is present, then mark it as highest compatible version for scala 2.13.x
            compatReleaseToLatestVersionMap.put("2.13", "3.0.0");
            //3.0.1 won't be backwards compatible anymore with 2.13.x (by the looks of current developments)
        }
    }

    /**
     * Looks up the latest version of Scala that is binary compatible with {@code scalaVersion}.
     * @param scalaVersion the version of Scala
     * @return the latest compatible version of Scala
     */
    PluginScalaVersion getLatestVersion(final PluginScalaVersion scalaVersion) {
        final String versionString = scalaVersion.getScalaVersion();
        final String compatVersion = compatVersion(versionString);

        String latestVersion = compatReleaseToLatestVersionMap.get(compatVersion);
        if (latestVersion == null) {
            latestVersion = versionString;
            compatReleaseToLatestVersionMap.put(compatVersion, versionString);
        }

        PluginScalaVersion latest = scalaMap.get(latestVersion);
        if (latest == null || latest.getScalaVersion().compareTo(scalaVersion.getScalaVersion()) < 0) {
            latest = scalaVersion;
            scalaMap.put(versionString, scalaVersion);
        }

        return latest;
    }

    /**
     * Strips the patch version from a semantic version string. e.g. "2.12.2"->"2.12".
     *
     * @param scalaVersion the semantic version
     * @return the compatibility version
     */
    private static String compatVersion(String scalaVersion) {
        String compatVersion;
        int lastDot = scalaVersion.lastIndexOf('.');
        if (lastDot != -1) {
            compatVersion = scalaVersion.substring(0, lastDot);
        } else {
            compatVersion = scalaVersion;
        }
        return compatVersion;
    }

    public String toString() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (Entry<String, String> compatLatestEntry : compatReleaseToLatestVersionMap.entrySet()) {
            final String compatVersion = compatLatestEntry.getKey();
            final String latestVersionForCompat = compatLatestEntry.getValue();
            final PluginScalaVersion latestScalaVersion = scalaMap.get(latestVersionForCompat);
            sj.add(compatVersion + "->" + latestScalaVersion);
        }
        return sj.toString();
    }
}
