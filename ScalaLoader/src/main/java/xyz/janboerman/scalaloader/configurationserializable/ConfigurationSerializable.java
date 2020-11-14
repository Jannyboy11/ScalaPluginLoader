package xyz.janboerman.scalaloader.configurationserializable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ConfigurationSerializable {

    //if this remains empty, then use ConfigurationSerialization#registerClass(Class<? extends ConfigurationSerializable>)
    //if not, then use ConfigurationSerialization#registerClass(Class<? extends ConfigurationSerializable>, String)
    String as() default "";

    Scan scan() default @Scan(value = Scan.Type.FIELDS);

    DeserializationMethod constructUsing() default DeserializationMethod.DESERIALIZE;

    InjectionPoint registerAt() default InjectionPoint.PLUGIN_ONENABLE;

}
