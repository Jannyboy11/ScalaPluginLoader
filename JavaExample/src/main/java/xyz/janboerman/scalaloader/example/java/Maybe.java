package xyz.janboerman.scalaloader.example.java;


import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable;
import xyz.janboerman.scalaloader.configurationserializable.DelegateSerialization;

import java.util.NoSuchElementException;

@DelegateSerialization
public sealed interface Maybe<T> permits Just, Nothing {

    public boolean isPresent();
    public T get();

    public static <T> Maybe<T> just(T value) { return new Just<>(value); }
    public static <T> Maybe<T> nothing() { return Nothing.getInstance(); }

    //TODO implement this, test this.

}

@ConfigurationSerializable //TODO infer Scan.Type.RECORD
//TODO should this @ConfigurationSerializable annotation be inferred too? that would be so cool!
record Just<T>(T value) implements Maybe<T> {

    @Override public boolean isPresent() { return true; }
    @Override public T get() { return value; }

}

@ConfigurationSerializable //TODO infer Scan.Type.SINGLETON_OBJECT (need to adjust the bytecode scanner such that the field name used isn't hardcoded to MODULE$)
//TODO should this @ConfigurationSerialiable annotation be inferred too? that would be so cool!
//TODO do I need a Scan.Type.LAZY_SINGLETON? that will do a static method call instead of a static field access?
final class Nothing<T> implements Maybe<T> {

    private static final Nothing<?> INSTANCE = new Nothing<>();

    private Nothing() {}

    public static <T> Nothing<T> getInstance() {
        return (Nothing<T>) INSTANCE;
    }

    @Override public boolean isPresent() { return false; }
    @Override public T get() { throw new NoSuchElementException("Nothing"); }

}
