package xyz.janboerman.scalaloader.plugin;

import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaVersion;
import xyz.janboerman.scalaloader.util.UnionFind;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class ScalaCompatMap<SV extends IScalaVersion> {

    private final UnionFind<String/*ScalaVersion*/> scalaVersions = new UnionFind<>();  /*representatives are latest compatible version*/   //e.g. ["2.13.0"->"3.3.0", "2.12.1"->"2.12.12", "3.3.0"->"3.3.0", "2.12.12"->"2.12.12"]
    private final Map</*ScalaVersion*/String, SV> scalaMap = new HashMap<>();                                                               //e.g. ["2.12.11"->PluginScalaVersion("2.12.11", stdLibUrl, stdReflectUrl)]

    public ScalaCompatMap() {
    }

    /**
     * Registers a Scala version configuration to this ScalaCompatMap.
     * @param scalaVersion the scala configuration
     */
    public void add(SV scalaVersion) {
        final String versionString = scalaVersion.getVersionString();
        scalaMap.putIfAbsent(versionString, scalaVersion);
        scalaVersions.add(versionString);

        for (String existingLatest : Compat.setCopy(scalaVersions.representatives())) {
            if (isCompatible(existingLatest, versionString)) {
                String oldest, newest;
                if (ScalaRelease.VERSION_COMPARATOR.compare(existingLatest, versionString) < 0) {
                    oldest = existingLatest; newest = versionString;
                } else {
                    oldest = versionString; newest = existingLatest;
                }
                scalaVersions.setParent(oldest, newest);
            }
        }
    }

    //TODO be more stricts about which version is consuming which other version?
    //TODO e.g. 3.4.0 can consume 2.13.x and 3.[0123].x, but 3.3.x cannot consume 3.4.x.
    private static boolean isCompatible(String one, String two) {
        ScalaRelease release1 = ScalaRelease.fromScalaVersion(one);
        ScalaRelease release2 = ScalaRelease.fromScalaVersion(two);
        if (release1.equals(release2)) return true;
        if (release1.isScala3() && release2.isScala3()) return true;
        if (release1.equals(ScalaRelease.SCALA_2_13) && release2.isScala3()) return true;
        if (release2.equals(ScalaRelease.SCALA_2_13) && release1.isScala3()) return true;
        return false;
    }

    /**
     * Looks up the latest version of Scala that is binary compatible with {@code scalaVersion}.
     * @param scalaVersion the version of Scala
     * @return the latest compatible version of Scala
     */
    public SV getLatestVersion(final PluginScalaVersion scalaVersion) {
        return getLatestVersion(scalaVersion.getScalaVersion());
    }

    /**
     * Looks up the latest version of Scala that is binary compatible with {@code scalaVersion}.
     * @param scalaVersion the version of Scala
     * @return the latest compatible version of Scala
     */
    public SV getLatestVersion(final String scalaVersion) {
        String latest = scalaVersions.getRepresentative(scalaVersion);
        return scalaMap.get(latest);
    }

    @Override
    public String toString() {
        final StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (String scalaVersion : scalaVersions.values()) {
            String latestScalaVersion = scalaVersions.getRepresentative(scalaVersion);
            sj.add(scalaVersion + "->" + latestScalaVersion);
        }
        return sj.toString();
    }

}
