package xyz.janboerman.scalaloader.configurationserializable.transform;

import xyz.janboerman.scalaloader.configurationserializable.DelegateSerialization;

/**
 * Thrown when a class annotated with {@link xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable}
 * or {@link DelegateSerialization} was not well-formed.
 */
public class ConfigurationSerializableError extends Error {

    public ConfigurationSerializableError(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationSerializableError(String message) {
        super(message);
    }

}
