package xyz.janboerman.scalaloader.configurationserializable.runtime;

import xyz.janboerman.scalaloader.compat.Compat;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class ParameterizedParameterType extends ParameterType {

    private final List<? extends ParameterType> typeParameters;

    ParameterizedParameterType(Set<? extends Annotation> annotations, Class<?> rawType, ParameterType[] typeParameters) {
        super(annotations, rawType);
        Objects.requireNonNull(typeParameters);
        if (typeParameters.length == 0) throw new IllegalArgumentException("No type parameters provided!");

        this.typeParameters = typeParameters.length == 1 ? Compat.singletonList(typeParameters[0]) : Compat.listOf(typeParameters);
    }

    public static ParameterizedParameterType from(Set<? extends Annotation> annotations, Class<?> rawType, ParameterType... typeParameters) {
        return new ParameterizedParameterType(annotations, rawType, typeParameters);
    }

    public static ParameterizedParameterType from(Class<?> rawType, ParameterType... typeParameters) {
        return from(Collections.emptySet(), rawType, typeParameters);
    }

    public List<? extends ParameterType> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ParameterizedParameterType)) return false;

        ParameterizedParameterType that = (ParameterizedParameterType) o;
        return super.equals(that) && this.getTypeParameters().equals(that.getTypeParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getTypeParameters());
    }

    @Override
    public String toString() {
        String superString = super.toString();
        List<? extends ParameterType> typeParameters = getTypeParameters();
        if (typeParameters.isEmpty()) {
        	return superString;
        } else {
            StringJoiner stringJoiner = new StringJoiner(",", "<", ">");
            for (ParameterType pt : typeParameters) {
                stringJoiner.add(pt.toString());
            }
            return superString + stringJoiner.toString();
        }
    }
}
