package xyz.janboerman.scalaloader.plugin;

import xyz.janboerman.scalaloader.ScalaRelease;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

class ScalaCompatMap {

    private final Map<ScalaRelease, String> compatReleaseToLatestVersionMap = new HashMap<>();  //e.g. ["2.12"->"2.12.11", "2.13"->"2.13.4"]
    private final Map<String, PluginScalaVersion> scalaMap = new HashMap<>();                   //e.g. ["2.12.11"->PluginScalaVersion("2.12.11", stdLibUrl, stdReflectUrl)]

    ScalaCompatMap() {
    }

    void add(PluginScalaVersion scalaVersion) {
        final String versionString = scalaVersion.getScalaVersion();
        scalaMap.putIfAbsent(versionString, scalaVersion);
        final ScalaRelease compatVersion = scalaVersion.getCompatRelease();
        compatReleaseToLatestVersionMap.compute(compatVersion, (cv, latest) -> {
            if (latest == null || latest.compareTo(versionString) < 0) return versionString;
            return latest;
        });

        //special-case for the transition to scala 3
        if (versionString.startsWith("3.0.")) {
            //if we detect that scala 3.0.x is present, then mark it as highest compatible version for scala 2.13.x
            compatReleaseToLatestVersionMap.put(ScalaRelease.SCALA_2_13, versionString);
            //3.1 won't be backwards compatible anymore with 2.13.x (by the looks of current developments)
        }
    }

    /**
     * Looks up the latest version of Scala that is binary compatible with {@code scalaVersion}.
     * @param scalaVersion the version of Scala
     * @return the latest compatible version of Scala
     */
    PluginScalaVersion getLatestVersion(final PluginScalaVersion scalaVersion) {
        final String versionString = scalaVersion.getScalaVersion();
        final ScalaRelease compatVersion = scalaVersion.getCompatRelease();

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

    @Override
    public String toString() {
        final StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (Entry<ScalaRelease, String> compatLatestEntry : compatReleaseToLatestVersionMap.entrySet()) {
            final ScalaRelease compatVersion = compatLatestEntry.getKey();
            final String latestVersionForCompat = compatLatestEntry.getValue();
            final PluginScalaVersion latestScalaVersion = scalaMap.get(latestVersionForCompat);
            sj.add(compatVersion.getCompatVersion() + "->" + latestScalaVersion);
        }
        return sj.toString();
    }
}
