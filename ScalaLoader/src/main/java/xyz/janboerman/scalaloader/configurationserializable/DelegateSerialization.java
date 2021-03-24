package xyz.janboerman.scalaloader.configurationserializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Dual of {@link ConfigurationSerializable}.
 * </p>
 * <p>
 *     While the {@literal @}ConfigurationSerializable interface is great for generating the serialization code for product types,
 *     this annotation is great for sum types. Simply slap this annotation on top of your sealed trait, class or interface
 *     and a deserialization method will be generated for it!
 * </p>
 * <p>
 *     Note that you should still annotate the subclasses with {@link ConfigurationSerializable}, or implement
 *     {@link org.bukkit.configuration.serialization.ConfigurationSerializable} manually.
 * </p>
 * @see ConfigurationSerializable
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DelegateSerialization {

    // This is implemented in two separate stages divided over 7 classes
    //  1. LocalScanner & LocalScanResult & DelegateTransformer
    //      --> Transforms the class annotated with @DelegateSerialization,
    //          generate deserialize method that load the variant from the map and calls ConfigurationSerialization.getClassByAlias
    //          and then ConfigurationSerialization.deserializeObject and then finishes with a cast to the supertype.
    //  2. GlobalScanner & GlobalScanResult & AddVariantTransformer & PluginTransformer
    //      --> AddVariantTransformer transforms the subclasses, modifies its serialize method to include the variant in the map
    //      --> PluginTransformer makes sure that the top class is registered with bukkit's ConfigurationSerialization too. (just like for @ConfigurationSerializable)
    //
    // We don't generate the @ConfigurationSerializable annotation on subclasses because they themselves might be sum types too! //TODO test that that actually works well.

    /**
     * The list of allowed subclasses. You can leave this empty if your type is already sealed at the jvm level
     * and has a permitted-subclasses attribute.
     * @return the list of subclasses of the top type.
     */
    Class<?>[] value() default {};

    /**
     * With what alias should the top type have?
     * @return the alias
     */
    String as() default "";

    /**
     * At what point should the top type be registered with Bukkit's {@link org.bukkit.configuration.serialization.ConfigurationSerialization}?
     * @return the injection point
     */
    InjectionPoint registerAt() default InjectionPoint.PLUGIN_ONENABLE;

    /**
     * Which deserialization method? Note that using {@link DeserializationMethod#MAP_CONSTRUCTOR} is disallowed for sum types!
     * @return the deserialization method
     */
    DeserializationMethod constructUsing() default DeserializationMethod.DESERIALIZE;

}
