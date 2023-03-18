package xyz.janboerman.scalaloader.plugin.runtime;

/**
 * A factory that generates a class definition given the class' name.
 */
public interface ClassGenerator {

    /**
     * Generates the class definition.
     *
     * @param className the name of the class
     * @return the classfile's bytecode
     */
    public byte[] generate(String className);

}
