package xyz.janboerman.scalaloader.configurationserializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DelegateSerialization {
    //see xyz.janboerman.scalaloader.example.scala.Maybe for example use!

    //for sum types, list all the cases here!
    // TODO in the future, when java adopts sealed types, we can generate this list if it's empty :)
    // TODO we can already add the listed classes as nest members to the nest of the host.
    // TODO the "nest host" is the class that has the DelegateDeserialization annotation
    Class<?>[] value() default {};

    String as() default "";

    InjectionPoint registerAt() default InjectionPoint.PLUGIN_ONENABLE;                 //TODO if (sub)classes in the value() array don't define an injectionpoint, then use this one.

    DeserializationMethod constructUsing() default DeserializationMethod.DESERIALIZE;   //TODO if (sub)classes in the value() array don't define a deserializationmethod, then use this one.

    //TODO classes that have this annotation should still get the ConfigurationSerializable interface (and they should still be registered!)
    //TODO it should be noted that this annotation can be put on traits (which could be compiled to either interfaces or classes)
    //TODO since interfaces can have static methods, it is no problem to generate a public static SomeTrait deserialize(Map<String, Object> map) method.
    //TODO its implementation will need to keep which subclass was used in the map.

}
