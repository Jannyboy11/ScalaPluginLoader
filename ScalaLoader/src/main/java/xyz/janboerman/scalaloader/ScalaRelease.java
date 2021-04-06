package xyz.janboerman.scalaloader;

import java.util.Objects;

/**
 * <p>
 *     This class represents a compatibility release version of Scala.
 * </p>
 * <p>
 *     Some constants for commonly used versions of Scala are provided.
 * </p>
 *
 *
 * @implNote this class should not be converted into an enum because we want to support the use case of plugin authors to use unsupported and experimental versions of Scala.
 */
public final class ScalaRelease {
    //don't enum-ize this class, because we still want to support scala releases that are not included by default!

    /** The Scala 2.11.x series */
    private static final ScalaRelease SCALA_2_11 = new ScalaRelease("2.11");
    /** The Scala 2.12.x series */
    public static final ScalaRelease SCALA_2_12 = new ScalaRelease("2.12");
    /** The Scala 2.13.x series */
    public static final ScalaRelease SCALA_2_13 = new ScalaRelease("2.13");
    /** The Scala 3.0.x series */
    public static final ScalaRelease SCALA_3_0 = new ScalaRelease("3.0");
    /** The Scala 3.1.x series */
    private static final ScalaRelease SCALA_3_1 = new ScalaRelease("3.1");

    private final String compatVersion;

    private ScalaRelease(String compatVersion) {
        this.compatVersion = compatVersion;
    }

    /**
     * <p>
     *     Get the ScalaRelease from a Scala version string.
     * </p>
     * <p>
     *     The Scala version sting is expected to have the compatibility-release as its prefix.
     * </p>
     *
     * @param scalaVersion the scala version string
     * @return the ScalaRelease
     */
    public static ScalaRelease fromScalaVersion(String scalaVersion) {
        Objects.requireNonNull(scalaVersion, "scalaVersion cannot be null!");

        if (scalaVersion.startsWith("2.11.")) {
            return SCALA_2_11;
        } else if (scalaVersion.startsWith("2.12.")) {
            return SCALA_2_12;
        } else if (scalaVersion.startsWith("2.13.")) {
            return SCALA_2_13;
        } else if (scalaVersion.startsWith("3.0.")) {
            return SCALA_3_0;
        } else if (scalaVersion.startsWith("3.1.")) {
            return SCALA_3_1;
        }

        else {
            //best effort
            String compatVersion;
            int lastDot = scalaVersion.lastIndexOf('.');
            if (lastDot != -1) {
                compatVersion = scalaVersion.substring(0, lastDot);
            } else {
                compatVersion = scalaVersion;
            }

            return new ScalaRelease(compatVersion);
        }
    }

    /**
     * <p>
     *     Get the compatibility version of Scala.
     * </p>
     * <p>
     *     For example, the compatibility version of Scala 2.13.5 would be "2.13".
     * </p>
     * @return the compatibility version
     */
    public String getCompatVersion() {
        return compatVersion;
    }

    @Override
    public String toString() {
        return "ScalaRelease " + compatVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ScalaRelease)) return false;

        ScalaRelease that = (ScalaRelease) o;
        return this.compatVersion.equals(that.compatVersion);
    }

    @Override
    public int hashCode() {
        return compatVersion.hashCode();
    }
}
