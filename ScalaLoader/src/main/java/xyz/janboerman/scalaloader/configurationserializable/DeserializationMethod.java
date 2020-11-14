package xyz.janboerman.scalaloader.configurationserializable;

public enum DeserializationMethod {
    DESERIALIZE,
    VALUE_OF,
    MAP_CONSTRUCTOR,    //only valid for ScanTypes: FIELDS, GETTER_SETTER_METHODS, RECORD
}