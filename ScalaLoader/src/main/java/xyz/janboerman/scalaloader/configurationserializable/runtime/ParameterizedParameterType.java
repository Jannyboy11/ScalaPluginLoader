package xyz.janboerman.scalaloader.configurationserializable.runtime;

import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.compat.Compat;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Represents parameterized types.
 *
 * @see ParameterType
 * @see RuntimeConversions
 */
public class ParameterizedParameterType extends ParameterType {

    private final List<? extends ParameterType> typeParameters;

    ParameterizedParameterType(Set<? extends Annotation> annotations, Class<?> rawType, ParameterType[] typeParameters) {
        super(annotations, rawType);
        Objects.requireNonNull(typeParameters);
        if (typeParameters.length == 0) throw new IllegalArgumentException("No type parameters provided!");

        this.typeParameters = typeParameters.length == 1 ? Compat.singletonList(typeParameters[0]) : Compat.listOf(typeParameters);
    }

    /**
     * Same as {@link #from(Class, ParameterType...)} but with annotation information included.
     * @param annotations the annotations
     * @param rawType the raw type of the class
     * @param typeParameters the type parameters
     * @return a new ParameterizedParameterType
     */
    public static ParameterizedParameterType from(Set<? extends Annotation> annotations, Class<?> rawType, ParameterType... typeParameters) {
        return new ParameterizedParameterType(annotations, rawType, typeParameters);
    }

    /**
     * Construct a ParameterizedParameterType from a class and its type parameters.
     * @param rawType the raw type of the class
     * @param typeParameters the type parameters
     * @return a new ParameterizedParameterType
     */
    @Called
    public static ParameterizedParameterType from(Class<?> rawType, ParameterType... typeParameters) {
        return from(Compat.emptySet(), rawType, typeParameters);
    }

    /**
     * Get the type parameters of this parameterized type.
     * @return the type parameters
     */
    public List<? extends ParameterType> getTypeParameters() {
        return typeParameters;
    }

    /**
     * Get the i'th type parameter.
     * @param index the 0-based index
     * @return the i'th type parameter
     */
    public ParameterType getTypeParameter(int index) {
        return getTypeParameters().get(index);
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
