package xyz.janboerman.scalaloader.configurationserializable.runtime;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

class TypeUtils {

    private TypeUtils() {
    }

    static Class<?> asRawType(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return c; //can be an array type - we don't care
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type t = pt.getRawType(); //is actually a Class
            return asRawType(t);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) type;
            Type t = gat.getGenericComponentType();
            Class rawComponentType = asRawType(t);
            // can use rawComponentType.arrayType() in Java 12+.
            return Array.newInstance(rawComponentType, 0).getClass();
        } else if (type instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) type;
            Type t = tv.getBounds()[0]; //best effort
            return asRawType(t);
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type t = wt.getUpperBounds()[0]; //best effort
            return asRawType(t);
        }

        throw new IllegalArgumentException("Unrecognized type: " + type + ". Are you using a programming language that is not Java?");
    }
    
}
