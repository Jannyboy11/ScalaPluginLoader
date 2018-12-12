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
}
