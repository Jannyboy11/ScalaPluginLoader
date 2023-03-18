package xyz.janboerman.scalaloader.configurationserializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate your serializable class with this annotation instead of implementing {@link org.bukkit.configuration.serialization.ConfigurationSerializable}.
 * ScalaLoader's serialization framework can generate the {@literal public serialize()} method if it's not already present.
 * Additionally a deserialization method can be generated based on {@link ConfigurationSerializable#constructUsing()}, if one is not already present.
 *
 * @see Scan.Type
 * @see DeserializationMethod
 * @see InjectionPoint
 * @see xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions
 * @see DelegateSerialization
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ConfigurationSerializable {

    // This is implemented in two separate stages divided over 6 classes
    //  1. LocalScanner & LocalScanResult & SerializableTransformer
    //      --> Transforms the class annotated with @ConfigurationSerializable,
    //          generate serialization and deserialization method based on scan type.
    //  2. GlobalScanner & GlobalScanResult & PluginTransformer
    //      --> PluginTransformer makes sure that the class is registered with bukkit's ConfigurationSerialization.
    //

    /**
     * Your alternative for {@link org.bukkit.configuration.serialization.SerializableAs}.
     * This lets you specify the name of the type in its serialized form.
     * If you specify the empty string then the framework falls back to using the class name.
     * @return the name of the type in its serialized form
     */
    String as() default "";

    /**
     * Specify how to implement the generated serialization and deserialization methods.
     * @return the scan type.
     */
    Scan scan() default @Scan();

    /**
     * Specify which of the three deserialization methods should be used.
     * If you don't specify this, then the framework will pick a method based on the {@link Scan.Type}
     *
     * @return the deserialization method.
     * @see org.bukkit.configuration.serialization.ConfigurationSerializable
     */
    DeserializationMethod constructUsing() default DeserializationMethod.DESERIALIZE;

    /**
     * Specifies at what point the class is registered with Bukkit's ConfigurationSerialization system.
     * Defaults to {@link InjectionPoint#PLUGIN_ONENABLE}.
     *
     * @return the injection point
     * @see org.bukkit.configuration.serialization.ConfigurationSerialization
     */
    InjectionPoint registerAt() default InjectionPoint.PLUGIN_ONENABLE;

}
