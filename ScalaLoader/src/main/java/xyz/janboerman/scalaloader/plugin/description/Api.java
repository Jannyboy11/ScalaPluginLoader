package xyz.janboerman.scalaloader.plugin.description;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link Api} annotation can be used to signal to the {@link xyz.janboerman.scalaloader.plugin.ScalaPluginLoader} which version of the bukkit api is used.
 * The annotation should only be used to annotate your ScalaPlugin's main class. Using this annotation is equivalent to defining an 'api-version' property in the plugin.yml file for JavaPlugins.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface Api {

    ApiVersion value();

}
