package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;

public class GlobalScanResult {

    String className;   //asm internal name

    boolean annotatedByDelegateSerialization;   //not yet used
    boolean annotatedByConfigurationSerializable;

    ConfigurationSerializable.InjectionPoint registerAt;

    Type[] allowedSerializableSubtypes;

}
