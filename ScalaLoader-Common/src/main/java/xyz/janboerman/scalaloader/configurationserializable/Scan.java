package xyz.janboerman.scalaloader.configurationserializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Tells the serialization framework how instances of the class annotated with {@link ConfigurationSerializable}
 *     should be serialized and deserialized.
 * </p>
 * <p>
 *     {@link Scan.Type#FIELDS} will generate the serialize and deserialize methods based on the fields present in the class.
 *     The code generation framework uses an empty blacklist. All instance fields are considered by default.
 *     With {@link ExcludeProperty} this behaviour can be adjusted. Simple annotate a field using this annotation,
 *     and then that field won't be serialized.
 *     Additionally {@link IncludeProperty} can be used to adjust the name of the yaml property in the serialized form.
 *     If the class does not have a zero-argument constructor, but {@link DeserializationMethod#DESERIALIZE} or {@link DeserializationMethod#VALUE_OF} is used,
 *     then the framework will try to generate a zero-argument constructor in the absence of one, which may lead to unexpected results!
 * </p>
 * <p>
 *     {@link Scan.Type#GETTER_SETTER_METHODS} will generate the serialize and deserialize methods based on the methods present in the class.
 *     The code generation framework uses and empty whitelist. None of the methods are considered by default.
 *     With {@link IncludeProperty} a pair of methods can be whitelisted. Both the getter method and the setter methods must be annotated!
 *     The annotation also allow you to specify the name of the yaml property that will be used.
 *     The 'adapt' field of the annotation lets the framework guess the name of the property based on java and scala method naming conventions.
 *     Set 'adapt' to false if you want the yaml property to exactly match the name of the method.
 *     If the class does not have a zero-argument constructor, but {@link DeserializationMethod#DESERIALIZE} or {@link DeserializationMethod#VALUE_OF} is used,
 *     then the framework will try to generate a zero-argument constructor in the absence of one, which may lead to unexpected results!
 * </p>
 * <p>
 *     {@link Scan.Type#CASE_CLASS} will generate the serialize and deserialize methods based on the presence of static apply and unapply methods.
 *     These are generated by default by the scala compiler for all case classes, but you could also implement them in a companion object,
 *     or just using static methods in Java (note that you will still need to return a boolean or scala.Option from the unapply method).
 * </p>
 * <p>
 *      {@link Scan.Type#SINGLETON_OBJECT} will cause the serialization framework to always serialize using an empty map, and always use the single instance for deserialization.
 *      This scan type only works for classes defined as an {@literal object} in Scala.
 * </p>
 * <p>
 *     {@link Scan.Type#RECORD} will cause the serialization framework to serialize and deserialize all record components.
 *     Records were introduced in Java 14 as a preview feature and stabilized in Java 16, so Scala hasn't taken advantage of them yet.
 * </p>
 * <p>
 *     {@link Scan.Type#ENUM} will generate the serialize and deserialize methods to use a Map with only one entry
 *     of which the value will be the name of the enum, as returned by {@link Enum#name()}.
 *     This scan type will only work on Java enums, and also Scala 3 enums that extend java.lang.Enum,
 *     but until then if you want to use this from Scala 2 you must implement the {@literal name(): String} in your SerializableClass
 *     and {@literal valueOf(String): SerializableClass} in the companion object.
 * </p>
 * <p>
 *     {@link Scan.Type#AUTO_DETECT} will cause the framework to detect automatically which of the other scan types should be used based on the features of the class.
 * </p>
 *
 */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Scan {

    Type value() default Type.AUTO_DETECT;

    public enum Type {

        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        FIELDS,                 //default to empty blacklist
        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        GETTER_SETTER_METHODS,  //default to empty whitelist    (include both getters and setters)
        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        CASE_CLASS,             //use productElementName(int)/productElementNames, productArity?, apply and unapply (still need to find matching apply and unapply methods!)
        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        SINGLETON_OBJECT,       //just serialize as an empty Map, deserialize using the MODULE$ static final field
        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        RECORD,                 //java records :) //use getters for serialization, use constructor for deserialization. need to auto-detect the right accessor methods and constructor from the private fields.
        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        ENUM,                   //enums. uses name() and valueOf(name). There used to be a bug in bukkit itself, but that's been fixed in 1.16.4: https://hub.spigotmc.org/jira/browse/SPIGOT-6234
        /** @see xyz.janboerman.scalaloader.configurationserializable.Scan **/
        AUTO_DETECT;

    }

    /**
     * Fields annotated with this annotation will not be considered when generating the serialization and deserialization methods.
     */
    @Target(ElementType.FIELD)
    public static @interface ExcludeProperty {
    }

    /**
     * Fields and methods annotated with this annotation will be considered when generating the serialization and deserialization methods.
     */
    @Target({ElementType.METHOD, ElementType.FIELD})
    public static @interface IncludeProperty {

        /**
         * The name of the property used in the yaml.
         * If set to the empty string, the property will be derived from the field or method name.
         *
         * @return the name of the property
         */
        String value() default "";

        /**
         * <p>
         *      Detect the property name based on the method name. This means that a property name such as 'height' can be derived from a method which
         *      uses the java bean convention 'getHeight'.
         *      The same goes for Scala naming conventions such as 'width_=' (in bytecode 'width_$eq')
         * </p>
         * <p>
         *      When set to false, the exact name of the method will be used.
         * </p>
         * @return whether the property name should be adapted from the method name.
         */
        boolean adapt() default true;

    }

}