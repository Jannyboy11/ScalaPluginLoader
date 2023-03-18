package xyz.janboerman.scalaloader.plugin.description;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is used to annotate your ScalaPlugin's main class, to tell the
 * {@link xyz.janboerman.scalaloader.plugin.ScalaPluginLoader} which scala library classes it should load.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Scala {

    ScalaVersion version();

}
