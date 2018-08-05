package xyz.janboerman.scalaloader.scala;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Version {
    String value();
    String scalaLibraryUrl();
    String scalaReflectUrl();
}
