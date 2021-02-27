package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;  //explicitly import because there is also java.lang.reflect.Type
import static org.objectweb.asm.Opcodes.*;

import static xyz.janboerman.scalaloader.bytecode.AsmConstants.*;
import xyz.janboerman.scalaloader.bytecode.*;

import java.util.*;

class Conversions {

    private Conversions() {}

    static StackLocal toSerializedType(MethodVisitor methodVisitor, String descriptor, String signature, int localVariableIndex, Label start, Label end, LocalVariableTable localVariables) {
        final StackLocal stackLocal = new StackLocal();

        //TODO detect arrays


        //TODO implement conversion of elements of java.util.List, java.util.Set and java.util.Map later.
        //TODO look at their signature!

        //TODO just like conversion for arrays, implement conversion for scala collection types (both mutable and immutable) (including: tuples, Option, Either, Try)

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

    private static void arrayToSerializedType(StackLocal stackLocal, MethodVisitor methodVisitor, String descriptor, String signature, TypeSignature typeSignature, int localVariableIndex, Label start, Label end, LocalVariableTable locals) {
        assert TypeSignature.ARRAY.equals(typeSignature.getTypeName()) : "not an array";

        final Label endLabel = new Label();                                         //[..., array]

        //store the array as a local variable.
        final Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        final int outerArrayLocalVariableIndex = localVariableIndex++;
        methodVisitor.visitVarInsn(ASTORE, outerArrayLocalVariableIndex);           //[...]
        LocalVariable array0 = new LocalVariable("array0", descriptor, signature, label0, endLabel, outerArrayLocalVariableIndex);
        locals.add(array0);

        //store the list as a local variable, use ArrayList(int) constructor.
        final int outerListLocalVariableIndex = localVariableIndex++;
        final Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");                    //[..., list]
        methodVisitor.visitInsn(DUP);                                               //[..., list, list]
        methodVisitor.visitVarInsn(ALOAD, outerArrayLocalVariableIndex);            //[..., list, list, array]
        methodVisitor.visitInsn(ARRAYLENGTH);                                       //[..., list, list, array.length]
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);   //stack = 1
        methodVisitor.visitVarInsn(ASTORE, outerListLocalVariableIndex);            //[..., list]
        final LocalVariable list0 = new LocalVariable("list0", "Ljava/util/List;", "Ljava/util/List<" + typeSignature.toSignature() + ">;", label1, endLabel, outerListLocalVariableIndex);
        locals.add(list0);

        //setup int idx0
        //setup int size
        final int sizeLocalVariableIndex = localVariableIndex++;
        final int outerIndexLocalVariableIndex = localVariableIndex++;
        final Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ALOAD, outerArrayLocalVariableIndex);            //[..., list, array]
        methodVisitor.visitInsn(ARRAYLENGTH);                                       //[..., list, array.length]
        methodVisitor.visitVarInsn(ISTORE, sizeLocalVariableIndex);                 //[..., list]
        final LocalVariable size = new LocalVariable("size", "I", null, label2, endLabel, sizeLocalVariableIndex);
        locals.add(size);
        methodVisitor.visitInsn(ICONST_0);                                          //[..., list, idx=0]
        methodVisitor.visitVarInsn(ISTORE, outerIndexLocalVariableIndex);           //[..., list]
        final LocalVariable idx0 = new LocalVariable("idx0", "I", null, label2, endLabel, outerIndexLocalVariableIndex);
        locals.add(idx0);

        final Label jumpBackTarget = new Label();
        methodVisitor.visitLabel(jumpBackTarget);
        final Object[] localVariablesFrame = locals.frame();
        final int currentLocals = localVariablesFrame.length;
        methodVisitor.visitFrame(F_FULL, currentLocals, localVariablesFrame, 1, new Object[] {"java/util/List"});  //TODO could there be MORE items on the stack?
        methodVisitor.visitVarInsn(ILOAD, outerIndexLocalVariableIndex);            //[..., list, idx]
        methodVisitor.visitVarInsn(ILOAD, sizeLocalVariableIndex);                  //[..., list, idx, size]

        final Label conditionFalseLabel = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, conditionFalseLabel);                //[..., list]
        final int itemLocalVariableIndex = localVariableIndex++;    //TODO unnecessary?
        methodVisitor.visitVarInsn(ALOAD, outerArrayLocalVariableIndex);            //[..., list, array]
        methodVisitor.visitVarInsn(ILOAD, outerIndexLocalVariableIndex);            //[..., list, array, idx]
        //determine bytecode opcodes based on array component type
        final String arrayComponentDescriptor = typeSignature.getTypeArguments().get(0).toDescriptor();
        final String arrayComponentSignature = typeSignature.getTypeArguments().get(0).toSignature();
        methodVisitor.visitInsn(arrayLoad(arrayComponentDescriptor));               //[..., list, element]

        //convert array element
        final Label startBodyLabel = new Label();
        final Label endBodyLabel = new Label();
        methodVisitor.visitLabel(startBodyLabel);
        //TODO insert bytecode for converting the array component.
        //TODO toLiveType(...)
        //TODO for now, just no-op. (that means that this method will just work for arrays of reference types for now)
        final int bodyMaxStackIncrease = 0; //TODO
        //TODO take the stack into account as well!
        methodVisitor.visitLabel(endBodyLabel);                                     //[..., list, serialized(element)]

        //add to the list
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);      //[..., boolean]
        methodVisitor.visitInsn(POP);                                               //[...]
        //increment index
        methodVisitor.visitIincInsn(outerIndexLocalVariableIndex, 1);
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //we have arrived at the end of the array
        methodVisitor.visitLabel(conditionFalseLabel);                              //[..., list]
        locals.removeFramesFromIndex(currentLocals);
        methodVisitor.visitFrame(F_FULL, currentLocals, localVariablesFrame, 1, new Object[] {"java/util/List"}); //TODO there could be more elements!

        //load the list
        methodVisitor.visitVarInsn(ALOAD, outerListLocalVariableIndex);                 //stack = 1
        methodVisitor.visitLabel(endLabel);
        //continue execution


        stackLocal.increasedMaxStack += 3 + bodyMaxStackIncrease;
    }

    private static final int arrayLoad(String descriptor) {
        switch (descriptor) {
            case "B":
            case "Z":
                return BALOAD;
            case "S":
                return SALOAD;
            case "I":
                return IALOAD;
            case "C":
                return CALOAD;
            case "F":
                return FALOAD;
            case "D":
                return DALOAD;
            case "J":
                return LALOAD;
            default:
                return AALOAD;
        }
    }

    private static final int storeLocalVariable(String descriptor) {
        switch (descriptor) {
            case "B":
            case "S":
            case "I":
            case "Z":
            case "C":
                return ISTORE;
            case "F":
                return FSTORE;
            case "D":
                return DSTORE;
            case "J":
                return LSTORE;
            default:
                return ASTORE;
        }
    }

    static StackLocal toLiveType(MethodVisitor methodVisitor, String descriptor, String signature, int localVariableIndex, Label start, Label end, LocalVariableTable localVariables) {
        final StackLocal stackLocal = new StackLocal();

        TypeSignature typeSignature;
        if (signature != null) {
            typeSignature = TypeSignature.ofSignature(signature);
        } else {
            typeSignature = TypeSignature.ofDescriptor(descriptor);
        }

        if (!typeSignature.getTypeArguments().isEmpty()) {
            if (TypeSignature.ARRAY.equals(typeSignature.getTypeName())) {
                //generate code for transforming arrays to lists and their elements



            } else {
                //TODO generate code converting elements of collections.
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

            //TODO convert elements
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

    //TODO arrays of boxed primitives
    //TODO arrays of other reference types that I want to support out of the box:
    //TODO BigInteger, BigDecimal, String and UUID
    //TODO ACTUALLY - I think it's probably better to generate that bytecode in the classfile itself! (in case of nested arrays!)
    //TODO arrays of enums
    //TODO arrays of other configurationserializable types

}
