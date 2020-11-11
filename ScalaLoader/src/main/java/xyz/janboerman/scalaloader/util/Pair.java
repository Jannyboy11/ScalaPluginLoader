package xyz.janboerman.scalaloader.util;

import java.util.Objects;

public final class Pair<T, U> {

    private final T first;
    private final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Pair)) return false;

        Pair that = (Pair) o;
        return Objects.equals(this.getFirst(), that.getFirst())
                && Objects.equals(this.getSecond(), that.getSecond());
    }

    @Override
    public String toString() {
        return "Pair{" + first + "," + second + "}";
    }
}
