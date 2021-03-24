package xyz.janboerman.scalaloader.configurationserializable.transform;

import xyz.janboerman.scalaloader.configurationserializable.Scan;

class LocalScanResult {

    boolean annotatedByConfigurationSerializable;
    boolean annotatedByDelegateSerialization;
    boolean implementsConfigurationSerializable;
    boolean annotatedBySerializableAs;
    Scan.Type scanType;

}
