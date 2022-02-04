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
            if (latest == null || ScalaRelease.VERSION_COMPARATOR.compare(latest, versionString) < 0) return versionString;
            return latest;
        });

        //special cases for the transition to scala 3
        if (versionString.length() > 4) {
            switch (versionString.substring(0, 4)) {
                case "3.1.":
                    //According to https://github.com/lampepfl/dotty/releases/tag/3.1.0-RC1:
                    //"Scala 3.1 is backwards binary-compatible with Scala 3.0: libraries compiled with 3.0.x can be used from 3.1 without change."
                    compatReleaseToLatestVersionMap.compute(ScalaRelease.SCALA_3_0, (cv, latest) -> {
                        if (latest == null || ScalaRelease.VERSION_COMPARATOR.compare(latest, versionString) < 0) return versionString;
                        return latest;
                    });
                case "3.0.": //intentional fall-through: Scala 3.1.x still uses 2_13 as a base: https://search.maven.org/artifact/org.scala-lang/scala3-library_3/3.1.0/jar
                    //if we detect that scala 3.0.x is present, then mark it as highest compatible version for scala 2.13.x
                    compatReleaseToLatestVersionMap.compute(ScalaRelease.SCALA_2_13, (cv, latest) -> {
                        if (latest == null || ScalaRelease.VERSION_COMPARATOR.compare(latest, versionString) < 0) return versionString;
                        return latest;
                    });
            }
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
        if (latest == null || ScalaRelease.VERSION_COMPARATOR.compare(latest.getScalaVersion(), scalaVersion.getScalaVersion()) < 0) {
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
