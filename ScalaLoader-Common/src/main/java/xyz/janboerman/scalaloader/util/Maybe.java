package xyz.janboerman.scalaloader.util;

import java.util.NoSuchElementException;
import java.util.Objects;

public abstract class/*interface*/ Maybe<T> /*permits Just, Nothing*/ {

    Maybe() {
    }

    public abstract T get();
    public abstract boolean isPresent();

    public static <T> Maybe<T> just(T value) {
        return new Just<>(value);
    }

    public static <T> Maybe<T> nothing() {
        return Nothing.getInstance();
    }
}

final class Just<T> extends Maybe<T> {

    private final T value;

    Just(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(get());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Just)) return false;

        Just<?> that = (Just<?>) obj;
        return Objects.equals(this.get(), that.get());
    }

    @Override
    public String toString() {
        return "Just(" + get() + ")";
    }
}

final class Nothing<T> extends Maybe<T> {

    private static final Nothing INSTANCE = new Nothing();

    private Nothing() {
    }

    @Override
    public T get() {
        throw new NoSuchElementException("Nothing");
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    static <T> Nothing<T> getInstance() {
        return (Nothing<T>) INSTANCE;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public String toString() {
        return "Nothing";
    }

}
