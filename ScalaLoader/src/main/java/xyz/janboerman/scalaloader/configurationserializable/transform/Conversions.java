package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;
import static xyz.janboerman.scalaloader.bytecode.AsmConstants.*;
import xyz.janboerman.scalaloader.bytecode.LocalVariableDefinition;

import java.util.List;

class Conversions {

    private Conversions() {}

    static StackLocal toSerializedType(MethodVisitor methodVisitor, String descriptor, String signature, int localVariableIndex, Label start, Label end, List<LocalVariableDefinition> extraLocals) {
        StackLocal stackLocal = new StackLocal();

        //TODO implement arrays, java.util.List, java.util.Set and java.util.Map later.
        //TODO look at their signature!

        //TODO scala type don't need any special conversion in theory because I can just INTERCEPT CLASSLOADING and make them implement ConfigurationSerializable :)) (and register them ofc!)

        switch (descriptor) {
            //primitives
            case "B": //interestingly, I can just call a method that takes an int with a byte.
            case "S": //interestingly, I can just call a method that takes an int with a short.
            case "I": //so we just fall-through
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case "J":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "toString", "(J)Ljava/lang/String;", false);
                break;
            case "F":
                methodVisitor.visitInsn(F2D); //convert float to double and fall-through to Double.valueOf(double)
            case "D":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case "C":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "toString", "(C)Ljava/lang/String;", false);
                break;
            case "Z":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;

            //boxed primitives
            case "Ljava/lang/Byte;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "intValue", "()I", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case "Ljava/lang/Short;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "intValue", "()I", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case "Ljava/lang/Integer;":
                break;
            case "Ljava/lang/Long;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "toString", "()Ljava/lang/String;", false);
                break;
            case "Ljava/lang/Float":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "doubleValue", "()D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case "Ljava/lang/Double;":
                break;
            case "Ljava/lang/Character;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "toString", "()Ljava/lang/String;", false);
                break;
            case "Ljava/lang/Boolean;":
                break;

            //other reference types
            //String, List, Set and Map are a no-op (just like Integer, Boolean and Double)
            //the same holds for any type that implements ConfigurationSerializable

            case "Ljava/math/BigInteger;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "toString", "()Ljava/lang/String;", false);
                break;
            case "Ljava/math/BigDecimal;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "toString", "()Ljava/lang/String;", false);
                break;
            case "Ljava/util/UUID;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/UUID", "toString", "()Ljava/lang/String;", false);
                break;

            //TODO something like Date, DateFormat, Instant, LocalDateTime, other Time-api related things?
            //TODO Locale, CharSet?

            //in any other case: assume the type is ConfigurationSerializable and just no-op!
        }

        return stackLocal;
    }

    static StackLocal toLiveType(MethodVisitor methodVisitor, String descriptor, String signature, int localVariableIndex, Label start, Label end, List<LocalVariableDefinition> extraLocals) {
        StackLocal stackLocal = new StackLocal();

        //TODO other types?

        if (signature != null) {
            switch (signature) {
                //TODO arrays, java.util.List, java.util.Set, java.util.Map
                //TODO recursive calls!
            }
        }

        switch (descriptor) {
            //primitives
            case "B":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "byteValue", "()B", false);
                break;
            case "S":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "shortValue", "()S", false);
                break;
            case "I":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                break;
            case "J":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "parseLong", "(Ljava/lang/String;)J", false);
                break;
            case "F":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "floatValue", "()F", false);
                break;
            case "D":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                break;
            case "C":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitInsn(ICONST_0); stackLocal.increasedMaxStack += 1;
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
                break;
            case "Z":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;

            //boxed primitives
            case "Ljava/lang/Byte;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "byteValue", "()B", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case "Ljava/lang/Short;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "shortValue", "()S", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case "Ljava/lang/Integer;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                break;
            case "Ljava/lang/Long;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(Ljava/lang/String;)Ljava/lang/Long;", false);
                break;
            case "Ljava/lang/Float;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "floatValue", "()F", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case "Ljava/lang/Double;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                break;
            case "Ljava/lang/Character":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitInsn(ICONST_0); stackLocal.increasedMaxStack += 1;
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case "Ljava/lang/Boolean;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                break;

            //non-supported reference types
            case "Ljava/math/BigInteger;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                //stack: [..., string]
                methodVisitor.visitTypeInsn(NEW, "java/math/BigInteger");
                //stack: [..., string, biginteger]
                methodVisitor.visitInsn(DUP_X1);
                //stack: [..., biginteger, string, biginteger]
                methodVisitor.visitInsn(SWAP);
                //stack: [..., biginteger, biginteger, string]
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V", false);
                //stack: [..., biginteger]
                stackLocal.increasedMaxStack += 2;
                break;
            case "Ljava/math/BigDecimal;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                //stack: [..., string]
                methodVisitor.visitTypeInsn(NEW, "java/math/BigDecimal");
                //stack: [..., string, bigdecimal]
                methodVisitor.visitInsn(DUP_X1);
                //stack: [..., bigdecimal, string, bigdecimal]
                methodVisitor.visitInsn(SWAP);
                //stack: [..., bigdecimal, bigdecimal, string]
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V", false);
                //stack: [..., bigdecimal]
                stackLocal.increasedMaxStack += 2;
                break;
            case "Ljava/util/UUID;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/UUID", "fromString", "(Ljava/lang/String;)Ljava/util/UUID;", false);
                break;

            //supported reference types
            case "Ljava/lang/String;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                break;
            case "Ljava/util/List;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");
                break;
            case "Ljava/util/Set;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Set");
                break;
            case "Ljava/util/Map;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
                break;

            default:
                //assume ConfigurationSerializable, just cast.
                methodVisitor.visitTypeInsn(CHECKCAST, Type.getType(descriptor).getInternalName());
                break;
        }

        return stackLocal;
    }

    static String boxedType(String type) {
        switch (type) {
            case "B": return javaLangByte_TYPE;
            case "S": return javaLangShort_TYPE;
            case "I": return javaLangInteger_TYPE;
            case "J": return javaLangLong_TYPE;
            case "C": return javaLangCharacter_TYPE;
            case "F": return javaLangFloat_TYPE;
            case "D": return javaLangDouble_TYPE;
            case "Z": return javaLangBoolean_TYPE;
            case "V": return javaLangVoid_TYPE;
        }

        return type;
    }

    static String boxedDescriptor(String descriptor) {
        switch (descriptor) {
            case "B": return javaLangByte_DESCRIPTOR;
            case "S": return javaLangShort_DESCRIPTOR;
            case "I": return javaLangInteger_DESCRIPTOR;
            case "J": return javaLangLong_DESCRIPTOR;
            case "C": return javaLangCharacter_DESCRIPTOR;
            case "F": return javaLangFloat_DESCRIPTOR;
            case "D": return javaLangDouble_DESCRIPTOR;
            case "Z": return javaLangBoolean_DESCRIPTOR;
            case "V": return javaLangVoid_DESCRIPTOR;
        }

        return descriptor;
    }

//    // primitives:
//
//    public static Integer byte2Integer(byte b) {
//        return Integer.valueOf((int) b);
//    }
//
//    public static byte Integer2byte(Integer i) {
//        return i.byteValue();
//    }
//
//    public static Integer short2Integer(short s) {
//        return Integer.valueOf((int) s);
//    }
//
//    public static short Integer2short(Integer i) {
//        return i.shortValue();
//    }
//
//    public static Integer int2Integer(int i) {
//        return Integer.valueOf(i);
//    }
//
//    public static int Integer2int(Integer i) {
//        return i.intValue();
//    }
//
//    public static String long2String(long l) {
//        return Long.toString(l);
//    }
//
//    public static long String2long(String s) {
//        return Long.parseLong(s);
//    }
//
//    public static Double float2Double(float f) {
//        return Double.valueOf((double) f);
//    }
//
//    public static float Double2float(Double d) {
//        return (float) d.doubleValue();
//    }
//
//    public static Double double2Double(double d) {
//        return Double.valueOf(d);
//    }
//
//    public static double Double2double(Double d) {
//        return d.doubleValue();
//    }
//
//    public static String char2String(char c) {
//        return Character.toString(c);
//    }
//
//    public static char String2char(String s) {
//        return s.charAt(0);
//    }
//
//    public static Boolean boolean2Boolean(boolean b) {
//        return Boolean.valueOf(b);
//    }
//
//    public static boolean Boolean2boolean(Boolean b) {
//        return b.booleanValue();
//    }
//
//
//    // boxed primitives:
//
//    public static Integer Byte2Integer(Byte b) {
//        return Integer.valueOf(b.intValue());
//    }
//
//    public static Byte Integer2Byte(Integer i) {
//        return Byte.valueOf(i.byteValue());
//    }
//
//    public static Integer Short2Integer(Short s) {
//        return Integer.valueOf(s.intValue());
//    }
//
//    public static Short Integer2Short(Integer i) {
//        return Short.valueOf(i.shortValue());
//    }
//
//    // Integer is already correct!
//
//    public static String Long2String(Long l) {
//        return l.toString();
//    }
//
//    public static Long String2Long(String s) {
//        return Long.valueOf(s);
//    }
//
//    public static Double Float2Double(Float f) {
//        return Double.valueOf(f.doubleValue());
//    }
//
//    public static Float Double2Float(Double d) {
//        return Float.valueOf(d.floatValue());
//    }
//
//    // Double is already correct!
//
//    public static String Character2String(Character c) {
//        return c.toString();
//    }
//
//    public static Character String2Char(String s) {
//        return Character.valueOf(s.charAt(0));
//    }
//
//    // Boolean is already correct!
//
//    // other reference types:
//
//    public static String BigInteger2String(BigInteger bi) {
//        return bi.toString();
//    }
//
//    public static BigInteger String2BigInteger(String s) {
//        return new BigInteger(s);
//    }
//
//    public static String BigDecimal2String(BigDecimal bd) {
//        return bd.toString();
//    }
//
//    public static BigDecimal String2BigDecimal(String s) {
//        return new BigDecimal(s);
//    }
//
//    public static long Long2long(Object lo) {
//        return Long.parseLong((String) lo);
//    }
//
//    // String is already correct!
//
//    public static byte[] List2byteArray(List<Integer> list) {
//        byte[] result = new byte[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = Integer2byte(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<Integer> byteArray2List(byte[] bytes) {
//        List<Integer> result = new ArrayList<>(bytes.length);
//        for (int i = 0; i < bytes.length; i++) {
//            result.add(byte2Integer(bytes[i]));
//        }
//        return result;
//    }
//
//    public static short[] List2shortArray(List<Integer> list) {
//        short[] result = new short[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = Integer2short(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<Integer> shortArray2List(short[] shorts) {
//        List<Integer> result = new ArrayList<>();
//        for (short aShort : shorts) {
//            result.add(short2Integer(aShort));
//        }
//        return result;
//    }
//
//    public static int[] List2intArray(List<Integer> list) {
//        int[] result = new int[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = Integer2int(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<Integer> intArray2List(int[] ints) {
//        List<Integer> result = new ArrayList<>();
//        for (int anInt : ints) {
//            result.add(int2Integer(anInt));
//        }
//        return result;
//    }
//
//    public static long[] List2longArray(List<String> list) {
//        long[] result = new long[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = String2long(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<String> longArray2List(long[] longs) {
//        List<String> result = new ArrayList<>(longs.length);
//        for (long aLong : longs) {
//            result.add(Long2String(aLong));
//        }
//        return result;
//    }
//
//    public static float[] List2floatArray(List<Double> list) {
//        float[] result = new float[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = Double2float(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<Double> floatArray2List(float[] floats) {
//        List<Double> result = new ArrayList<>(floats.length);
//        for (float aFloat : floats) {
//            result.add(float2Double(aFloat));
//        }
//        return result;
//    }
//
//    public static double[] List2doubleArray(List<Double> list) {
//        double[] result = new double[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = Double2double(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<Double> doubleArray2List(double[] doubles) {
//        List<Double> result = new ArrayList<>(doubles.length);
//        for (double d : doubles) {
//            result.add(double2Double(d));
//        }
//        return result;
//    }
//
//    public static char[] List2charArray(List<String> list) {
//        char[] result = new char[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = String2char(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<String> charArray2List(char[] chars) {
//        List<String> result = new ArrayList<>();
//        for (char c : chars) {
//            result.add(c);
//        }
//        return result;
//    }
//
//    public static boolean[] List2booleanArray(List<Boolean> list) {
//        boolean[] result = new boolean[list.size()];
//        for (int i = 0; i < result.length; i++) {
//            result[i] = Boolean2boolean(list.get(i));
//        }
//        return result;
//    }
//
//    public static List<Boolean> booleanArray2List(boolean[] booleans) {
//        List<Boolean> result = new ArrayList<>(booleans.length);
//        for (boolean b : booleans) {
//            result.add(Boolean2boolean(b));
//        }
//        return result;
//    }
//
//    //TODO arrays of boxed primitives
//    //TODO arrays of other reference types that I want to support out of the box:
//    //TODO BigInteger, BigDecimal, String and UUID
//    //TODO ACTUALLY - I think it's probably better to generate that bytecode in the classfile itself! (in case of nested arrays!)
//    //TODO arrays of enums
//    //TODO arrays of other configurationserializable types

}
