package xyz.janboerman.scalaloader.configurationserializable.runtime;

public interface Codec<LIVE, SERIALIZED> {

    public SERIALIZED serialize(LIVE value);

    public LIVE deserialize(SERIALIZED serialized);

}
