package xyz.janboerman.scalaloader.configurationserializable.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class ArrayParameterType extends ParameterType {

    private final ParameterType componentType;
    private final boolean varArgs;

    ArrayParameterType(Set<? extends Annotation> annotations, ParameterType componentType, boolean varArgs) {
        super(annotations, Array.newInstance(componentType.getRawType(), 0).getClass());
        this.componentType = Objects.requireNonNull(componentType);
        this.varArgs = varArgs;
    }

    public static ArrayParameterType from(Set<? extends Annotation> annotations, ParameterType componentType, boolean varArgs) {
        return new ArrayParameterType(annotations, componentType, varArgs);
    }

    public static ArrayParameterType from(ParameterType componentType, boolean varArgs) {
        return from(Collections.emptySet(), componentType, varArgs);
    }

    public ParameterType getComponentType() {
        return componentType;
    }

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
