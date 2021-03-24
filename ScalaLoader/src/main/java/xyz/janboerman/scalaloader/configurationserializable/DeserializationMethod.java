package xyz.janboerman.scalaloader.configurationserializable;

/**
 * Lets you specify which of the three deserialization methods should be generated
 *
 * @see ConfigurationSerializable
 */
public enum DeserializationMethod {
    /**
     * Causes the framework to generate:
     * <pre>
     *     <code>
     *         public static MyClass deserialize(Map<String, Object>) {
     *             ...
     *         }
     *     </code>
     * </pre>
     */
    DESERIALIZE,
    /**
     * Causes the framework to generate:
     * <pre>
     *     <code>
     *         public static MyClass valueOf(Map<String, Object>) {
     *             ...
     *         }
     *     </code>
     * </pre>
     */
    VALUE_OF,
    /**
     * Causes the framework to generate:
     * <pre>
     *     <code>
     *         public MyClass(Map<String, Object>) {
     *             ...
     *         }
     *     </code>
     * </pre>
     */
    MAP_CONSTRUCTOR,
}