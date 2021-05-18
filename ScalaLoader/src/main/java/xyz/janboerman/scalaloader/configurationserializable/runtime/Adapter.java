package xyz.janboerman.scalaloader.configurationserializable.runtime;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import xyz.janboerman.scalaloader.bytecode.Called;

/**
 * <p>
 *     Represents a wrapper for types that dont implement {@link ConfigurationSerializable} by themselves.
 * </p>
 * <p>
 *     This class is used by {@link RuntimeConversions} to deserialize underlying objects.
 * </p>
 *
 * @param <T> the type of the wrapped value
 */
public interface Adapter<T> extends ConfigurationSerializable {

    /**
     * Get the value that is wrapped by this adapter.
     * @return the value
     */
    @Called
    public T getValue();

}
