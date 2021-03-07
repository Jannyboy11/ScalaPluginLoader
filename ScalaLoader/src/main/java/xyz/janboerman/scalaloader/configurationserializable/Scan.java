package xyz.janboerman.scalaloader.configurationserializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//TODO javadoc this.
//TODO tell something that there is an empty black list by default, so that every field is included.
//TODO in case of methods, it's the other way around - there is an empty whitelist.
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Scan {

    //TODO rename this to 'SerializationMethod' or sth? That's clashes with DeserializationMethod (which has values VALUE_OF, DESERIALIZE and MAP_CONSTRUCTOR) tho.

    Type value() default Type.FIELDS;   //TODO default to AUTO_DETECT whenever I implement that.

    public enum Type {

        FIELDS,                 //default to empty blacklist
        GETTER_SETTER_METHODS,  //default to empty whitelist    (include both getters and setters)
        CASE_CLASS,             //use productElementName(int)/productElementNames, productArity?, apply and unapply (still need to find matching apply and unapply methods!)
        SINGLETON_OBJECT,       //just serialize as an empty Map, deserialize using the MODULE$ static final field
        RECORD,                 //java records :) //use getters for serialization, use constructor for deserialization. need to auto-detect the right accessor methods and constructor from the private fields.
        ENUM;                   //enums. uses name() and valueOf(name). There used to be a bug in bukkit itself, but that's been fixed in 1.16.4: https://hub.spigotmc.org/jira/browse/SPIGOT-6234

        //TODO AUTO_DETECT ??? would be nice to have since we can pick
        //TODO  CONSTANTS for other classes that have a private constructor but static final fields of its own type
        //TODO  CASE_CLASS for classes that have an unapply and matching apply method
        //TODO  ENUM for classes that have a 'String name()' and 'static Foo valueOf(String name)'
        //TODO  RECORD if the class extends java.lang.Record
        //TODO  SINGLETON_OBJECT for classes at end with '$' and have a public static final field of the same type named "MODULE$"
        //TODO  otherwise FIELDS
    }

    @Target(ElementType.FIELD)
    public static @interface ExcludeProperty {
        //TODO document that this only has an effect when the scantype is FIELDS
        //TODO forces this field to be excluded from the (de)serialization methods

        //TODO I suppose transient fields are excluded by default? just like static fields.
    }

    @Target({ElementType.METHOD, ElementType.FIELD})
    public static @interface IncludeProperty {
        //TODO document that this only has an effect when the scantype is GETTER_SETTER_METHODS
        //TODO forces this method to be included in the (de)serialization methods

        String value() default "";

        //TODO document that adapts both getter/setter convention from java, as well as the _$eq extension from scala.
        boolean adapt() default true;

    }

}
