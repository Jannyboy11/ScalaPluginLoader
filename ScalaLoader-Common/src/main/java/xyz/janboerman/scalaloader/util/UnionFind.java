package xyz.janboerman.scalaloader.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a Union-Find (or 'disjoint union') data structure.
 * <p>
 * A Union-Find is a structure which partitions its members into disjoint sets. Each disjoint set is called a partition.
 * Each partition is identified (uniquely) by its representative element.
 * <p>
 * This Union-Find does not support null elements.
 *
 * @param <T> the type of elements in the Union-Find
 */
public class UnionFind<T> {

    private final Map<T, T> parents = new HashMap<>();
    private final Set<T> roots = new HashSet<>();

    public T getParent(T value) {
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

        add(parent);                    //parent is added as root if it is new.
        parents.put(child, parent);     //if the parent was not new, then either it already was its own representative, or there is a different representative.

        if (!child.equals(parent)) {    //if the child is not its own parent,
            roots.remove(child);        //then it's definitely not a root.
        }
    }

    public T getRepresentative(T value) {
        if (!values().contains(value))
            throw new IllegalStateException("Union-Find does not contain " + value);

        return rep(value);
    }

    private T rep(T value) {
        T parent = getParent(value);
        if (Objects.equals(parent, value)) return value;    //if a value is its own parent, then it is a representative

        T rep = getRepresentative(parent);                  //if not, get the representative of the parent
        setParent(value, rep);                              //path compression
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
