package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * This class is NOT part of the public API!
 */
public class AsmConstants {

    public static final int ASM_API = Opcodes.ASM9;

    private AsmConstants() {}

    public static final String    byte_TYPE = Type.BYTE_TYPE.getInternalName();
    public static final String   short_TYPE = Type.SHORT_TYPE.getInternalName();
    public static final String     int_TYPE = Type.INT_TYPE.getInternalName();
    public static final String    long_TYPE = Type.LONG_TYPE.getInternalName();
    public static final String    char_TYPE = Type.CHAR_TYPE.getInternalName();
    public static final String   float_TYPE = Type.FLOAT_TYPE.getInternalName();
    public static final String  double_TYPE = Type.DOUBLE_TYPE.getInternalName();
    public static final String boolean_TYPE = Type.BOOLEAN_TYPE.getInternalName();
    public static final String    void_TYPE = Type.VOID_TYPE.getInternalName();

    public static final String      javaLangByte_TYPE   = Type.getType(Byte.class).getInternalName();
    public static final String     javaLangShort_TYPE   = Type.getType(Short.class).getInternalName();
    public static final String   javaLangInteger_TYPE   = Type.getType(Integer.class).getInternalName();
    public static final String      javaLangLong_TYPE   = Type.getType(Long.class).getInternalName();
    public static final String javaLangCharacter_TYPE   = Type.getType(Character.class).getInternalName();
    public static final String     javaLangFloat_TYPE   = Type.getType(Float.class).getInternalName();
    public static final String    javaLangDouble_TYPE   = Type.getType(Double.class).getInternalName();
    public static final String   javaLangBoolean_TYPE   = Type.getType(Boolean.class).getInternalName();
    public static final String      javaLangVoid_TYPE   = Type.getType(Void.class).getInternalName();
    public static final String    javaLangString_TYPE   = Type.getType(String.class).getInternalName();
    public static final String    javaLangObject_TYPE   = Type.getType(Object.class).getInternalName();

    public static final String      javaLangByte_DESCRIPTOR = Type.getType(Byte.class).getDescriptor();
    public static final String     javaLangShort_DESCRIPTOR = Type.getType(Short.class).getDescriptor();
    public static final String   javaLangInteger_DESCRIPTOR = Type.getType(Integer.class).getDescriptor();
    public static final String      javaLangLong_DESCRIPTOR = Type.getType(Long.class).getDescriptor();
    public static final String javaLangCharacter_DESCRIPTOR = Type.getType(Character.class).getDescriptor();
    public static final String     javaLangFloat_DESCRIPTOR = Type.getType(Float.class).getDescriptor();
    public static final String    javaLangDouble_DESCRIPTOR = Type.getType(Double.class).getDescriptor();
    public static final String   javaLangBoolean_DESCRIPTOR = Type.getType(Boolean.class).getDescriptor();
    public static final String      javaLangVoid_DESCRIPTOR = Type.getType(Void.class).getDescriptor();
    public static final String    javaLangString_DESCRIPTOR = Type.getType(String.class).getDescriptor();
    public static final String    javaLangObject_DESCRIPTOR = Type.getType(Object.class).getDescriptor();

}
