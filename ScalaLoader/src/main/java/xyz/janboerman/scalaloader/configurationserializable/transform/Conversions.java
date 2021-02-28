package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;  //explicitly import because there is also java.lang.reflect.Type
import static org.objectweb.asm.Opcodes.*;

import static xyz.janboerman.scalaloader.bytecode.AsmConstants.*;
import xyz.janboerman.scalaloader.bytecode.*;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

class Conversions {

    private Conversions() {}

    //TODO return void.
    static StackLocal toSerializedType(MethodVisitor methodVisitor, String descriptor, String signature, int localVariableIndex, Label start, Label end, LocalVariableTable localVariables, OperandStack operandStack) {
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
                operandStack.replaceTop(Integer_TYPE);
                break;
            case "J":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "toString", "(J)Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "F":
                methodVisitor.visitInsn(F2D); //convert float to double and fall-through to Double.valueOf(double)
                operandStack.replaceTop(Type.DOUBLE_TYPE);
            case "D":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                operandStack.replaceTop(Double_TYPE);
                break;
            case "C":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "toString", "(C)Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Z":
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                operandStack.replaceTop(Boolean_TYPE);
                break;

            //boxed primitives
            case "Ljava/lang/Byte;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "intValue", "()I", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                operandStack.replaceTop(Type.INT_TYPE);
                operandStack.replaceTop(Integer_TYPE);
                break;
            case "Ljava/lang/Short;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "intValue", "()I", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                operandStack.replaceTop(Type.INT_TYPE);
                operandStack.replaceTop(Integer_TYPE);
                break;
            case "Ljava/lang/Integer;":
                break;
            case "Ljava/lang/Long;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/lang/Float":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "doubleValue", "()D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                operandStack.replaceTop(Type.DOUBLE_TYPE);
                operandStack.replaceTop(Double_TYPE);
                break;
            case "Ljava/lang/Double;":
                break;
            case "Ljava/lang/Character;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/lang/Boolean;":
                break;

            //other reference types
            //String, List, Set and Map are a no-op (just like Integer, Boolean and Double)
            //the same holds for any type that implements ConfigurationSerializable

            case "Ljava/math/BigInteger;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/math/BigDecimal;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/util/UUID;":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/UUID", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;

            //in any other case: assume the type is ConfigurationSerializable and just no-op!
            default:
                //TODO insert a cast?
                //TODO should not be necessary, we are serializing, not deserializing.
                break;


            //TODO something like Date, DateFormat, Instant, LocalDateTime, other Time-api related things?
            //TODO Locale, CharSet?
        }

        return stackLocal;
    }

    private static void arrayToSerializedType(MethodVisitor methodVisitor, String descriptor, String signature, TypeSignature typeSignature, OperandStack operandStack, int localVariableIndex, Label start, Label end, LocalVariableTable locals) {
        assert TypeSignature.ARRAY.equals(typeSignature.getTypeName()) : "not an array";

        final Type ARRAY_TYPE = Type.getType(descriptor);

        final Label endLabel = new Label();                                                                                 //[..., array]

        //store the array as a local variable.
        final Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        final int outerArrayLocalVariableIndex = localVariableIndex++;
        methodVisitor.visitVarInsn(ASTORE, outerArrayLocalVariableIndex);           operandStack.pop();                     //[...]
        LocalVariable array0 = new LocalVariable("array0", descriptor, signature, label0, endLabel, outerArrayLocalVariableIndex);
        locals.add(array0);

        //store the list as a local variable, use ArrayList(int) constructor.
        final int outerListLocalVariableIndex = localVariableIndex++;
        final Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");              operandStack.push(ARRAYLIST_TYPE);                //[..., list]
        methodVisitor.visitInsn(DUP);                                               operandStack.push(ARRAYLIST_TYPE);              //[..., list, list]
        methodVisitor.visitVarInsn(ALOAD, outerArrayLocalVariableIndex);            operandStack.push(ARRAY_TYPE);    //[..., list, list, array]
        methodVisitor.visitInsn(ARRAYLENGTH);                                       operandStack.replaceTop(Type.INT_TYPE);         //[..., list, list, array.length]
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);   operandStack.pop(2);
        methodVisitor.visitVarInsn(ASTORE, outerListLocalVariableIndex);            operandStack.pop();
        final LocalVariable list0 = new LocalVariable("list0", "Ljava/util/List;", "Ljava/util/List<" + typeSignature.toSignature() + ">;", label1, endLabel, outerListLocalVariableIndex);
        locals.add(list0);

        //setup int idx0
        //setup int size
        final int sizeLocalVariableIndex = localVariableIndex++;
        final int outerIndexLocalVariableIndex = localVariableIndex++;
        final Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitVarInsn(ALOAD, outerArrayLocalVariableIndex);            operandStack.push(ARRAY_TYPE);                  //[..., list, array]
        methodVisitor.visitInsn(ARRAYLENGTH);                                       operandStack.replaceTop(Type.INT_TYPE);          //[..., list, array.length]
        methodVisitor.visitVarInsn(ISTORE, sizeLocalVariableIndex);                 operandStack.pop();                             //[..., list]
        final LocalVariable size = new LocalVariable("size", "I", null, label2, endLabel, sizeLocalVariableIndex);
        locals.add(size);
        methodVisitor.visitInsn(ICONST_0);                                          operandStack.push(Type.INT_TYPE);               //[..., list, idx=0]
        methodVisitor.visitVarInsn(ISTORE, outerIndexLocalVariableIndex);           operandStack.pop();                             //[..., list]
        final LocalVariable idx0 = new LocalVariable("idx0", "I", null, label2, endLabel, outerIndexLocalVariableIndex);
        locals.add(idx0);

        final Label jumpBackTarget = new Label();                                                                                   //[..., list]
        methodVisitor.visitLabel(jumpBackTarget);
        final Object[] localVariablesFrame = locals.frame();
        final int currentLocals = localVariablesFrame.length;
        methodVisitor.visitFrame(F_FULL, currentLocals, localVariablesFrame, operandStack.operandsSize(), operandStack.frame());
        methodVisitor.visitVarInsn(ILOAD, outerIndexLocalVariableIndex);            operandStack.push(Type.INT_TYPE);               //[..., list, idx]
        methodVisitor.visitVarInsn(ILOAD, sizeLocalVariableIndex);                  operandStack.push(Type.INT_TYPE);               //[..., list, idx, size]

        final Label conditionFalseLabel = new Label();
        methodVisitor.visitJumpInsn(IF_ICMPGE, conditionFalseLabel);                operandStack.pop(2);                    //[..., list]
        //no need to make a local variable for the current array element, just leave it on the stack! :)
        methodVisitor.visitVarInsn(ALOAD, outerArrayLocalVariableIndex);            operandStack.push(ARRAY_TYPE);                  //[..., list, array]
        methodVisitor.visitVarInsn(ILOAD, outerIndexLocalVariableIndex);            operandStack.push(Type.INT_TYPE);               //[..., list, array, idx]
        //determine bytecode opcodes based on array component type
        final String arrayComponentDescriptor = typeSignature.getTypeArguments().get(0).toDescriptor();
        final String arrayComponentSignature = typeSignature.getTypeArguments().get(0).toSignature();
        methodVisitor.visitInsn(arrayLoad(arrayComponentDescriptor));               operandStack.replaceTop(2, Type.getType(arrayComponentDescriptor));     //[..., list, element]

        //convert the array element
        final Label startBodyLabel = new Label();
        final Label endBodyLabel = new Label();
        methodVisitor.visitLabel(startBodyLabel);
        final StackLocal bodyStackLocal = toSerializedType(methodVisitor, arrayComponentDescriptor, arrayComponentSignature, localVariableIndex, startBodyLabel, endBodyLabel, locals, operandStack);
        @Deprecated final int bodyMaxStackIncrease = bodyStackLocal.increasedMaxStack;
        methodVisitor.visitLabel(endBodyLabel);                                                                                     //[..., list, serialized(element)]

        locals.removeFramesFromIndex(currentLocals);

        //add to the list
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);      operandStack.replaceTop(2, Type.BOOLEAN_TYPE);     //[..., boolean]
        methodVisitor.visitInsn(POP);                                               operandStack.pop();                             //[...]
        //increment index
        methodVisitor.visitIincInsn(outerIndexLocalVariableIndex, 1);
        //load the list again
        methodVisitor.visitVarInsn(ALOAD, outerListLocalVariableIndex);             operandStack.push(LIST_TYPE);                   //[..., list]
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //we have arrived at the end of the array
        methodVisitor.visitLabel(conditionFalseLabel);                                                                              //[..., list]
        methodVisitor.visitFrame(F_FULL, currentLocals, localVariablesFrame, operandStack.operandsSize(), operandStack.frame());

        //continue execution
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
    //TODO arrayStore

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
    //TODO loadLocalVariable

    //TODO return void instead of StackLocal
    static StackLocal toLiveType(MethodVisitor methodVisitor, String descriptor, String signature, int localVariableIndex, Label start, Label end, LocalVariableTable localVariables, OperandStack operandStack) {
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
                operandStack.replaceTop(Integer_TYPE);
                operandStack.replaceTop(Type.BYTE_TYPE);
                break;
            case "S":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "shortValue", "()S", false);
                operandStack.replaceTop(Integer_TYPE);
                operandStack.replaceTop(Type.SHORT_TYPE);
                break;
            case "I":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                operandStack.replaceTop(Integer_TYPE);
                operandStack.replaceTop(Type.INT_TYPE);
                break;
            case "J":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "parseLong", "(Ljava/lang/String;)J", false);
                operandStack.replaceTop(STRING_TYPE);
                operandStack.replaceTop(Type.LONG_TYPE);
                break;
            case "F":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "floatValue", "()F", false);
                operandStack.replaceTop(Double_TYPE);
                operandStack.replaceTop(Type.FLOAT_TYPE);
                break;
            case "D":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                operandStack.replaceTop(Double_TYPE);
                operandStack.replaceTop(Type.DOUBLE_TYPE);
                break;
            case "C":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitInsn(ICONST_0); stackLocal.increasedMaxStack += 1;   //TODO get rid of StackLocal
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
                operandStack.replaceTop(STRING_TYPE);
                operandStack.push(Type.INT_TYPE);
                operandStack.replaceTop(2, Type.CHAR_TYPE);
                break;
            case "Z":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                operandStack.replaceTop(Boolean_TYPE);
                operandStack.replaceTop(Type.BOOLEAN_TYPE);
                break;

            //boxed primitives
            case "Ljava/lang/Byte;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "byteValue", "()B", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                operandStack.replaceTop(Integer_TYPE);
                operandStack.replaceTop(Type.BYTE_TYPE);
                operandStack.replaceTop(Byte_TYPE);
                break;
            case "Ljava/lang/Short;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "shortValue", "()S", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                operandStack.replaceTop(Integer_TYPE);
                operandStack.replaceTop(Type.SHORT_TYPE);
                operandStack.replaceTop(Short_TYPE);
                break;
            case "Ljava/lang/Integer;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                operandStack.replaceTop(Integer_TYPE);
                break;
            case "Ljava/lang/Long;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(Ljava/lang/String;)Ljava/lang/Long;", false);
                operandStack.replaceTop(STRING_TYPE);
                operandStack.replaceTop(Long_TYPE);
                break;
            case "Ljava/lang/Float;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "floatValue", "()F", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                operandStack.replaceTop(Double_TYPE);
                operandStack.replaceTop(Type.FLOAT_TYPE);
                operandStack.replaceTop(Float_TYPE);
                break;
            case "Ljava/lang/Double;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                operandStack.replaceTop(Double_TYPE);
                break;
            case "Ljava/lang/Character":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitInsn(ICONST_0); stackLocal.increasedMaxStack += 1;   //TODO get rid of StackLocal
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                operandStack.replaceTop(STRING_TYPE);
                operandStack.push(Type.INT_TYPE);
                operandStack.replaceTop(2, Type.CHAR_TYPE);
                operandStack.replaceTop(Character_TYPE);
                break;
            case "Ljava/lang/Boolean;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                operandStack.replaceTop(Boolean_TYPE);
                break;

            //non-supported reference types
            case "Ljava/math/BigInteger;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                operandStack.replaceTop(STRING_TYPE);                                                               //stack: [..., string]
                methodVisitor.visitTypeInsn(NEW, "java/math/BigInteger");
                operandStack.push(BIGINTEGER_TYPE);                                                                 //stack: [..., string, biginteger]
                methodVisitor.visitInsn(DUP_X1);
                operandStack.pop(2);    operandStack.push(BIGINTEGER_TYPE, STRING_TYPE, BIGINTEGER_TYPE);   //stack: [..., biginteger, string, biginteger]
                methodVisitor.visitInsn(SWAP);
                operandStack.pop(2);    operandStack.push(BIGINTEGER_TYPE, STRING_TYPE);                    //stack: [..., biginteger, biginteger, string]
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V", false);
                operandStack.pop(2);                                                                        //stack: [..., biginteger]
                stackLocal.increasedMaxStack += 2;
                break;
            case "Ljava/math/BigDecimal;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                operandStack.replaceTop(STRING_TYPE);                                                               //stack: [..., string]
                methodVisitor.visitTypeInsn(NEW, "java/math/BigDecimal");
                operandStack.push(BIGDECIMAL_TYPE);                                                                 //stack: [..., string, bigdecimal]
                methodVisitor.visitInsn(DUP_X1);
                operandStack.pop(2);    operandStack.push(BIGDECIMAL_TYPE, STRING_TYPE, BIGDECIMAL_TYPE);   //stack: [..., bigdecimal, string, bigdecimal]
                methodVisitor.visitInsn(SWAP);
                operandStack.pop(2);    operandStack.push(BIGDECIMAL_TYPE, STRING_TYPE);                    //stack: [..., bigdecimal, bigdecimal, string]
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/math/BigDecimal", "<init>", "(Ljava/lang/String;)V", false);
                operandStack.pop(2);                                                                        //stack: [..., bigdecimal]
                stackLocal.increasedMaxStack += 2;
                break;
            case "Ljava/util/UUID;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/UUID", "fromString", "(Ljava/lang/String;)Ljava/util/UUID;", false);
                operandStack.replaceTop(STRING_TYPE);
                operandStack.replaceTop(UUID_TYPE);
                break;

            //supported reference types
            case "Ljava/lang/String;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                operandStack.replaceTop(STRING_TYPE);
                break;

            //TODO convert elements
            case "Ljava/util/List;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");
                operandStack.replaceTop(LIST_TYPE);
                break;
            case "Ljava/util/Set;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Set");
                operandStack.replaceTop(SET_TYPE);
                break;
            case "Ljava/util/Map;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
                operandStack.push(MAP_TYPE);
                break;

            default:
                //assume ConfigurationSerializable, just cast.
                Type type = Type.getType(descriptor);
                methodVisitor.visitTypeInsn(CHECKCAST, type.getInternalName());
                operandStack.replaceTop(type);
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
