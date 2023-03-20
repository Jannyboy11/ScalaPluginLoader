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

    /** Download urls for additional jar files for classes not included in the standard library or reflection library. */
    ScalaLibrary[] scalaLibs() default {};

    /** A location for a library jar for the Scala library. */
    public static @interface ScalaLibrary {
        /** The name of the library */
        String name();
        /** The download url */
        String url();
        //TODO sha256 or some other kind of checksum hash
    }
}
