package xyz.janboerman.scalaloader.configurationserializable.runtime;

/**
 * A converter that converts objects from their live form to their serialized form and vice versa.
 * When a codec is registered, the ParameterType should match the LIVE type of this Codec.
 * 
 * @param <LIVE> the type of the object as it's used within the plugin, should be matched by the ParameterType this codec is registered with.
 * @param <SERIALIZED> the type of the object in its serialized form. This type must either implement {@link org.bukkit.configuration.serialization.ConfigurationSerializable},
 *                    or be one of {@link java.lang.Integer}, {@link java.lang.Double}, {@link java.lang.Boolean}, {@link java.lang.String},
 *                    {@link java.util.List}, {@link java.util.Set}, {@link java.util.Map}.
 *
 * @see RuntimeConversions
 */
public interface Codec<LIVE, SERIALIZED> {

    /**
     * Converts a live value to its serialized form.
     * @param liveValue the live value
     * @return an object that contains the same information in a different form
     */
    public SERIALIZED serialize(LIVE liveValue);

    /**
     * Converts a serialized value back into its live form.
     * @param serializedValue the serialized value
     * @return an object that contains the same information in a different form
     */
    public LIVE deserialize(SERIALIZED serializedValue);

}
