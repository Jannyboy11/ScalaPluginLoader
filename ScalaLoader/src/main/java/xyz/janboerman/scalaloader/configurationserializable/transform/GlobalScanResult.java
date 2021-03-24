package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.Type;
import xyz.janboerman.scalaloader.configurationserializable.InjectionPoint;
import xyz.janboerman.scalaloader.configurationserializable.Scan;

import java.util.Set;

/**
 * This class is NOT part of the public API!
 */
public class GlobalScanResult {

    String className;   //asm internal name (e.g. "com/example/Foo")
    boolean isInterface;

    boolean annotatedByDelegateSerialization;
    boolean annotatedByConfigurationSerializable;

    InjectionPoint registerAt;
    Scan.Type scanType;

    Set<Type> sumAlternatives;

}
