package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;  //explicitly import because there is also java.lang.reflect.Type
import static org.objectweb.asm.Opcodes.*;

import static xyz.janboerman.scalaloader.bytecode.AsmConstants.*;
import xyz.janboerman.scalaloader.bytecode.*;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is NOT part of the public API!
 */
class Conversions {

    private Conversions() {}

    static void toSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, String descriptor, String signature, LocalVariableTable localVariables, OperandStack operandStack) {

        final TypeSignature typeSignature = signature != null ? TypeSignature.ofSignature(signature) : TypeSignature.ofDescriptor(descriptor);

        //detect arrays
        if (typeSignature.hasTypeArguments()) {
            if (typeSignature.isArray()) {
                //convert array to java.util.List.
                arrayToSerializedType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localVariables);
                return;
            } else if (isJavaUtilCollection(typeSignature, pluginClassLoader)) {
                //convert collection to ArrayList or LinkedHashSet
                collectionToSerializedType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localVariables);
                return;
            }
            /*TODO else if (isJavaUtilMap(typeSignature, pluginClassLoader)) {

            }*/
            //TODO else if (isScalaCollection(typeSignature, pluginClassLoader))
        }

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
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "intValue", "()I", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                operandStack.replaceTop(Type.INT_TYPE);
                operandStack.replaceTop(Integer_TYPE);
                break;
            case "Ljava/lang/Short;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Short");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "intValue", "()I", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                operandStack.replaceTop(Type.INT_TYPE);
                operandStack.replaceTop(Integer_TYPE);
                break;
            case "Ljava/lang/Integer;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                break;
            case "Ljava/lang/Long;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Long");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/lang/Float":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Float");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "doubleValue", "()D", false);
                methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                operandStack.replaceTop(Type.DOUBLE_TYPE);
                operandStack.replaceTop(Double_TYPE);
                break;
            case "Ljava/lang/Double;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Double");
                break;
            case "Ljava/lang/Character;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Character");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/lang/Boolean;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                break;

            //built-ins
            case "Ljava/math/BigInteger;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/math/BigInteger");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/math/BigDecimal;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/math/BigDecimal");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            case "Ljava/util/UUID;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/UUID");
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/UUID", "toString", "()Ljava/lang/String;", false);
                operandStack.replaceTop(STRING_TYPE);
                break;
            //TODO java.util.Date maybe? anything else?

            //unsupported type - attempt runtime conversion!
            default:
                //a java/lang/Object is already on top of the stack
                //which is nice because it is also the first argument of RuntimeConversions#serialize
                // :D
                genParameterType(methodVisitor, typeSignature, operandStack, localVariables);
                genScalaPluginClassLoader(methodVisitor, pluginClassLoader, operandStack, localVariables);
                methodVisitor.visitMethodInsn(INVOKESTATIC,
                        Type.getType(RuntimeConversions.class).getInternalName(),
                        "serialize",
                        "(" + OBJECT_TYPE.getDescriptor()
                                + Type.getType(ParameterType.class).getDescriptor()
                                + Type.getType(ScalaPluginClassLoader.class).getDescriptor()
                                + ")" + OBJECT_TYPE.getDescriptor(),
                        false);
                operandStack.replaceTop(3, OBJECT_TYPE);
                break;
        }

    }

    private static void arrayToSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature arrayTypeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {

        assert arrayTypeSignature.isArray() : "not an array";
        final TypeSignature componentTypeSignature = arrayTypeSignature.getTypeArgument(0);
        final Type arrayType = Type.getType(arrayTypeSignature.toDescriptor());
        final Type arrayComponentType = Type.getType(componentTypeSignature.toDescriptor());
        final TypeSignature serializedComponentTypeSignature = serializedType(componentTypeSignature);
        final Type serializedComponentType = Type.getType(serializedComponentTypeSignature.toDescriptor());

        int localVariableIndex = localVariableTable.frameSize();
        final Label start = new Label();
        final Label end = new Label();
        methodVisitor.visitLabel(start);

        //store array in local variable
        methodVisitor.visitTypeInsn(CHECKCAST, arrayType.getInternalName());        operandStack.replaceTop(arrayType);
        final int arrayIndex = localVariableIndex++;
        final LocalVariable array = new LocalVariable("array", arrayTypeSignature.toDescriptor(), arrayTypeSignature.toSignature(), start, end, arrayIndex);
        localVariableTable.add(array);
        methodVisitor.visitVarInsn(ASTORE, arrayIndex);                             operandStack.pop();

        //make list and store it in a local variable
        final int listIndex = localVariableIndex++;
        methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");                operandStack.push(ARRAYLIST_TYPE);
        methodVisitor.visitInsn(DUP);                                               operandStack.push(ARRAYLIST_TYPE);
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                              operandStack.push(arrayType);
        methodVisitor.visitInsn(ARRAYLENGTH);                                       operandStack.replaceTop(Type.INT_TYPE);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);    operandStack.pop(2);
        final LocalVariable list = new LocalVariable("list", "Ljava/util/List;", "Ljava/util/List<" + serializedComponentTypeSignature.toSignature() + ">;", start, end, listIndex);
        localVariableTable.add(list);
        methodVisitor.visitVarInsn(ASTORE, listIndex);                              operandStack.pop();

        //make size and index local variables, additionally create an extra local variable for the array!
        final int sameArrayIndex = localVariableIndex++;
        final int sizeIndex = localVariableIndex++;
        final int indexIndex = localVariableIndex++;
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                              operandStack.push(arrayType);
        final LocalVariable sameArray = new LocalVariable("sameArray", arrayTypeSignature.toDescriptor(), arrayTypeSignature.toSignature(), start, end, sameArrayIndex);
        localVariableTable.add(sameArray);
        methodVisitor.visitVarInsn(ASTORE, sameArrayIndex);                         operandStack.pop();
        methodVisitor.visitVarInsn(ALOAD, sameArrayIndex);                          operandStack.push(arrayType);
        methodVisitor.visitInsn(ARRAYLENGTH);                                       operandStack.replaceTop(Type.INT_TYPE);
        final LocalVariable size = new LocalVariable("size", "I", null, start, end, sizeIndex);
        localVariableTable.add(size);
        methodVisitor.visitVarInsn(ISTORE, sizeIndex);                              operandStack.pop();
        methodVisitor.visitInsn(ICONST_0);                                          operandStack.push(Type.INT_TYPE);
        final LocalVariable index = new LocalVariable("index", "I", null, start, end, indexIndex);
        localVariableTable.add(index);
        methodVisitor.visitVarInsn(ISTORE, indexIndex);                             operandStack.pop();

        //loop body
        final Label jumpBackTarget = new Label();
        final Label endOfLoopTarget = new Label();

        methodVisitor.visitLabel(jumpBackTarget);
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
        //compare index to size
        methodVisitor.visitVarInsn(ILOAD, indexIndex);                              operandStack.push(Type.INT_TYPE);
        methodVisitor.visitVarInsn(ILOAD, sizeIndex);                               operandStack.push(Type.INT_TYPE);
        methodVisitor.visitJumpInsn(IF_ICMPGE, endOfLoopTarget);                    operandStack.pop(2);
        //load the element
        methodVisitor.visitVarInsn(ALOAD, sameArrayIndex);                          operandStack.push(arrayType);
        methodVisitor.visitVarInsn(ILOAD, indexIndex);                              operandStack.push(Type.INT_TYPE);
        methodVisitor.visitInsn(arrayComponentType.getOpcode(IALOAD));              operandStack.replaceTop(2, arrayComponentType);

        //convert
        final int elementIndex = localVariableIndex++;
        final Label bodyStart = new Label();
        final Label bodyEnd = new Label();
        methodVisitor.visitLabel(bodyStart);
        toSerializedType(pluginClassLoader, methodVisitor, componentTypeSignature.toDescriptor(), componentTypeSignature.toSignature(), localVariableTable, operandStack);
        methodVisitor.visitLabel(bodyEnd);
        final LocalVariable element = new LocalVariable("element", serializedComponentTypeSignature.toDescriptor(), serializedComponentTypeSignature.toSignature(), jumpBackTarget, endOfLoopTarget, elementIndex);
        localVariableTable.add(element);
        methodVisitor.visitVarInsn(serializedComponentType.getOpcode(ISTORE), elementIndex);        operandStack.pop();

        //call list.add
        methodVisitor.visitVarInsn(ALOAD, listIndex);                               operandStack.push(LIST_TYPE);
        methodVisitor.visitVarInsn(ALOAD, elementIndex);                            operandStack.push(serializedComponentType);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);     operandStack.replaceTop(2, Type.BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);                                               operandStack.pop();

        //index++;
        methodVisitor.visitIincInsn(indexIndex, 1);
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        methodVisitor.visitLabel(endOfLoopTarget);
        localVariableTable.removeFramesFromIndex(elementIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the list again, and continue execution.
        methodVisitor.visitVarInsn(ALOAD, listIndex);                               operandStack.push(LIST_TYPE);
        methodVisitor.visitLabel(end);
        localVariableTable.removeFramesFromIndex(arrayIndex);
    }

    private static void collectionToSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {
        final String rawTypeName = typeSignature.getTypeName();
        final TypeSignature elementTypeSignature = typeSignature.getTypeArgument(0);
        int localVariableIndex = localVariableTable.frameSize();

        //serializing is a lot easier than deserializing.
        //if it's a set, we just need to create a LinkedHashSet,
        //otherwise we just create an ArrayList.
        //and we convert the elements!

        final Label startLabel = new Label();
        final Label endLabel = new Label();

        //store the existing collection in a local variable
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");                                                                             operandStack.replaceTop(Type.getType(Collection.class));
        final int oldCollectionIndex = localVariableIndex++;
        final LocalVariable oldCollection = new LocalVariable("liveCollection", "Ljava/util/Collection;", null, startLabel, endLabel, oldCollectionIndex);
        methodVisitor.visitVarInsn(ASTORE, oldCollectionIndex);                     localVariableTable.add(oldCollection);                              operandStack.pop();
        methodVisitor.visitLabel(startLabel);

        //store the new collection in a local variable
        switch (rawTypeName) {
            //set interfaces
            case "java/util/Set":
            case "java/util/NavigableSet":
            case "java/util/SortedSet":
            //set classes
            case "java/util/AbstractSet":
            case "java/util/concurrent/ConcurrentHashMap$KeySetView":
            case "java/util/concurrent/ConcurrentSkipListSet":
            case "java/util/concurrent/CopyOnWriteArraySet":
            case "java/util/EnumSet":
            case "java/util/HashSet":
            case "java/util/LinkedHashSet":
            case "java/util/TreeSet":
                methodVisitor.visitTypeInsn(NEW, "java/util/LinkedHashMap");                                                                        operandStack.push(Type.getType(LinkedHashMap.class));
                methodVisitor.visitInsn(DUP);                                                                                                           operandStack.push(Type.getType(LinkedHashMap.class));
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false);        operandStack.pop();
                break;
            //if it's not a set, then use a List.
            default:
                methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");                                                                            operandStack.push(Type.getType(ArrayList.class));
                methodVisitor.visitInsn(DUP);                                                                                                           operandStack.push(Type.getType(ArrayList.class));
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);            operandStack.pop();
                break;
        }

        final Label newCollectionLabel = new Label();
        final int newCollectionIndex = localVariableIndex++;
        final LocalVariable newCollection = new LocalVariable("serializedCollection", "Ljava/util/Collection;", null, newCollectionLabel, endLabel, newCollectionIndex);
        methodVisitor.visitVarInsn(ASTORE, newCollectionIndex);                     localVariableTable.add(newCollection);                              operandStack.pop();
        methodVisitor.visitLabel(newCollectionLabel);

        //get the iterator
        final Label iteratorLabel = new Label();
        methodVisitor.visitVarInsn(ALOAD, oldCollectionIndex);                                                                                          operandStack.push(Type.getType(Collection.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);      operandStack.replaceTop(Type.getType(Iterator.class));
        final int iteratorIndex = localVariableIndex++;
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex);
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                          localVariableTable.add(iterator);                                   operandStack.pop();
        methodVisitor.visitLabel(iteratorLabel);

        final Label jumpBackTarget = iteratorLabel, endLoopLabel = new Label();
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //get the iterator, call hasNext()
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);                   operandStack.replaceTop(Type.BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endLoopLabel);    /*IFEQ branches if the value on the stack is 0 (false) !!*/                                 operandStack.pop();
        //load the new collection so that we can sore later
        methodVisitor.visitVarInsn(ALOAD, newCollectionIndex);                                                                                          operandStack.push(Type.getType(Collection.class));
        //call iterator.next()
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);             operandStack.replaceTop(OBJECT_TYPE);
        //convert element
        toSerializedType(pluginClassLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localVariableTable, operandStack);
        //store in the new collection
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);       operandStack.replaceTop(2, Type.BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);                       /*discard boolean result of Collection#add(Object) !*/                                      operandStack.pop();
        //jump back
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        methodVisitor.visitLabel(endLoopLabel);
        localVariableTable.removeFramesFromIndex(localVariableIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the result
        methodVisitor.visitVarInsn(ALOAD, newCollectionIndex);                                                                                          operandStack.push(Type.getType(Collection.class));
        methodVisitor.visitLabel(endLabel);                                         localVariableTable.removeFramesFromIndex(oldCollectionIndex);
    }



    private static TypeSignature serializedType(TypeSignature liveType) {
        String internalName = liveType.getTypeName();
        List<TypeSignature> typeArguments = liveType.getTypeArguments();

        if (liveType.isArray())
            return new TypeSignature("java/util/List", typeArguments.stream()
                    .map(Conversions::serializedType)
                    .collect(Collectors.toList()));

        switch (internalName) {
            case "B": case "java/lang/Byte":
            case "S": case "java/lang/Short":
            case "I": case "java/lang/Integer":
                return new TypeSignature("java/lang/Integer", Compat.emptyList());
            case "F": case "java/lang/Float":
            case "D": case "java/lang/Double:":
                return new TypeSignature("java/lang/Double", Compat.emptyList());
            case "C": case "java/lang/Character":
            case "J": case "java/lang/Long":
                return new TypeSignature("java/lang/String", Compat.emptyList());
            case "Z": case "java/lang/Boolean":
                return new TypeSignature("java/lang/Boolean", Compat.emptyList());

            case "java/util/UUID":
            case "java/math/BigInteger":
            case "java/math/BigDecimal":
                return new TypeSignature("java/lang/String", Compat.emptyList());
        }

        return new TypeSignature(internalName, typeArguments.stream()
                .map(Conversions::serializedType)
                .collect(Collectors.toList()));
    }


    // ==================================================================================================================================================================

    static void toLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, String descriptor, String signature, LocalVariableTable localVariables, OperandStack operandStack) {

        final TypeSignature typeSignature = signature != null ? TypeSignature.ofSignature(signature) : TypeSignature.ofDescriptor(descriptor);

        if (typeSignature.hasTypeArguments()) {
            if (typeSignature.isArray()) {
                //generate code for transforming arrays to lists and their elements
                arrayToLiveType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localVariables);
                return;
            } else if (isJavaUtilCollection(typeSignature, pluginClassLoader)) {
                collectionToLiveType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localVariables);
                return;
            } //TODO else if Map
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
                methodVisitor.visitInsn(ICONST_0);
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
                methodVisitor.visitInsn(ICONST_0);
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

            //TODO remove this too when bytecode transformation for Maps is implemented.
            case "Ljava/util/Map;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");
                operandStack.push(MAP_TYPE);
                break;

            default:
                //a serialized java/lang/Object is already on top of the stack
                genParameterType(methodVisitor, typeSignature, operandStack, localVariables);
                genScalaPluginClassLoader(methodVisitor, pluginClassLoader, operandStack, localVariables);
                methodVisitor.visitMethodInsn(INVOKESTATIC,
                        Type.getType(RuntimeConversions.class).getInternalName(),
                        "deserialize",
                        "(" + OBJECT_TYPE.getDescriptor()
                                + Type.getType(ParameterType.class).getDescriptor()
                                + Type.getType(ScalaPluginClassLoader.class).getDescriptor()
                                + ")" + OBJECT_TYPE.getDescriptor(),
                        false);
                operandStack.replaceTop(3, OBJECT_TYPE);
                //now, just cast.
                methodVisitor.visitTypeInsn(CHECKCAST, typeSignature.internalName());
                break;
        }
    }

    private static void arrayToLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature arrayTypeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {

        assert arrayTypeSignature.isArray() : "not an array";

        final TypeSignature componentTypeSignature = arrayTypeSignature.getTypeArgument(0);
        final Type arrayType = Type.getType(arrayTypeSignature.toDescriptor());
        final Type componentType = Type.getType(componentTypeSignature.toDescriptor());
        final TypeSignature listTypeSignature = serializedType(arrayTypeSignature);

        int localVariableIndex = localVariableTable.frameSize();
        final Label start = new Label();
        final Label end = new Label();
        methodVisitor.visitLabel(start);

        //take operand on top of the stack, cast it to list, store it in a local variable
        final int listIndex = localVariableIndex++;
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");                       operandStack.replaceTop(LIST_TYPE);                                         //[..., list]
        final LocalVariable list = new LocalVariable("list", "Ljava/util/List;", listTypeSignature.toSignature(), start, end, listIndex);
        localVariableTable.add(list);
        methodVisitor.visitVarInsn(ASTORE, listIndex);                                  operandStack.pop();                                                             //[...]

        //get the size, instantiate a new array, store it in a local variable
        final int arrayIndex = localVariableIndex++;
        methodVisitor.visitVarInsn(ALOAD, listIndex);                                   operandStack.push(LIST_TYPE);                                                   //[..., list]
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);      operandStack.replaceTop(Type.INT_TYPE);     //[..., size]
        visitNewArray(arrayTypeSignature, methodVisitor);                               operandStack.replaceTop(arrayType);                                             //[..., array]
        final LocalVariable array = new LocalVariable("array", arrayTypeSignature.toDescriptor(), arrayTypeSignature.toSignature(), start, end, arrayIndex);
        localVariableTable.add(array);
        methodVisitor.visitVarInsn(ASTORE, arrayIndex);                                 operandStack.pop();                                                             //[...]

        //instantiate index
        final int indexIndex = localVariableIndex++;
        methodVisitor.visitInsn(ICONST_0);                                              operandStack.push(Type.INT_TYPE);                                               //[..., index]
        final LocalVariable index = new LocalVariable("index", "I", null, start, end, indexIndex);
        localVariableTable.add(index);
        methodVisitor.visitVarInsn(ISTORE, indexIndex);                                 operandStack.pop();                                                             //[...]

        //loop body
        final Label jumpBackTarget = new Label();
        final Label endOfLoopTarget = new Label();
        methodVisitor.visitLabel(jumpBackTarget);
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
        methodVisitor.visitVarInsn(ILOAD, indexIndex);                                  operandStack.push(Type.INT_TYPE);                                               //[..., index]
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                                  operandStack.push(arrayType);                                                   //[..., index, array]
        methodVisitor.visitInsn(ARRAYLENGTH);                                           operandStack.replaceTop(Type.INT_TYPE);                                         //[..., index, length]
        //if (index < array.length) continue loop body
        methodVisitor.visitJumpInsn(IF_ICMPGE, endOfLoopTarget);                        operandStack.pop(2);                                                    //[...]

        //prepare array and index so that we can use (B/S/I/J/Z/C/F/D/A)ASTORE later
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                                  operandStack.push(arrayType);                                                   //[..., array]
        methodVisitor.visitVarInsn(ILOAD, indexIndex);                                  operandStack.push(Type.INT_TYPE);                                               //[..., array, index]
        //call list.get(index)
        methodVisitor.visitVarInsn(ALOAD, listIndex);                                   operandStack.push(LIST_TYPE);                                                   //[..., array, index, list]
        methodVisitor.visitVarInsn(ILOAD, indexIndex);                                  operandStack.push(Type.INT_TYPE);                                               //[..., array, index, list, index]
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);         operandStack.replaceTop(2, OBJECT_TYPE);    //[..., array, index, object]

        //convert
        final Label bodyStart = new Label(), bodyEnd = new Label();
        methodVisitor.visitLabel(bodyStart);
        toLiveType(pluginClassLoader, methodVisitor, componentTypeSignature.toDescriptor(), componentTypeSignature.toSignature(), localVariableTable, operandStack);   //[..., array, index, element]
        methodVisitor.visitLabel(bodyEnd);
        //store in the array (that we loaded earlier before list.get)
        methodVisitor.visitInsn(componentType.getOpcode(IASTORE));                      operandStack.pop(3);                                                    //[...]

        //index++
        methodVisitor.visitIincInsn(indexIndex, 1);
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //(index < size) is no longer true
        methodVisitor.visitLabel(endOfLoopTarget);                                                                                                                      //[...]
        localVariableTable.removeFramesFromIndex(localVariableIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the array again, and continue execution
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                                  operandStack.push(arrayType);                                                   //[..., array]
        methodVisitor.visitLabel(end);
        localVariableTable.removeFramesFromIndex(listIndex);
    }

    private static void visitNewArray(TypeSignature theArrayType, MethodVisitor mv) {
        TypeSignature ofWhat = theArrayType.getTypeArgument(0);
        switch (ofWhat.getTypeName()) {
            case "B":   mv.visitIntInsn(NEWARRAY, T_BYTE);                      break;
            case "S":   mv.visitIntInsn(NEWARRAY, T_SHORT);                     break;
            case "I":   mv.visitIntInsn(NEWARRAY, T_INT);                       break;
            case "J":   mv.visitIntInsn(NEWARRAY, T_LONG);                      break;
            case "F":   mv.visitIntInsn(NEWARRAY, T_FLOAT);                     break;
            case "D":   mv.visitIntInsn(NEWARRAY, T_DOUBLE);                    break;
            case "Z":   mv.visitIntInsn(NEWARRAY, T_BOOLEAN);                   break;
            case "C":   mv.visitIntInsn(NEWARRAY, T_CHAR);                      break;
            default:    mv.visitTypeInsn(ANEWARRAY, ofWhat.internalName());      break;
        }
    }


    private static void collectionToLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {
        final String collectionTypeName = typeSignature.getTypeName();

        //determine implementation class for the live type.
        final String implementationClassName;
        switch (collectionTypeName) {
            //known interfaces
            case "java/util/concurrent/BlockingDeque":
                implementationClassName = "java/util/concurrent/LinkedBlockingDeque";
                break;
            case "java/util/concurrent/TransferQueue":
                implementationClassName = "java/util/concurrent/LinkedTransferQueue";
                break;
            case "java/util/concurrent/BlockingQueue":
                implementationClassName = "java/util/concurrent/LinkedBlockingQueue";
                break;
            case "java/util/Deque":
                implementationClassName = "java/util/ArrayDeque";
                break;
            case "java/util/Queue":
                implementationClassName = "java/util/LinkedList";
                break;
            case "java/util/SortedSet":
            case "java/util/NavigableSet":
                implementationClassName = "java/util/TreeSet";
                break;
            case "java/util/Set":
                implementationClassName = "java/util/LinkedHashSet";
                break;
            case "java/util/List":
            case "java/util/Collection":
                implementationClassName = "java/util/ArrayList";
                break;

            //it is probably was a class already.
            default:
                implementationClassName = collectionTypeName;
                break;
        }

        //now, let's generate some code!

        final TypeSignature elementTypeSignature = typeSignature.getTypeArgument(0);

        //assert that we have a collection indeed.
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");                                                                                                 operandStack.replaceTop(Type.getType(Collection.class));
        int localVariableIndex = localVariableTable.frameSize();
        final Label startLabel = new Label();
        final Label endLabel = new Label();

        //store it in a local variable
        final int serializedCollectionIndex = localVariableIndex++;
        final LocalVariable serializedCollection = new LocalVariable("serializedCollection", "Ljava/util/Collection;", null, startLabel, endLabel, serializedCollectionIndex);
        methodVisitor.visitVarInsn(ASTORE, serializedCollectionIndex);                                                  localVariableTable.add(serializedCollection);       operandStack.pop();

        final Label liveCollectionLabel = new Label();
        methodVisitor.visitLabel(liveCollectionLabel);

        //instantiate the new collection.
        if ("java/util/EnumSet".equals(implementationClassName)) {
            //special-case EnumSet because has no nullary constructor.
            methodVisitor.visitLdcInsn(Type.getType(elementTypeSignature.toDescriptor()));                                                                                      operandStack.push(Type.getType(Class.class));
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/EnumSet", "noneOf", "(Ljava/langClass;)Ljava/util/EnumSet;", false);     operandStack.replaceTop(Type.getType(EnumSet.class));
        } else {
            //call nullary constructor (this may fail at runtime with a NoSuchMethodDefError) //TODO generate try-catch code?
            methodVisitor.visitTypeInsn(NEW, implementationClassName);                                                                                                          operandStack.push(Type.getObjectType(collectionTypeName));
            methodVisitor.visitInsn(DUP);                                                                                                                                       operandStack.push(Type.getObjectType(collectionTypeName));
            methodVisitor.visitMethodInsn(INVOKESPECIAL, implementationClassName, "<init>", "()V", false);                                          operandStack.pop();
        }
        //store it in a local variable
        final int liveCollectionIndex = localVariableIndex++;
        final LocalVariable liveCollection = new LocalVariable("liveCollection", typeSignature.toDescriptor(), typeSignature.toSignature(), liveCollectionLabel, endLabel, liveCollectionIndex);
        methodVisitor.visitVarInsn(ASTORE, liveCollectionIndex);                                                        localVariableTable.add(liveCollection);                 operandStack.pop();

        final Label iteratorLabel = new Label();
        methodVisitor.visitLabel(iteratorLabel);

        //let's get the iterator and store it
        methodVisitor.visitVarInsn(ALOAD, serializedCollectionIndex);                                                                                                           operandStack.push(Type.getType(Collection.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);                     operandStack.replaceTop(Type.getType(Iterator.class));
        final int iteratorIndex = localVariableIndex++;
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex);
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                                                              localVariableTable.add(iterator);                       operandStack.pop();

        //loop body!
        final Label jumpBackTarget = new Label();
        final Label endOfLoopLabel = new Label();
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();

        methodVisitor.visitLabel(jumpBackTarget);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the iterator, call hasNext(), call next(), convert
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);   operandStack.replaceTop(Type.BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endOfLoopLabel);                                                              operandStack.pop();
        //prepare live collection so that we can add the element later
        methodVisitor.visitVarInsn(ALOAD, liveCollectionIndex);                                                         operandStack.push(Type.getObjectType(collectionTypeName));
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);     operandStack.replaceTop(OBJECT_TYPE);
        toLiveType(pluginClassLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localVariableTable, operandStack);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);       operandStack.replaceTop(2, Type.BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);   /*get rid of da boolean*/                                                       operandStack.pop();
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        localVariableTable.removeFramesFromIndex(localVariableIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitLabel(endOfLoopLabel);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the result
        methodVisitor.visitVarInsn(ALOAD, liveCollectionIndex);                                                         operandStack.push(Type.getObjectType(collectionTypeName));
        methodVisitor.visitLabel(endLabel);                                             localVariableTable.removeFramesFromIndex(serializedCollectionIndex);
    }


    // ==================================================================================================================================================================

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


    private static Map<Pair<String, ClassLoader>, Class<?>> knownCollectionClasses;
    private static boolean isJavaUtilCollection(TypeSignature typeSignature, ClassLoader pluginClassLoader) {

        final String typeName = typeSignature.getTypeName();

        //java standard library
        switch (typeName) {
            //interfaces
            case "java/util/Collection":
            case "java/util/concurrent/BlockingQueue":
            case "java/util/Deque":
            case "java/util/List":
            case "java/util/NavigableSet":
            case "java/util/Queue":
            case "java/util/Set":
            case "java/util/SortedSet":
            case "java/util/concurrent/TransferQueue":
            //classes
            case "java/util/AbstractCollection":
            case "java/util/AbstractList":
            case "java/util/AbstractQueue":
            case "java/util/AbstractSequentialList":
            case "java/util/AbstractSet":
            case "java/util/concurrent/ArrayBlockingQueue":
            case "java/util/ArrayDeque":
            case "java/util/ArrayList":
            case "java/util/concurrent/ConcurrentHashMap$KeySetView":
            case "java/util/concurrent/ConcurrentLinkedDeque":
            case "java/util/concurrent/ConcurrentLinkedQueue":
            case "java/util/concurrent/ConcurrentSkipListSet":
            case "java/util/concurrent/CopyOnWriteArrayList":
            case "java/util/concurrent/CopyOnWriteArraySet":
            case "java/util/concurrent/DelayQueue":
            case "java/util/EnumSet":
            case "java/util/HashSet":
            case "java/util/concurrent/LinkedBlockingDeque":
            case "java/util/concurrent/LinkedBlockingQueue":
            case "java/util/LinkedHashSet":
            case "java/util/LinkedList":
            case "java/util/concurrent/LinkedTransferQueue":
            case "java/util/concurrent/PriorityBlockingQueue":
            case "java/util/PriorityQueue":
            case "java/util/Stack":
            case "java/util/concurrent/SynchronousQueue":
            case "java/util/TreeSet":
            case "java/util/Vector":
                return true;
        }

        //fallback - try to classload the collection class and check whether it is assignable to java.util.Collection.
        if (knownCollectionClasses == null) knownCollectionClasses = new HashMap<>();
        final Pair<String, ClassLoader> pair = new Pair<>(typeSignature.getTypeName(), pluginClassLoader);
        if (knownCollectionClasses.containsKey(pair)) return true;

        String jvmClassName = typeName.replace('/', '.');
        try {
            Class<?> clazz = Class.forName(jvmClassName, false, pluginClassLoader);
            if (Collection.class.isAssignableFrom(clazz)) return true;
        } catch (ClassNotFoundException tooBad) {
            //plugin's jar contained a class that referred to a class that couldn't be found by its classloader.
            NoClassDefFoundError error = new NoClassDefFoundError(jvmClassName);
            error.addSuppressed(tooBad);
            throw error;
        }

        //if we reached this point, there is no hope.
        return false;
    }


    // ==================================================================================================================================================================

    private static void genScalaPluginClassLoader(MethodVisitor methodVisitor, ScalaPluginClassLoader plugin, OperandStack operandStack, LocalVariableTable localVariableTable) {
        String main = plugin.getMainClassName();
        Type mainType = Type.getType("L" + main.replace('.', '/') + ";");

        methodVisitor.visitLdcInsn(mainType);                                                                                                                   operandStack.push(Type.getType(Class.class));
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);      operandStack.replaceTop(Type.getType(ClassLoader.class));
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader");                                                     operandStack.replaceTop(Type.getType(ScalaPluginClassLoader.class));
    }

    private static void genParameterType(MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {
        //include annotations? would need to make typeSignature contain annotations in that case.

        if (typeSignature.isArray()) {
            //gen component conversion
            genParameterType(methodVisitor, typeSignature.getTypeArgument(0), operandStack, localVariableTable);
            //load 'false' (not var-args)
            methodVisitor.visitInsn(ICONST_0);                                                                                  operandStack.push(Type.BOOLEAN_TYPE);
            //invoke
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "xyz/janboerman/scalaloader/configurationserializable/runtime/ArrayParameterType",
                    "from",
                    "(Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;Z)Lxyz/janboerman/scalaloader/configurationserializable/runtime/ArrayParameterType;",
                    false);                                                                                                 operandStack.replaceTop(2, Type.getType(ArrayParameterType.class));
        } else if (typeSignature.hasTypeArguments()) {
            //load raw type
            methodVisitor.visitLdcInsn(Type.getObjectType(typeSignature.internalName()));                                       operandStack.push(Type.getType(Class.class));
            //load type arguments
            methodVisitor.visitIntInsn(BIPUSH, typeSignature.countTypeArguments());                                             operandStack.push(Type.INT_TYPE);
            methodVisitor.visitTypeInsn(ANEWARRAY, "xyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType");   operandStack.replaceTop(Type.getType(ParameterType[].class));
            for (int i = 0; i < typeSignature.countTypeArguments(); i++) {
                methodVisitor.visitInsn(DUP);                                                                                   operandStack.push(Type.getType(ParameterType[].class));
                TypeSignature typeArgument = typeSignature.getTypeArgument(i);
                methodVisitor.visitIntInsn(BIPUSH, i);                                                                          operandStack.push(Type.INT_TYPE);
                genParameterType(methodVisitor, typeArgument, operandStack, localVariableTable);
                methodVisitor.visitInsn(AASTORE);                                                                               operandStack.pop(2);
            }
            //invoke
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "xyz/janboerman/scalaloader/configurationserializable/runtime/ParameterizedParameterType",
                    "from",
                    "(Ljava/lang/Class;[Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;)Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterizedParameterType;",
                    false);                                                                                                 operandStack.replaceTop(2, Type.getType(ParameterizedParameterType.class));
        } else {
            //load raw type
            methodVisitor.visitLdcInsn(Type.getObjectType(typeSignature.internalName()));                                       operandStack.push(Type.getType(Class.class));
            //invoke
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "xyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType",
                    "from",
                    "(Ljava/lang/reflect/Type;)Lxyz/janboerman/scalaloader/configurationserializable/runtime/ParameterType;",
                    false);                                                                                             operandStack.replaceTop(Type.getType(ParameterType.class));
        }
    }

}
