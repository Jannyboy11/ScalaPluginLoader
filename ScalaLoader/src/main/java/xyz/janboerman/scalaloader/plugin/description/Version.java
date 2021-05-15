package xyz.janboerman.scalaloader.plugin.description;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Version information provided to {@link CustomScala} as an alternative to {@link ScalaVersion}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Version {
    String value();
    String scalaLibraryUrl();
    String scalaReflectUrl();
    ScalaLibrary[] scalaLibs() default {};

    public static @interface ScalaLibrary {
        String name();
        String url();
        //TODO sha256 or some other kind of hash
    }
}
