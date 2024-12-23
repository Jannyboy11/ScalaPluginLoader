package xyz.janboerman.scalaloader;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public final class ScalaRelease implements Comparable<ScalaRelease> {

    private static final Comparator<String> AS_INT_COMPARATOR = Comparator.comparingInt(Integer::parseInt);
    private static final Pattern VERSION_SCHEME = Pattern.compile("(?<epoch>\\d+)\\.(?<major>\\d+)\\.(?<minor>\\d+).*");
    /**
     * This comparator compares two version strings that are following the epoch.major.minor version scheme.
     */
    public static final Comparator<String> VERSION_COMPARATOR = (first, second) -> {
        Matcher matcher1 = VERSION_SCHEME.matcher(first);
        Matcher matcher2 = VERSION_SCHEME.matcher(second);
        if (matcher1.matches() && matcher2.matches()) {
            String epoch1 = matcher1.group("epoch");
            String epoch2 = matcher2.group("epoch");
            int res = AS_INT_COMPARATOR.compare(epoch1, epoch2);
            if (res != 0) return res;

            String major1 = matcher1.group("major");
            String major2 = matcher2.group("major");
            res = AS_INT_COMPARATOR.compare(major1, major2);
            if (res != 0) return res;

            String minor1 = matcher1.group("minor");
            String minor2 = matcher2.group("minor");
            res = AS_INT_COMPARATOR.compare(minor1, minor2);
            if (res != 0) return res;

            //fallback, just compare whatever is after the minor version lexicographically
            int endMinor1 = matcher1.end("minor");
            int endMinor2 = matcher2.end("minor");
            return first.substring(endMinor1).compareTo(second.substring(endMinor2));
        }

        else {
            return first.compareTo(second);
        }
    };


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
    public static final ScalaRelease SCALA_3_1 = new ScalaRelease("3.1");
    /** The Scala 3.2.x series */
    public static final ScalaRelease SCALA_3_2 = new ScalaRelease("3.2");
    /** The Scala 3.3.x series */
    public static final ScalaRelease SCALA_3_3 = new ScalaRelease("3.3");
    /** The Scala 3.4.x series */
    public static final ScalaRelease SCALA_3_4 = new ScalaRelease("3.4");
    /** The Scala 3.5.x series */
    public static final ScalaRelease SCALA_3_5 = new ScalaRelease("3.5");
    /** The Scala 3.6.x series */
    public static final ScalaRelease SCALA_3_6 = new ScalaRelease("3.6");

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
        } else if (scalaVersion.startsWith("3.2.")) {
            return SCALA_3_2;
        } else if (scalaVersion.startsWith("3.3.")) {
            return SCALA_3_3;
        } else if (scalaVersion.startsWith("3.4.")) {
            return SCALA_3_4;
        } else if (scalaVersion.startsWith("3.5.")) {
            return SCALA_3_5;
        } else if (scalaVersion.startsWith("3.6.")) {
            return SCALA_3_6;
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

    public boolean isScala2() {
        return compatVersion.startsWith("2.");
    }

    public boolean isScala3() {
        return compatVersion.startsWith("3.");
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

    @Override
    public int compareTo(ScalaRelease that) {
        String[] thisVersions = this.compatVersion.split("\\.");
        String[] thatVersions = that.compatVersion.split("\\.");

        int thisEpoch = Integer.parseInt(thisVersions[0]);
        int thatEpoch = Integer.parseInt(thatVersions[0]);
        int res = Integer.compare(thisEpoch, thatEpoch);
        if (res != 0) return res;

        int thisMajor = Integer.parseInt(thisVersions[1]);
        int thatMajor = Integer.parseInt(thatVersions[1]);
        res = Integer.compare(thisMajor, thatMajor);
        return res;
    }
}
