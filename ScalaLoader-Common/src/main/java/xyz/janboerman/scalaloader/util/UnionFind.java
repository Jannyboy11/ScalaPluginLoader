package xyz.janboerman.scalaloader.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a Union-Find (or 'disjoint union') data structure.
 * This Union-Find does not support null elements.
 *
 * @param <T> the type of elements in the Union-Find
 */
public class UnionFind<T> {

    private final Map<T, T> parents = new HashMap<>();
    private final Set<T> roots = new HashSet<>();

    private T getParent(T value) {
        validate(value);

        return parents.get(value);
    }

    public void add(T value) {
        validate(value);

        boolean added = parents.putIfAbsent(value, value) == null;
        if (added) {
            roots.add(value);
        }
    }

    public void setParent(T child, T parent) {
        validate(child);

        add(parent);
        parents.put(child, parent);
        roots.remove(child);
    }

    public T getRepresentative(T value) {
        T parent = getParent(value);
        if (Objects.equals(parent, value)) return value;

        T rep = getRepresentative(parent);
        setParent(value, rep);  //path compression
        return rep;
    }

    public void unite(T one, T two) {
        setParent(getRepresentative(two), getRepresentative(one));
    }

    public Set<T> values() {
        return Collections.unmodifiableSet(parents.keySet());
    }

    public Set<T> representatives() {
        return Collections.unmodifiableSet(roots);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UnionFind)) return false;

        UnionFind that = (UnionFind) o;
        return this.parents.equals(that.parents);
    }

    @Override
    public int hashCode() {
        return parents.hashCode();
    }

    private void validate(T value) {
        if (value == null)
            throw new IllegalArgumentException("This Union-Find implementation does not support null values.");
    }
}
