package xyz.janboerman.scalaloader.plugin.description;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Version {
    String value();
    String scalaLibraryUrl();
    String scalaReflectUrl();
}
