package xyz.janboerman.scalaloader.configurationserializable;

/**
 * Lets you specify which of the three deserialization methods should be generated.
 *
 * @see ConfigurationSerializable
 * @see DelegateSerialization
 */
public enum DeserializationMethod {
    /**
     * Causes the framework to generate:
     * <pre>
     *     <code>
     *         public static MyClass deserialize(Map&ltString, Object&gt) {
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
     *         public static MyClass valueOf(Map&ltString, Object&gt) {
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
     *         public MyClass(Map&ltString, Object&gt) {
     *             ...
     *         }
     *     </code>
     * </pre>
     */
    MAP_CONSTRUCTOR,
}