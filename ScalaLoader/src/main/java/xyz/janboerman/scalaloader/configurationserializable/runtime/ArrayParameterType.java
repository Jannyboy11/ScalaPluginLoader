package xyz.janboerman.scalaloader.configurationserializable.runtime;

import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.compat.Compat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Represents array types.
 *
 * @see ParameterType
 * @see RuntimeConversions
 */
@Called
public class ArrayParameterType extends ParameterType {

    private final ParameterType componentType;
    private final boolean varArgs;

    ArrayParameterType(Set<? extends Annotation> annotations, ParameterType componentType, boolean varArgs) {
        super(annotations, Array.newInstance(componentType.getRawType(), 0).getClass());
        this.componentType = Objects.requireNonNull(componentType);
        this.varArgs = varArgs;
    }

    /**
     * The same as {@link #from(ParameterType, boolean)}, but includes annotation information.
     * @param annotations the annotations present on this parameter
     * @param componentType the component type of the array type
     * @param varArgs whether this array type was declared as var-args
     * @return a new ArrayParameterType
     */
    public static ArrayParameterType from(Set<? extends Annotation> annotations, ParameterType componentType, boolean varArgs) {
        return new ArrayParameterType(annotations, componentType, varArgs);
    }

    /**
     * Construct a new ArrayParameterType
     * @param componentType the component type of the array type
     * @param varArgs whether this array type was declared as var-args
     * @return a new ArrayParameterType
     */
    @Called
    public static ArrayParameterType from(ParameterType componentType, boolean varArgs) {
        return from(Compat.emptySet(), componentType, varArgs);
    }

    /**
     * Get the component type of this array type.
     * @return the component type
     */
    public ParameterType getComponentType() {
        return componentType;
    }

    /**
     * Whether this array parameter type uses the varargs notation (Foo... foos)
     * @return true if this parameter uses the varargs notation, otherwise false
     */
    public boolean isVarArgs() {
        return varArgs;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ArrayParameterType)) return false;

        ArrayParameterType that = (ArrayParameterType) o;
        return super.equals(that) && this.getComponentType().equals(that.getComponentType()) && this.isVarArgs() == that.isVarArgs();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getComponentType(), isVarArgs());
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        Set<? extends Annotation> annotations = getAnnotations();
        if (!annotations.isEmpty()) {
            StringJoiner annotationJoiner = new StringJoiner(" ");
            for (Annotation annotation : annotations) {
                annotationJoiner.add(annotation.toString());
            }
            stringBuilder.append(annotationJoiner.toString()).append(" ");
        }

        stringBuilder.append(getComponentType().toString());
        if (isVarArgs()) {
            stringBuilder.append("...");
        } else {
            stringBuilder.append("[]");
        }

        return stringBuilder.toString();
    }

}
