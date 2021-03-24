package xyz.janboerman.scalaloader.configurationserializable.runtime;

public interface Codec<T> {

    public Object serialize(T value);

    public T deserialize(Object serialized);

}
