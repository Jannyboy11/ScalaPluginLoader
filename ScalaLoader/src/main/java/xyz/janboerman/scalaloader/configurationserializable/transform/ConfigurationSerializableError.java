package xyz.janboerman.scalaloader.configurationserializable.transform;

/**
 * Thrown when a class annotated with {@link xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable}
 * or {@link xyz.janboerman.scalaloader.configurationserializable.DelegateSerialization} was not well-formed.
 */
public class ConfigurationSerializableError extends Error {

    public ConfigurationSerializableError(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationSerializableError(String message) {
        super(message);
    }

}
