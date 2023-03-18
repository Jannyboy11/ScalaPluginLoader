package xyz.janboerman.scalaloader.configurationserializable.transform;

import xyz.janboerman.scalaloader.configurationserializable.Scan;

/**
 * This class is NOT part of the public API!
 */
class LocalScanResult {

    boolean annotatedByConfigurationSerializable;
    boolean annotatedByDelegateSerialization;
    boolean implementsConfigurationSerializable;
    boolean annotatedBySerializableAs;
    Scan.Type scanType;

}
