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
     *         public static MyClass deserialize(Map&lt;String, Object&gt;) {
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
     *         public static MyClass valueOf(Map&lt;String, Object&gt;) {
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
     *         public MyClass(Map&lt;String, Object&gt;) {
     *             ...
     *         }
     *     </code>
     * </pre>
     * 
     * Note that you can't use this value with {@link DelegateSerialization}.
     */
    MAP_CONSTRUCTOR,
}