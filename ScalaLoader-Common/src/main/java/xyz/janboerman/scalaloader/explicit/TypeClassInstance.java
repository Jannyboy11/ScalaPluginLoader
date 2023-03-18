package xyz.janboerman.scalaloader.explicit;

import java.util.Objects;

public interface TypeClassInstance /*TODO extends ConfigurationSerializable*/ {

}

class SimpleInstance implements TypeClassInstance {

    public final String companionObjectName;    //could be either typeclass trait's companion object, or the companion object of the type for which the instance is needed.
    public final String methodName;

    public SimpleInstance(String companionObjectName, String methodName) {
        this.companionObjectName = Objects.requireNonNull(companionObjectName);
        this.methodName = Objects.requireNonNull(methodName);
    }

}