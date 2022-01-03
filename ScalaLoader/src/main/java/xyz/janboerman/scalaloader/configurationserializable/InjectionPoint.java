package xyz.janboerman.scalaloader.configurationserializable;

/**
 * <p>
 *      Lets you specify at what point in the lifecycle of the plugin the configuration-serializable class should be registered.
 *      So you don't have to do this yourself.
 * </p>
 * <p>
 *     If {@link ConfigurationSerializable#as()} or {@link DelegateSerialization#as()} is specified, then the framework will generate
 *     a call to {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class, String)},
 *     otherwise {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class)}.
 * </p>
 *
 * @see ConfigurationSerializable
 * @see DelegateSerialization
 */
public enum InjectionPoint {
    /**
     * Injects a call to {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class, String)}
     * in the plugin's onEnable method.
     */
    PLUGIN_ONENABLE,
    /**
     * Injects a call to {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class, String)}
     * in the plugin's onLoad method.
     */
    PLUGIN_ONLOAD,
    /**
     * Injects a call to {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class, String)}
     * in the plugin's constructor(s).
     */
    PLUGIN_CONSTRUCTOR,
    /**
     * Injects a call to {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class, String)}
     * in the plugin's class initializer.
     */
    PLUGIN_CLASS_INTIALIZER,
    /**
     * Injects a call to {@link org.bukkit.configuration.serialization.ConfigurationSerialization#registerClass(Class, String)}
     * in the class initializer of the serializable class itself. This means that the class won't be registered with Bukkit's
     * {@link org.bukkit.configuration.serialization.ConfigurationSerialization} until the class is loaded, but the class won't be
     * loaded until explicitly done so by your plugin's code.
     */
    CLASS_INITIALIZER,
}
