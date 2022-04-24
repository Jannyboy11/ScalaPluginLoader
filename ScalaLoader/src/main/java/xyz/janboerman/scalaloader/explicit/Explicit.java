package xyz.janboerman.scalaloader.explicit;

import xyz.janboerman.scalaloader.bytecode.Called;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Called
public class Explicit {

    private static final String NUMERIC = "scala.math.Numeric";
    private static final String INTEGRAL = "scala.math.Integral";
    private static final String FRACTIONAl = "scala.math.Fractional";
    private static final String ORDERING = "scala.math.Ordering";
    private static final String CLASSTAG = "scala.reflect.ClassTag";

//    scala.math.Numeric$ numeric;
//    scala.math.Integral$ integral$;
//    scala.math.Ordering$ ordering$;
//    scala.reflect.ClassTag$ classTag$;
//    scala.math.Numeric.BigDecimalIsFractional$ test;
//    scala.math.Ordering.BigDecimal test2;

    private Explicit() {
    }

    //TODO actually call this.
    @Called
    public static Object getTypeClassInstance(Class<?> typeClassTrait, Class<?> instanceFor, ClassLoader classLoader) {
        RuntimeException ex = new RuntimeException("Could not find " + typeClassTrait.getName() + " instance for class " + instanceFor.getName());

        //first, check for a few hardcoded instances
        try {
            switch (typeClassTrait.getName()) {
                case NUMERIC:
                    switch (instanceFor.getName()) {
                        case "scala.math.BigInt":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".BigIntIsIntegral", true, classLoader));
                        case "scala.Int":
                        case "int":
                        case "java.lang.Integer":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".IntIsIntegral", true, classLoader));
                        case "scala.Short":
                        case "short":
                        case "java.lang.Short":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".ShortIsIntegral", true, classLoader));
                        case "scala.Byte":
                        case "byte":
                        case "java.lang.Byte":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".ByteIsIntegral", true, classLoader));
                        case "scala.Char":
                        case "char":
                        case "java.lang.Character":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".CharIsIntegral", true, classLoader));
                        case "scala.Long":
                        case "long":
                        case "java.lang.Long":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".LongIsIntegral", true, classLoader));
                        case "scala.Float":
                        case "float":
                        case "java.lang.Float":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".FloatIsFractional", true, classLoader));
                        case "scala.Double":
                        case "double":
                        case "java.lang.Double":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".DoubleIsFractional", true, classLoader));
                        case "scala.math.BigDecimal":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".BigDecimalIsFractional", true, classLoader));
                    }
                    break;
                case INTEGRAL:
                    switch (instanceFor.getName()) {
                        case "scala.math.BigInt":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".BigIntIsIntegral", true, classLoader));
                        case "scala.Int":
                        case "int":
                        case "java.lang.Integer":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".IntIsIntegral", true, classLoader));
                        case "scala.Short":
                        case "short":
                        case "java.lang.Short":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".ShortIsIntegral", true, classLoader));
                        case "scala.Byte":
                        case "byte":
                        case "java.lang.Byte":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".ByteIsIntegral", true, classLoader));
                        case "scala.Char":
                        case "char":
                        case "java.lang.Character":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".CharIsIntegral", true, classLoader));
                        case "scala.Long":
                        case "long":
                        case "java.lang.Long":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".LongIsIntegral", true, classLoader));
                        case "scala.BigDecimal":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".BigDecimalAsIfIntegral", true, classLoader));
                    }
                    break;
                case FRACTIONAl:
                    switch (instanceFor.getName()) {
                        case "scala.Float":
                        case "float":
                        case "java.lang.Float":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".FloatIsFractional", true, classLoader));
                        case "scala.Double":
                        case "double":
                        case "java.lang.Double":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".DoubleIsFractional", true, classLoader));
                        case "scala.math.BigDecimal":
                            return getCompanionObjectInstance(Class.forName(NUMERIC + ".BigDecimalIsFractional", true, classLoader));
                    }
                    break;
                case ORDERING:
                    switch (instanceFor.getName()) {
                        case "scala.Unit":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Unit", true, classLoader));
                        case "scala.Boolean":
                        case "boolean":
                        case "java.lang.Boolean":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Boolean", true, classLoader));
                        case "scala.Byte":
                        case "byte":
                        case "java.lang.Byte":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Byte", true, classLoader));
                        case "scala.Char":
                        case "char":
                        case "java.lang.Character":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Char", true, classLoader));
                        case "scala.Short":
                        case "short":
                        case "java.lang.Short":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Short", true, classLoader));
                        case "scala.Int":
                        case "int":
                        case "java.lang.Integer":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Int", true, classLoader));
                        case "scala.Long":
                        case "long":
                        case "java.lang.Long":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Long", true, classLoader));
                        case "scala.Float":
                        case "float":
                        case "java.lang.Float":
                            try { //Scala 2.13.x and later
                                return getCompanionObjectInstance(Class.forName(ORDERING + ".Float" + ".TotalOrdering", true, classLoader));
                            } catch (Exception e1) {
                                try { //Scala 2.12.x and earlier
                                    return getCompanionObjectInstance(Class.forName(ORDERING + ".Float", true, classLoader));
                                } catch (Exception e2) {
                                    ex.addSuppressed(e1);
                                    ex.addSuppressed(e2);
                                }
                            }
                            break;
                        case "scala.Double":
                        case "double":
                        case "java.lang.Double":
                            try { //Scala 2.13.x and later
                                return getCompanionObjectInstance(Class.forName(ORDERING + ".Double" + ".TotalOrdering", true, classLoader));
                            } catch (Exception e1) {
                                try { //Scala 2.12.x and earlier
                                    return getCompanionObjectInstance(Class.forName(ORDERING + ".Double", true, classLoader));
                                } catch (Exception e2) {
                                    ex.addSuppressed(e1);
                                    ex.addSuppressed(e2);
                                }
                            }
                            break;
                        case "scala.BigInt":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".BigInt", true, classLoader));
                        case "scala.BigDecimal":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".BigDecimal", true, classLoader));
                        case "java.lang.String":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".String", true, classLoader));
                        case "scala.Symbol":
                            return getCompanionObjectInstance(Class.forName(ORDERING + ".Symbol", true, classLoader));
                        //can't know the orderings for Option, Seq, Either or Tuple, because we would need the ordering instances for the elements,
                        //and we don't know the type arguments sadly.
                    }
                    break;
                case CLASSTAG:
                    Class<?> companionObjectClass = getCompanionObject(Class.forName(CLASSTAG, true, classLoader));
                    Object classTagObjectInstance = getCompanionObjectInstance(companionObjectClass);
                    Method method = companionObjectClass.getMethod("apply", Class.class);
                    return method.invoke(classTagObjectInstance, instanceFor);
            }
        } catch (Exception e) {
            ex.addSuppressed(e);
        }

        //find in companion object
        List<Method> instanceMethods = getTypeClassInstancesFromCompanionObject(typeClassTrait, instanceFor);
        for (Method instanceMethod : instanceMethods) {
            Class<?>[] parameterTypes = instanceMethod.getParameterTypes();
            if (parameterTypes.length == 0) {
                try {
                    Object theCompanion = getCompanionObjectInstance(instanceFor);
                    return typeClassTrait.cast(instanceMethod.invoke(theCompanion));
                } catch (Exception e) {
                    ex.addSuppressed(e);
                }
            }
        }
        throw ex;
    }

    private static List<Method> getTypeClassInstancesFromCompanionObject(Class<?> typeClassTrait, Class<?> instanceFor) {
        List<Method> result = new ArrayList<>(1);

        //first search in the companion object of the class for which we want to find the type-class instance.
        try {
            Class<?> companionObject = getCompanionObject(instanceFor);
            for (Method method : companionObject.getMethods()) {
                if (method.getDeclaringClass() == java.lang.Object.class) continue;
                if ((method.getModifiers() & Modifier.STATIC) == Modifier.STATIC) continue;
                if (typeClassTrait.isAssignableFrom(method.getReturnType())) {
                    result.add(method);
                }
            }
        } catch (ClassNotFoundException companionObjectNotFound) {
        }

        return result;
    }

    private static Class<?> getCompanionObject(Class<?> clazz) throws ClassNotFoundException {
        String className = clazz.getName();
        if (className.endsWith("$")) return clazz;

        return Class.forName(className + "$", false, clazz.getClassLoader());
    }

    private static Object getCompanionObjectInstance(Class<?> clazz) throws ClassNotFoundException, Exception {
        clazz = getCompanionObject(clazz);
        try {
            Field field = clazz.getField("MODULE$");
            if (field.getType() == clazz && (field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) == (Modifier.STATIC | Modifier.FINAL)) {
                return field.get(null);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }
        throw new Exception("Not a scala object");
    }

}
