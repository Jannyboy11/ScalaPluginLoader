package xyz.janboerman.scalaloader.configurationserializable.runtime;

import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.compat.Compat;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedTypeVariable;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Represents a plain type.
 * When registering a Codec, this type should correspond to the LIVE type of a {@link Codec}
 *
 * @see RuntimeConversions
 */
public class ParameterType {

    private final Set<? extends Annotation> annotations; //already unmodifiable :)
    private final Class<?> rawType;

    ParameterType(Set<? extends Annotation> annotations, Class<?> rawType) {
        Objects.requireNonNull(annotations);
        Objects.requireNonNull(rawType);

        this.annotations = annotations.isEmpty() ? Compat.emptySet() : Compat.setCopy(annotations);
        this.rawType = rawType;
    }

    /**
     * Get the raw type
     * @return the raw type
     */
    public Class<?> getRawType() {
        return rawType;
    }

    /**
     * Get the annotations present with this parameter type.
     * @return the annotations
     */
    public Set<? extends Annotation> getAnnotations() {
        return annotations;
    }

    /**
     * Don't use this. ever. the map may be altered by another thread.
     * @see #getAnnotationsMap()
     */
    private Map<Class<? extends Annotation>, Annotation> lazyAnnotations = null; //never access this directly!
    private synchronized Map<Class<? extends Annotation>, Annotation> getAnnotationsMap() {
        if (lazyAnnotations != null) return lazyAnnotations;
        Set<? extends Annotation> annotations = getAnnotations();
        if (annotations.isEmpty()) {
            lazyAnnotations = Compat.emptyMap();
        } else {
            lazyAnnotations = new HashMap<>();
            for (Annotation annotation : annotations) {
                lazyAnnotations.put(annotation.annotationType(), annotation);
            }
        }
        return lazyAnnotations;
    }

    /**
     * Get the annotation of a given annotation type.
     * @param annotationClass the annotation type
     * @param <A> the annotation type
     * @return the annotation, or null if this type is not annotated with an annotation of the given annotation type
     */
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return annotationClass.cast(getAnnotationsMap().get(annotationClass));
    }

    /**
     * Construct the ParameterType that includes annotation information.
     * @param annotations the annotations
     * @param type the {@link Class}
     * @return a new ParameterType
     */
    public static ParameterType from(Set<? extends Annotation> annotations, Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return ArrayParameterType.from(annotations, from(clazz.getComponentType()), false);
            } else {
                return new ParameterType(annotations, clazz);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Class<?> rawType = TypeUtils.asRawType(pt.getRawType());
            Type[] typeArguments = pt.getActualTypeArguments();
            ParameterType[] typeParameters = new ParameterType[typeArguments.length];
            for (int i = 0; i < typeParameters.length; i++) {
                typeParameters[i] = from(typeArguments[i]);
            }
            return ParameterizedParameterType.from(annotations, rawType, typeParameters);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            Type componentType = gat.getGenericComponentType();
            return ArrayParameterType.from(annotations, from(componentType), false);
        } else if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            AnnotatedType annotatedBound = tv.getAnnotatedBounds()[0];
            //merge annotations from the type variable, and the upper bound.
            Set<Annotation> newAnnotations = new HashSet<>(annotations);
            newAnnotations.addAll(Arrays.asList(annotatedBound.getAnnotations()));
            Type bound = annotatedBound.getType();
            return from(newAnnotations, bound);
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type upperBound = wt.getUpperBounds()[0];
            return from(annotations, upperBound);
        } else {
            //this branch should never be taken actually.
            //when it is take, TypeUtils.asRawType will throw an IllegalArgumentException
            return new ParameterType(annotations, TypeUtils.asRawType(type));
        }
    }

    /**
     * Construct a ParameterType from a {@link Class}.
     * @param type the type in terms of Java's reflection api.
     * @return a new ParameterType
     */
    @Called
    public static ParameterType from(Type type) {
        return from(Compat.emptySet(), type);
    }

    /**
     * Construct a ParameterType from a Parameter.
     * @param parameter the type in terms of Java's reflection api.
     * @return a new ParameterType
     */
    public static ParameterType from(Parameter parameter) {
        if (parameter.isVarArgs()) {
            AnnotatedArrayType annotatedArrayType = (AnnotatedArrayType) parameter.getAnnotatedType();
            return from(annotatedArrayType, true);
        } else {
            return from(parameter.getAnnotatedType());
        }
    }

    /**
     * Construct a ParameterType from an AnnotatedType
     * @param type the type in terms of Java's reflection api.
     * @return a new ParameterType
     */
    public static ParameterType from(AnnotatedType type) {
        if (type instanceof AnnotatedArrayType) {
            return from((AnnotatedArrayType) type, false);
        } else if (type instanceof AnnotatedParameterizedType) {
            return from((AnnotatedParameterizedType) type);
        } else if (type instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) type;
            AnnotatedType[] bounds = atv.getAnnotatedBounds();
            return from(bounds[0]);
        } else if (type instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType awt = (AnnotatedWildcardType) type;
            AnnotatedType[] bounds = awt.getAnnotatedUpperBounds();
            return from(bounds[0]);
        } else {
            Class<?> rawType = TypeUtils.asRawType(type.getType());
            Set<? extends Annotation> annotations = Compat.setOf(type.getAnnotations());
            return from(annotations, rawType);
        }
    }

    private static ArrayParameterType from(AnnotatedArrayType arrayType, boolean varArgs) {
        Set<? extends Annotation> baseAnnotations = Compat.setOf(arrayType.getAnnotations());

        AnnotatedType componentType = arrayType.getAnnotatedGenericComponentType();
        ParameterType componentParameterType = from(componentType);

        return ArrayParameterType.from(baseAnnotations, componentParameterType, varArgs);
    }

    private static ParameterizedParameterType from(AnnotatedParameterizedType parameterizedType) {
        Class<?> rawType = TypeUtils.asRawType(parameterizedType.getType());
        Set<? extends Annotation> baseAnnotations = Compat.setOf(parameterizedType.getAnnotations());
        AnnotatedType[] actualTypeArguments = parameterizedType.getAnnotatedActualTypeArguments();
        ParameterType[] typeParameters = new ParameterType[actualTypeArguments.length];
        for (int i = 0; i < typeParameters.length; i++) {
            AnnotatedType annotatedType = actualTypeArguments[i];
            typeParameters[i] = from(annotatedType);
        }
        return ParameterizedParameterType.from(baseAnnotations, rawType, typeParameters);
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

        stringBuilder.append(getRawType().getName());

        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ParameterType)) return false;

        ParameterType that = (ParameterType) o;
        return Objects.equals(this.getRawType(), that.getRawType())
                && Objects.equals(this.getAnnotations(), that.getAnnotations());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawType(), getAnnotations());
    }

}
