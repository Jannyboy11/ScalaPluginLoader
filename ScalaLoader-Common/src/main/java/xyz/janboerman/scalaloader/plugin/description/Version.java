package xyz.janboerman.scalaloader.plugin.description;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Version information provided to {@link CustomScala} as an alternative to {@link ScalaVersion}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
    /** The Scala version string. */
    String value();

    /** The download url for the Scala-2 standard library (for Scala 3, use the url for Scala 2.13). */
    String scalaLibraryUrl();
    /** The download url for the Scala-2 reflection library (for Scala 3, use the url for Scala 2.13). */
    String scalaReflectUrl();

    /** SHA-1 checksum for scala-library jar file. */
    String scalaLibrarySha1() default "";
    /** SHA-1 checksum for scala-reflect jar file. */
    String scalaReflectSha1() default "";

    /** Download urls for additional jar files for classes not included in the standard library or reflection library. */
    ScalaLibrary[] scalaLibs() default {};

    /** A location for a library jar for the Scala library. */
    public static @interface ScalaLibrary {
        /** The name of the library */
        String name();
        /** The download url */
        String url();
        /** The SHA-1 checksum*/
        String sha1() default "";
    }
}
