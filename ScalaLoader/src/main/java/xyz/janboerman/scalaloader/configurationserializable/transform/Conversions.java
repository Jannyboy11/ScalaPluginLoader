package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;  //explicitly import because there is also java.lang.reflect.Type
import static org.objectweb.asm.Opcodes.*;

import static xyz.janboerman.scalaloader.bytecode.AsmConstants.*;
import xyz.janboerman.scalaloader.bytecode.*;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import xyz.janboerman.scalaloader.configurationserializable.runtime.types.NumericRange;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.util.ArrayOps;
import xyz.janboerman.scalaloader.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is NOT part of the public API!
 */
class Conversions {

    private Conversions() {}

    static void toSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, String descriptor, String signature, LocalCounter localCounter, LocalVariableTable localVariables, OperandStack operandStack) {

        final TypeSignature typeSignature = signature != null ? TypeSignature.ofSignature(signature) : TypeSignature.ofDescriptor(descriptor);

        if (typeSignature.hasTypeArguments()) {
            if (typeSignature.isArray()) {
                //convert array to java.util.List.
                arrayToSerializedType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                return;
            } else if (isJavaUtilCollection(typeSignature, pluginClassLoader)) {
                //convert collection to ArrayList or LinkedHashSet
                collectionToSerializedType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                return;
            } else if (isJavaUtilMap(typeSignature, pluginClassLoader)) {
                mapToSerializedType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                return;
            }
        }

        else if (ScalaConversions.isScalaCollection(typeSignature, pluginClassLoader)) {
            //some of the scala collections don't have type parameters.
            ScalaConversions.serializeCollection(pluginClassLoader, methodVisitor, typeSignature, localCounter, localVariables, operandStack);
            return;
        }
        /*TODO else if (isScalaMap(typeSignature, pluginClassLoader)) {

        }*/

        //TODO just like conversion for arrays (including: tuples, Option, Either)

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
                operandStack.replaceTop(Integer_TYPE);
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
            case "Ljava/lang/Void;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Void");
                operandStack.replaceTop(Void_TYPE);
                break;

            //supported reference types
            case "Ljava/lang/String;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                operandStack.replaceTop(STRING_TYPE);
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

            //unsupported type - attempt runtime serialization!
            default:
                //a java/lang/Object is already on top of the stack
                //which is nice because it is also the first argument of RuntimeConversions#serialize
                // :D
                genParameterType(methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                genScalaPluginClassLoader(methodVisitor, pluginClassLoader, operandStack, localCounter, localVariables);
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

    private static void arrayToSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature arrayTypeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {

        assert arrayTypeSignature.isArray() : "not an array";
        final TypeSignature componentTypeSignature = arrayTypeSignature.getTypeArgument(0);
        final Type arrayType = Type.getType(arrayTypeSignature.toDescriptor());
        final Type arrayComponentType = Type.getType(componentTypeSignature.toDescriptor());
        final TypeSignature serializedComponentTypeSignature = serializedType(componentTypeSignature);
        final Type serializedComponentType = Type.getType(serializedComponentTypeSignature.toDescriptor());

        final Label start = new Label();
        final Label end = new Label();
        methodVisitor.visitLabel(start);

        //store array in local variable
        methodVisitor.visitTypeInsn(CHECKCAST, arrayType.getInternalName());        operandStack.replaceTop(arrayType);
        final int arrayIndex = localCounter.getSlotIndex(), arrayFrameIndex = localCounter.getFrameIndex();
        final LocalVariable array = new LocalVariable("array", arrayTypeSignature.toDescriptor(), arrayTypeSignature.toSignature(), start, end, arrayIndex, arrayFrameIndex);
        localVariableTable.add(array); localCounter.add(Type.getType(arrayTypeSignature.toDescriptor()));
        methodVisitor.visitVarInsn(ASTORE, arrayIndex);                             operandStack.pop();

        //make list and store it in a local variable
        final int listIndex = localCounter.getSlotIndex(), listFrameIndex = localCounter.getFrameIndex();
        methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");                operandStack.push(ARRAYLIST_TYPE);
        methodVisitor.visitInsn(DUP);                                               operandStack.push(ARRAYLIST_TYPE);
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                              operandStack.push(arrayType);
        methodVisitor.visitInsn(ARRAYLENGTH);                                       operandStack.replaceTop(Type.INT_TYPE);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);    operandStack.pop(2);
        final LocalVariable list = new LocalVariable("list", "Ljava/util/List;", "Ljava/util/List<" + serializedComponentTypeSignature.toSignature() + ">;", start, end, listIndex, listFrameIndex);
        localVariableTable.add(list); localCounter.add(Type.getType(java.util.List.class));
        methodVisitor.visitVarInsn(ASTORE, listIndex);                              operandStack.pop();

        //make size and index local variables, additionally create an extra local variable for the array!
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                              operandStack.push(arrayType);
        final int sameArrayIndex = localCounter.getSlotIndex(), sameArrayFrameIndex = localCounter.getFrameIndex();
        final LocalVariable sameArray = new LocalVariable("sameArray", arrayTypeSignature.toDescriptor(), arrayTypeSignature.toSignature(), start, end, sameArrayIndex, sameArrayFrameIndex);
        localVariableTable.add(sameArray); localCounter.add(Type.getType(arrayTypeSignature.toDescriptor()));
        methodVisitor.visitVarInsn(ASTORE, sameArrayIndex);                         operandStack.pop();
        methodVisitor.visitVarInsn(ALOAD, sameArrayIndex);                          operandStack.push(arrayType);
        methodVisitor.visitInsn(ARRAYLENGTH);                                       operandStack.replaceTop(Type.INT_TYPE);
        final int sizeIndex = localCounter.getSlotIndex(), sizeFrameIndex = localCounter.getFrameIndex();
        final LocalVariable size = new LocalVariable("size", "I", null, start, end, sizeIndex, sizeFrameIndex);
        localVariableTable.add(size); localCounter.add(Type.INT_TYPE);
        methodVisitor.visitVarInsn(ISTORE, sizeIndex);                              operandStack.pop();
        methodVisitor.visitInsn(ICONST_0);                                          operandStack.push(Type.INT_TYPE);
        final int indexIndex = localCounter.getSlotIndex(), indexFrameIndex = localCounter.getFrameIndex();
        final LocalVariable index = new LocalVariable("index", "I", null, start, end, indexIndex, indexFrameIndex);
        localVariableTable.add(index); localCounter.add(Type.INT_TYPE);
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
        final Label bodyStart = new Label();
        final Label bodyEnd = new Label();
        methodVisitor.visitLabel(bodyStart);
        toSerializedType(pluginClassLoader, methodVisitor, componentTypeSignature.toDescriptor(), componentTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        methodVisitor.visitLabel(bodyEnd);
        final int elementIndex = localCounter.getSlotIndex(), elementFrameIndex = localCounter.getFrameIndex();
        final LocalVariable element = new LocalVariable("element", serializedComponentTypeSignature.toDescriptor(), serializedComponentTypeSignature.toSignature(), jumpBackTarget, endOfLoopTarget, elementIndex, elementFrameIndex);
        localVariableTable.add(element); localCounter.add(Type.getType(serializedComponentTypeSignature.toDescriptor()));
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
        localVariableTable.removeFramesFromIndex(elementFrameIndex);    localCounter.reset(elementIndex, elementFrameIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the list again, and continue execution.
        methodVisitor.visitVarInsn(ALOAD, listIndex);                               operandStack.push(LIST_TYPE);
        methodVisitor.visitLabel(end);
        localVariableTable.removeFramesFromIndex(arrayFrameIndex);      localCounter.reset(arrayIndex, arrayFrameIndex);
    }

    private static void collectionToSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {
        final String rawTypeName = typeSignature.getTypeName();
        final TypeSignature elementTypeSignature = typeSignature.getTypeArgument(0);

        //serializing is a lot easier than deserializing.
        //if it's a set, we just need to create a LinkedHashSet,
        //otherwise we just create an ArrayList.
        //and we convert the elements!

        final Label startLabel = new Label();
        final Label endLabel = new Label();

        //store the existing collection in a local variable
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");                                                                             operandStack.replaceTop(Type.getType(Collection.class));
        final int oldCollectionIndex = localCounter.getSlotIndex(), oldCollectionFrameIndex = localCounter.getFrameIndex();
        final LocalVariable oldCollection = new LocalVariable("liveCollection", "Ljava/util/Collection;", null, startLabel, endLabel, oldCollectionIndex, oldCollectionFrameIndex);
        methodVisitor.visitVarInsn(ASTORE, oldCollectionIndex); localVariableTable.add(oldCollection); localCounter.add(Type.getType(java.util.Collection.class)); operandStack.pop();
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
                methodVisitor.visitTypeInsn(NEW, "java/util/LinkedHashSet");                                                                        operandStack.push(Type.getType(LinkedHashMap.class));
                methodVisitor.visitInsn(DUP);                                                                                                           operandStack.push(Type.getType(LinkedHashMap.class));
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashSet", "<init>", "()V", false);        operandStack.pop();
                break;
            //if it's not a Set, then use a List.
            default:
                methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");                                                                            operandStack.push(Type.getType(ArrayList.class));
                methodVisitor.visitInsn(DUP);                                                                                                           operandStack.push(Type.getType(ArrayList.class));
                methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);            operandStack.pop();
                break;
        }

        final Label newCollectionLabel = new Label();
        final int newCollectionIndex = localCounter.getSlotIndex(), newCollectionFrameIndex = localCounter.getFrameIndex();
        final LocalVariable newCollection = new LocalVariable("serializedCollection", "Ljava/util/Collection;", null, newCollectionLabel, endLabel, newCollectionIndex, newCollectionFrameIndex);
        methodVisitor.visitVarInsn(ASTORE, newCollectionIndex);                     localVariableTable.add(newCollection);                              operandStack.pop();     localCounter.add(Type.getType(java.util.Collection.class));
        methodVisitor.visitLabel(newCollectionLabel);

        //get the iterator
        final Label iteratorLabel = new Label();
        methodVisitor.visitVarInsn(ALOAD, oldCollectionIndex);                                                                                          operandStack.push(Type.getType(Collection.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);      operandStack.replaceTop(Type.getType(Iterator.class));
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex, iteratorFrameIndex);
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                          localVariableTable.add(iterator);                                   operandStack.pop();     localCounter.add(Type.getType(java.util.Iterator.class));
        methodVisitor.visitLabel(iteratorLabel);

        final Label jumpBackTarget = iteratorLabel, endLoopLabel = new Label();     //jumpBackTarget label is already visited!
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //get the iterator, call hasNext()
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);                   operandStack.replaceTop(Type.BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endLoopLabel);    /*IFEQ branches if the value on the stack is 0 (false) !!*/                                 operandStack.pop();
        //load the new collection so that we can store later
        methodVisitor.visitVarInsn(ALOAD, newCollectionIndex);                                                                                          operandStack.push(Type.getType(Collection.class));
        //call iterator.next()
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);             operandStack.replaceTop(OBJECT_TYPE);
        //convert element
        toSerializedType(pluginClassLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        //store in the new collection
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);       operandStack.replaceTop(2, Type.BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);                       /*discard boolean result of Collection#add(Object) !*/                                      operandStack.pop();
        //jump back
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        methodVisitor.visitLabel(endLoopLabel);
        localVariableTable.removeFramesFromIndex(localCounter.getFrameIndex());
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the result
        methodVisitor.visitVarInsn(ALOAD, newCollectionIndex);                                                                                          operandStack.push(Type.getType(Collection.class));
        methodVisitor.visitLabel(endLabel);
        localVariableTable.removeFramesFromIndex(oldCollectionFrameIndex);  localCounter.reset(oldCollectionIndex, oldCollectionFrameIndex);
    }

    private static void mapToSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {
        final String rawTypeName = typeSignature.getTypeName();

        final TypeSignature keyTypeSignature = typeSignature.getTypeArgument(0);
        final TypeSignature valueTypeSignature = typeSignature.getTypeArgument(1);
        final Type keyType = Type.getObjectType(keyTypeSignature.internalName());
        final Type valueType = Type.getObjectType(valueTypeSignature.internalName());

        //strategy: just create a new LinkedHashMap and convert the elements!

        final Label startLabel = new Label(), endLabel = new Label();

        //store the existing map in a local variable
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");                operandStack.replaceTop(MAP_TYPE);
        final int oldMapIndex = localCounter.getSlotIndex(), oldMapFrameIndex = localCounter.getFrameIndex();
        final LocalVariable oldMap = new LocalVariable("liveMap", "Ljava/util/Map;", null, startLabel, endLabel, oldMapIndex, oldMapFrameIndex);
        localVariableTable.add(oldMap); localCounter.add(Type.getType(Map.class));
        methodVisitor.visitVarInsn(ASTORE, oldMapIndex);                            operandStack.pop();
        methodVisitor.visitLabel(startLabel);

        //create the new LinkedHashmap and store it in a local variable
        final Label newMapLabel = new Label();
        methodVisitor.visitTypeInsn(NEW, "java/util/LinkedHashMap");            operandStack.push(LINKEDHASHMAP_TYPE);
        methodVisitor.visitInsn(DUP);                                               operandStack.push(LINKEDHASHMAP_TYPE);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false);      operandStack.pop();
        final int newMapIndex = localCounter.getSlotIndex(), newMapFrameIndex = localCounter.getFrameIndex();
        final LocalVariable newMap = new LocalVariable("serializedMap", "Ljava/util/Map;", null, newMapLabel, endLabel, newMapIndex, newMapFrameIndex);
        localVariableTable.add(newMap); localCounter.add(Type.getType(java.util.Map.class));
        methodVisitor.visitVarInsn(ASTORE, newMapIndex);                            operandStack.pop();
        methodVisitor.visitLabel(newMapLabel);

        //get the entry set iterator
        final Label iteratorLabel = new Label();
        methodVisitor.visitVarInsn(ALOAD, oldMapIndex);                             operandStack.push(MAP_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, "entrySet", "()Ljava/util/Set;", true);    operandStack.replaceTop(SET_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, SET_NAME, "iterator", "()Ljava/util/Iterator;", true);       operandStack.replaceTop(ITERATOR_TYPE);
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex, iteratorFrameIndex);
        localVariableTable.add(iterator); localCounter.add(Type.getType(java.util.Iterator.class));
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                          operandStack.pop();
        methodVisitor.visitLabel(iteratorLabel);

        //prepare for loop
        final Label jumpBackTarget = iteratorLabel, endLoopLabel = new Label();     //jumpBackTarget label is already visited!
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //get the iterator, call hasNext
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                                                                                               operandStack.push(Type.getType(Iterator.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);                   operandStack.replaceTop(Type.BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endLoopLabel);    /*IFEQ branches if the value on the stack is 0 (false) !!*/                                 operandStack.pop();
        //load the iterator, call iterator.next(), cast to Map.Entry
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                           operandStack.push(ITERATOR_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);     operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, MAP$ENTRY_NAME);                     operandStack.replaceTop(MAP$ENTRY_TYPE);
        //store the entry in a new local variable
        final Label entryLabel = new Label();
        final int entryIndex = localCounter.getSlotIndex(), entryFrameIndex = localCounter.getFrameIndex();
        final LocalVariable entry = new LocalVariable("entry", "Ljava/util/Map$Entry;", null, jumpBackTarget, endLoopLabel, entryIndex, entryFrameIndex);
        localVariableTable.add(entry); localCounter.add(Type.getType(java.util.Map.Entry.class));
        methodVisitor.visitVarInsn(ASTORE, entryIndex);                             operandStack.pop();
        methodVisitor.visitLabel(entryLabel);

        //resultMap.put(serialize(entry.getKey()), serialize(entry.getValue()));
        //load resultMap
        methodVisitor.visitVarInsn(ALOAD, newMapIndex);                             operandStack.push(MAP_TYPE);
        //serialize(entry(getKey())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                              operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getKey", "()Ljava/lang/Object;", true);     operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, keyTypeSignature.internalName());    operandStack.replaceTop(keyType);
        toSerializedType(pluginClassLoader, methodVisitor, keyTypeSignature.toDescriptor(), keyTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        //serialize(entry(getValue())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                              operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getValue", "()Ljava/lang/Object;", true);   operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, valueTypeSignature.internalName());  operandStack.replaceTop(valueType);
        toSerializedType(pluginClassLoader, methodVisitor, valueTypeSignature.toDescriptor(), valueTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        //call resultMap.put
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_PUT_NAME, MAP_PUT_DESCRIPTOR, true);           operandStack.replaceTop(3, OBJECT_TYPE);
        methodVisitor.visitInsn(POP);   /*pop the result from resultMap.put (which is the old value for the key)*/      operandStack.pop();
        //go back to loop start.
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        methodVisitor.visitLabel(endLoopLabel);
        localVariableTable.removeFramesFromIndex(entryFrameIndex);  localCounter.reset(entryIndex, entryFrameIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the map containing the serialized keys and values
        methodVisitor.visitVarInsn(ALOAD, newMapIndex);                             operandStack.push(MAP_TYPE);
        localVariableTable.removeFramesFromIndex(oldMapFrameIndex); localCounter.reset(oldMapIndex, oldMapFrameIndex);
        methodVisitor.visitLabel(endLabel);
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

    static void toLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, String descriptor, String signature, LocalCounter localCounter, LocalVariableTable localVariables, OperandStack operandStack) {

        final TypeSignature typeSignature = signature != null ? TypeSignature.ofSignature(signature) : TypeSignature.ofDescriptor(descriptor);

        if (typeSignature.hasTypeArguments()) {
            if (typeSignature.isArray()) {
                //generate code for transforming arrays to lists and their elements
                arrayToLiveType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                return;
            } else if (isJavaUtilCollection(typeSignature, pluginClassLoader)) {
                collectionToLiveType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                return;
            } else if (isJavaUtilMap(typeSignature, pluginClassLoader)) {
                mapToLiveType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                return;
            }
        }

        else if (ScalaConversions.isScalaCollection(typeSignature, pluginClassLoader)) {
            ScalaConversions.deserializeCollection(pluginClassLoader, methodVisitor, typeSignature, localCounter, localVariables, operandStack);
            return;
        } //TODO else if Scala Map

        //TODO other Scala types (Option, Either, Tuples)
        //TODO Scala primitive wrappers (BigInt, BigDecimal, RichInt, RichFloat, etc.)

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
            case "Ljava/lang/Void;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Void");
                operandStack.replaceTop(Void_TYPE);
                break;

            //supported reference types
            case "Ljava/lang/String;":
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/String");
                operandStack.replaceTop(STRING_TYPE);
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

            //unsupported type - attempt runtime deserialization
            default:
                //a serialized java/lang/Object is already on top of the stack
                genParameterType(methodVisitor, typeSignature, operandStack, localCounter, localVariables);
                genScalaPluginClassLoader(methodVisitor, pluginClassLoader, operandStack, localCounter, localVariables);
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

    private static void arrayToLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature arrayTypeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {

        assert arrayTypeSignature.isArray() : "not an array";

        final TypeSignature componentTypeSignature = arrayTypeSignature.getTypeArgument(0);
        final Type arrayType = Type.getType(arrayTypeSignature.toDescriptor());
        final Type componentType = Type.getType(componentTypeSignature.toDescriptor());
        final TypeSignature listTypeSignature = serializedType(arrayTypeSignature);

        final Label start = new Label();
        final Label end = new Label();
        methodVisitor.visitLabel(start);

        //take operand on top of the stack, cast it to list, store it in a local variable
        final int listIndex = localCounter.getSlotIndex(), listFrameIndex = localCounter.getFrameIndex();
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");                       operandStack.replaceTop(LIST_TYPE);                                         //[..., list]
        final LocalVariable list = new LocalVariable("list", "Ljava/util/List;", listTypeSignature.toSignature(), start, end, listIndex, listFrameIndex);
        localVariableTable.add(list); localCounter.add(Type.getType(java.util.List.class));
        methodVisitor.visitVarInsn(ASTORE, listIndex);                                  operandStack.pop();                                                             //[...]

        //get the size, instantiate a new array, store it in a local variable
        final int arrayIndex = localCounter.getSlotIndex(), arrayFrameIndex = localCounter.getFrameIndex();
        methodVisitor.visitVarInsn(ALOAD, listIndex);                                   operandStack.push(LIST_TYPE);                                                   //[..., list]
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);      operandStack.replaceTop(Type.INT_TYPE);     //[..., size]
        visitNewArray(arrayTypeSignature, methodVisitor);                               operandStack.replaceTop(arrayType);                                             //[..., array]
        final LocalVariable array = new LocalVariable("array", arrayTypeSignature.toDescriptor(), arrayTypeSignature.toSignature(), start, end, arrayIndex, arrayFrameIndex);
        localVariableTable.add(array); localCounter.add(Type.getType(arrayTypeSignature.toDescriptor()));
        methodVisitor.visitVarInsn(ASTORE, arrayIndex);                                 operandStack.pop();                                                             //[...]

        //instantiate index
        final int indexIndex = localCounter.getSlotIndex(), indexFrameIndex = localCounter.getFrameIndex();
        methodVisitor.visitInsn(ICONST_0);                                              operandStack.push(Type.INT_TYPE);                                               //[..., index]
        final LocalVariable index = new LocalVariable("index", "I", null, start, end, indexIndex, indexFrameIndex);
        localVariableTable.add(index); localCounter.add(Type.INT_TYPE);
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
        toLiveType(pluginClassLoader, methodVisitor, componentTypeSignature.toDescriptor(), componentTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);   //[..., array, index, element]
        methodVisitor.visitLabel(bodyEnd);
        //store in the array (that we loaded earlier before list.get)
        methodVisitor.visitInsn(componentType.getOpcode(IASTORE));                      operandStack.pop(3);                                                    //[...]

        //index++
        methodVisitor.visitIincInsn(indexIndex, 1);
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //(index < size) is no longer true
        methodVisitor.visitLabel(endOfLoopTarget);                                                                                                                      //[...]
        localVariableTable.removeFramesFromIndex(localCounter.getFrameIndex());
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the array again, and continue execution
        methodVisitor.visitVarInsn(ALOAD, arrayIndex);                                  operandStack.push(arrayType);                                                   //[..., array]
        methodVisitor.visitLabel(end);
        localVariableTable.removeFramesFromIndex(listFrameIndex);   localCounter.reset(listIndex, listFrameIndex);
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


    private static void collectionToLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {
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

            //it probably was a class already.
            default:
                implementationClassName = collectionTypeName;
                break;
        }

        //now, let's generate some code!

        final TypeSignature elementTypeSignature = typeSignature.getTypeArgument(0);

        //assert that we have a collection indeed.
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Collection");                                                                                                 operandStack.replaceTop(Type.getType(Collection.class));
        final Label startLabel = new Label();
        final Label endLabel = new Label();
        methodVisitor.visitLabel(startLabel);

        //store it in a local variable
        final int serializedCollectionIndex = localCounter.getSlotIndex(), serializedCollectionFrameIndex = localCounter.getFrameIndex();
        final LocalVariable serializedCollection = new LocalVariable("serializedCollection", "Ljava/util/Collection;", null, startLabel, endLabel, serializedCollectionIndex, serializedCollectionFrameIndex);
        localVariableTable.add(serializedCollection);                       localCounter.add(Type.getType(java.util.Collection.class));                                     operandStack.pop();
        methodVisitor.visitVarInsn(ASTORE, serializedCollectionIndex);

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
        final int liveCollectionIndex = localCounter.getSlotIndex(), liveCollectionFrameIndex = localCounter.getFrameIndex();
        final LocalVariable liveCollection = new LocalVariable("liveCollection", typeSignature.toDescriptor(), typeSignature.toSignature(), liveCollectionLabel, endLabel, liveCollectionIndex, liveCollectionFrameIndex);
        localVariableTable.add(liveCollection);                             localCounter.add(Type.getType(typeSignature.toDescriptor()));                                       operandStack.pop();
        methodVisitor.visitVarInsn(ASTORE, liveCollectionIndex);

        final Label iteratorLabel = new Label();
        methodVisitor.visitLabel(iteratorLabel);

        //let's get the iterator and store it
        methodVisitor.visitVarInsn(ALOAD, serializedCollectionIndex);                                                                                                           operandStack.push(Type.getType(Collection.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);                     operandStack.replaceTop(Type.getType(Iterator.class));
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex, iteratorFrameIndex);
        localCounter.add(Type.getType(java.util.Iterator.class));                                   localVariableTable.add(iterator);                                          operandStack.pop();
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);

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
        toLiveType(pluginClassLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);       operandStack.replaceTop(2, Type.BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);   /*get rid of da boolean*/                                                       operandStack.pop();
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        localVariableTable.removeFramesFromIndex(localCounter.getFrameIndex());
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitLabel(endOfLoopLabel);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the result
        methodVisitor.visitVarInsn(ALOAD, liveCollectionIndex);                                                         operandStack.push(Type.getObjectType(collectionTypeName));
        methodVisitor.visitLabel(endLabel);
        localVariableTable.removeFramesFromIndex(serializedCollectionFrameIndex);    //the lowest index that we generated!
        localCounter.reset(serializedCollectionIndex, serializedCollectionFrameIndex);
    }

    private static void mapToLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {

        final String mapTypeName = typeSignature.getTypeName();

        //determine implementation class for the live type
        final String implementationClassName;
        switch (mapTypeName) {
            //known interfaces
            case "java/util/Map":
                implementationClassName = "java/util/LinkedHashMap";
                break;
            case "java/util/concurrent/ConcurrentMap":
                implementationClassName = "java/util/concurrent/ConcurrentHashMap";
                break;
            case "java/util/concurrent/ConcurrentNavigableMap":
                implementationClassName = "java/util/concurrent/ConcurrentSkipListMap";
                break;
            case "java/util/NavigableMap":
            case "java/util/SortedMap":
                implementationClassName = "java/util/TreeMap";
                break;

            //it probably was a class already.
            default:
                implementationClassName = mapTypeName;
                break;
        }

        //now, let's generate some code!

        final TypeSignature keyTypeSignature = typeSignature.getTypeArgument(0);
        final TypeSignature valueTypeSignature = typeSignature.getTypeArgument(1);
        final Type keyType = Type.getObjectType(keyTypeSignature.internalName());
        final Type valueType = Type.getObjectType(valueTypeSignature.internalName());

        //assert that we have a map indeed.
        methodVisitor.visitTypeInsn(CHECKCAST, MAP_NAME);                   operandStack.replaceTop(MAP_TYPE);
        final Label startlabel = new Label();
        final Label endLabel = new Label();
        methodVisitor.visitLabel(startlabel);

        //store it in a local variable
        final int serializedMapIndex = localCounter.getSlotIndex(), serializedMapFrameIndex = localCounter.getFrameIndex();
        final LocalVariable serializedMap = new LocalVariable("serializedMap", MAP_DESCRIPTOR, null, startlabel, endLabel, serializedMapIndex, serializedMapFrameIndex);
        localVariableTable.add(serializedMap); localCounter.add(Type.getType(java.util.Map.class));
        methodVisitor.visitVarInsn(ASTORE, serializedMapIndex);             operandStack.pop();

        //create the live map
        final Label liveMapLabel = new Label();
        methodVisitor.visitLabel(liveMapLabel);
        if ("java/util/EnumMap".equals(implementationClassName)) {
            //call the constructor that takes the enum class parameter
            methodVisitor.visitTypeInsn(NEW, "java/util/EnumMap");      operandStack.push(Type.getType(EnumMap.class));
            methodVisitor.visitInsn(DUP);                                   operandStack.push(Type.getType(EnumMap.class));
            methodVisitor.visitLdcInsn(keyType); /*assume key type is enum*/    operandStack.push(keyType);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/EnumMap", "<init>", "(Ljava/lang/Class;)V", false);      operandStack.pop(2);
        } else {
            //call the nullary constructor
            methodVisitor.visitTypeInsn(NEW, implementationClassName);      operandStack.push(Type.getObjectType(implementationClassName));
            methodVisitor.visitInsn(DUP);                                   operandStack.push(Type.getObjectType(implementationClassName));
            methodVisitor.visitMethodInsn(INVOKESPECIAL, implementationClassName, "<init>", "()V", false);                          operandStack.pop();
        }
        //store it in a local variable
        final int liveMapIndex = localCounter.getSlotIndex(), liveMapFrameIndex = localCounter.getFrameIndex();
        final LocalVariable liveMap = new LocalVariable("liveMap", "L" + implementationClassName + ";", null, liveMapLabel, endLabel, liveMapIndex, liveMapFrameIndex);
        localVariableTable.add(liveMap); localCounter.add(Type.getObjectType(implementationClassName));
        methodVisitor.visitVarInsn(ASTORE, liveMapIndex);                   operandStack.pop();

        //get the iterator and store it in a local variable
        final Label iteratorLabel = new Label();
        methodVisitor.visitLabel(iteratorLabel);
        methodVisitor.visitVarInsn(ALOAD, serializedMapIndex);              operandStack.push(MAP_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, "entrySet", "()Ljava/util/Set;", true);    operandStack.replaceTop(SET_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, SET_NAME, "iterator", "()Ljava/util/Iterator;", true);       operandStack.replaceTop(ITERATOR_TYPE);
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex, iteratorFrameIndex);
        localVariableTable.add(iterator); localCounter.add(Type.getType(java.util.Iterator.class));
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                  operandStack.pop();

        //loop body!
        final Label jumpBackTarget = new Label();
        final Label endOfLoopLabel = new Label();
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();

        methodVisitor.visitLabel(jumpBackTarget);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the iterator, call hasNext and if false jump to the loop end
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);               operandStack.push(ITERATOR_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);       operandStack.replaceTop(Type.BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endOfLoopLabel);              operandStack.pop();
        //call iterator.next, cast to Map.Entry and store it in a local variable
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);               operandStack.push(ITERATOR_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);     operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, MAP$ENTRY_NAME);         operandStack.replaceTop(MAP$ENTRY_TYPE);
        final int entryIndex = localCounter.getSlotIndex(), entryFrameIndex = localCounter.getFrameIndex();
        final LocalVariable entry = new LocalVariable("entry", "Ljava/util/Map$Entry;", null, jumpBackTarget, endOfLoopLabel, entryIndex, entryFrameIndex);
        localVariableTable.add(entry); localCounter.add(Type.getType(java.util.Map.Entry.class));
        methodVisitor.visitVarInsn(ASTORE, entryIndex);                 operandStack.pop();
        //prepare live map so that we can call .put on it later!
        methodVisitor.visitVarInsn(ALOAD, liveMapIndex);                operandStack.push(MAP_TYPE);
        //deserialize(entry.getKey())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                  operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getKey", "()Ljava/lang/Object;", true);    operandStack.replaceTop(OBJECT_TYPE);
        toLiveType(pluginClassLoader, methodVisitor, keyTypeSignature.toDescriptor(), keyTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        //deserialize(entry.getValue())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                  operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getValue", "()Ljava/lang/Object;", true);       operandStack.replaceTop(OBJECT_TYPE);
        toLiveType(pluginClassLoader, methodVisitor, valueTypeSignature.toDescriptor(), valueTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        //call liveMap.put
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_PUT_NAME, MAP_PUT_DESCRIPTOR, true);                           operandStack.replaceTop(3, OBJECT_TYPE);
        methodVisitor.visitInsn(POP);   /*get rid of the old value in the map*/     operandStack.pop();
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        localVariableTable.removeFramesFromIndex(entryFrameIndex);      localCounter.reset(entryIndex, entryFrameIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitLabel(endOfLoopLabel);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the result
        methodVisitor.visitVarInsn(ALOAD, liveMapIndex);                operandStack.push(Type.getObjectType(implementationClassName));
        methodVisitor.visitLabel(endLabel);
        localVariableTable.removeFramesFromIndex(serializedMapFrameIndex);   //the lowest index that we generated!
        localCounter.reset(serializedMapIndex, serializedMapFrameIndex);
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

    // ==================================================================================================================================================================


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
        final Pair<String, ClassLoader> pair = new Pair<>(typeName, pluginClassLoader);
        if (knownCollectionClasses.containsKey(pair)) return true;

        String jvmClassName = typeName.replace('/', '.');
        try {
            Class<?> clazz = Class.forName(jvmClassName, false, pluginClassLoader);
            if (Collection.class.isAssignableFrom(clazz)) {
                knownCollectionClasses.put(pair, clazz);
                return true;
            }
        } catch (ClassNotFoundException tooBad) {
            //plugin's jar contained a class that referred to a class that couldn't be found by its classloader.
            NoClassDefFoundError error = new NoClassDefFoundError(jvmClassName);
            error.addSuppressed(tooBad);
            throw error;
        }

        //if we reached this point, there is no more hope.
        return false;
    }


    private static Map<Pair<String, ClassLoader>, Class<?>> knownMapClasses;
    private static boolean isJavaUtilMap(TypeSignature typeSignature, ClassLoader pluginClassLoader) {

        final String typeName = typeSignature.getTypeName();

        switch (typeName) {
            //interfaces
            case "java/util/Map":
            case "java/util/concurrent/ConcurrentMap":
            case "java/util/concurrent/ConcurrentNavigableMap":
            case "java/util/NavigableMap":
            case "java/util/SortedMap":
            //classes
            case "java/util/AbstractMap":
            case "java/util/concurrent/ConcurrentHashMap":
            case "java/util/concurrent/ConcurrentSkipListMap":
            case "java/util/EnumMap":
            case "java/util/HashMap":
            case "java/util/Hashtable":
            case "java/util/IdentityHashMap":
            case "java/util/LinkedHashMap":
            case "java/util/Properties":
            case "java/util/TreeMap":
            case "java/util/WeakHashMap":
                return true;
        }

        //fallback, try to classload the map class and check whether it is assignable to java.util.Map.
        if (knownMapClasses == null) knownMapClasses = new HashMap<>();
        final Pair<String, ClassLoader> pair = new Pair<>(typeName, pluginClassLoader);
        if (knownMapClasses.containsKey(pair)) return true;

        String jvmClassName = typeName.replace('/', '.');
        try {
            Class<?> clazz = Class.forName(jvmClassName, false, pluginClassLoader);
            if (Map.class.isAssignableFrom(clazz)) {
                knownMapClasses.put(pair, clazz);
                return true;
            }
        } catch (ClassNotFoundException tooBad) {
            //plugin's jar contained a class that referred to a class that couldn't be found by its classloader.
            NoClassDefFoundError error = new NoClassDefFoundError(jvmClassName);
            error.addSuppressed(tooBad);
            throw error;
        }

        //if we reached this point, there is no more hope.
        return false;
    }


    // ==================================================================================================================================================================

    private static void genScalaPluginClassLoader(MethodVisitor methodVisitor, ScalaPluginClassLoader plugin, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {
        String main = plugin.getMainClassName();
        Type mainType = Type.getType("L" + main.replace('.', '/') + ";");

        methodVisitor.visitLdcInsn(mainType);                                                                                                                   operandStack.push(Type.getType(Class.class));
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);      operandStack.replaceTop(Type.getType(ClassLoader.class));
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader");                                                     operandStack.replaceTop(Type.getType(ScalaPluginClassLoader.class));
    }

    private static void genParameterType(MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalCounter localCounter, LocalVariableTable localVariableTable) {
        //include annotations? would need to make typeSignature contain annotations in that case.

        if (typeSignature.isArray()) {
            //gen component conversion
            genParameterType(methodVisitor, typeSignature.getTypeArgument(0), operandStack, localCounter, localVariableTable);
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
                genParameterType(methodVisitor, typeArgument, operandStack, localCounter, localVariableTable);
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

class ScalaConversions {

    private static final String STRING = STRING_TYPE.getInternalName();
    private static final String STRING_DESCRIPTOR = STRING_TYPE.getDescriptor();

    private static final String CLASSTAG = "scala/reflect/ClassTag";
    private static final String CLASSTAG_COMPANION = "scala/reflect/ClassTag$";
    private static final String CLASSTAG_DESCRIPTOR = 'L' + CLASSTAG + ';';
    private static final String CLASSTAG_COMPANION_DESCRIPTOR = 'L' + CLASSTAG_COMPANION + ';';
    private static final String MODULE$ = "MODULE$";

    private static final String BYTE_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$ByteManifest;";
    private static final String SHORT_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$ShortManifest;";
    private static final String INT_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$IntManifest;";
    private static final String LONG_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$LongManifest;";
    private static final String FLOAT_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$FloatManifest;";
    private static final String DOUBLE_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$DoubleManifest;";
    private static final String CHAR_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$CharManifest;";
    private static final String BOOLEAN_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$BooleanManifest;";
    private static final String UNIT_TAG_DESCRIPTOR = "Lscala/reflect/ManifestFactory$UnitManifest;";
    private static final String NOTHING_TAG_DESCRIPTOR = CLASSTAG_DESCRIPTOR;
    private static final String NULL_TAG_DESCRIPTOR = CLASSTAG_DESCRIPTOR;
    private static final String ANY_TAG_DESCRIPTOR = CLASSTAG_DESCRIPTOR;
    private static final String ANYREF_TAG_DESCRIPTOR = CLASSTAG_DESCRIPTOR;
    private static final String ANYVAL_TAG_DESCRIPTOR = CLASSTAG_DESCRIPTOR;
    private static final String OBJECT_TAG_DESCRIPTOR = CLASSTAG_DESCRIPTOR;

    private static final Type CLASSTAG_TYPE = Type.getObjectType(CLASSTAG);
    private static final Type CLASSTAG_COMPANION_TYPE = Type.getObjectType(CLASSTAG_COMPANION);

    private static final String ITERABLE = "scala/collection/Iterable";
    private static final String ITERATOR = "scala/collection/Iterator";
    private static final Type ITERABLE_TYPE = Type.getObjectType(ITERABLE);
    private static final Type ITERATOR_TYPE = Type.getObjectType(ITERATOR);
    private static final String ITERABLE_DESCRIPTOR = ITERABLE_TYPE.getDescriptor();
    private static final String ITERATOR_DESCRIPTOR = ITERATOR_TYPE.getDescriptor();

    private static final String BUILDER = "scala/collection/mutable/Builder";
    private static final Type BUILDER_TYPE = Type.getObjectType(BUILDER);
    private static final String ORDERING = "scala/math/Ordering";
    private static final Type ORDERING_TYPE = Type.getObjectType(ORDERING);
    private static final String GROWABLE = "scala/collection/mutable/Growable";
    private static final Type GROWABLE_TYPE = Type.getObjectType(GROWABLE);

    private static final String WRAPPED_STRING = "scala/collection/immutable/WrappedString";
    private static final Type WRAPPED_STRING_TYPE = Type.getObjectType(WRAPPED_STRING);
    private static final String WRAPPED_STRING_DESCRIPTOR = "L" + WRAPPED_STRING + ";";
    private static final String RANGE = "scala/collection/immutable/Range";
    private static final Type RANGE_TYPE = Type.getObjectType(RANGE);
    private static final String RANGE_DESCRIPTOR = RANGE_TYPE.getDescriptor();
    private static final String NUMERIC_RANGE = "scala/collection/immutable/NumericRange";
    private static final Type NUMERIC_RANGE_TYPE = Type.getObjectType(NUMERIC_RANGE);
    private static final String NUMERIC_RANGE_DESCRIPTOR = NUMERIC_RANGE_TYPE.getDescriptor();

    private static final String ARRAY_BUILDER = "scala/collection/mutable/ArrayBuilder";
    private static final Type ARRAY_BUILDER_TYPE = Type.getObjectType(ARRAY_BUILDER);
    private static final String ARRAY_BUILDER_DESCRIPTOR = ARRAY_BUILDER_TYPE.getDescriptor();
    //TODO ArraySeq (both immutable and mutable). Do I even want ArrayBuilder? because it isn't really a collection in itself.

    private static final Type SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE = Type.getType(NumericRange.OfInteger.class);
    private static final String SCALALOADER_NUMERICRANGE_OFINTEGER = SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE.getInternalName();
    private static final String SCALALOADER_NUMERICRANGE_OFINTEGER_DESCRIPTOR = SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE.getDescriptor();
    private static final String RANGE_COMPANION = RANGE + "$";
    private static final String RANGE_COMPANION_DESCRIPTOR = "L" + RANGE_COMPANION + ";";
    private static final Type RANGE_COMPANION_TYPE = Type.getObjectType(RANGE_COMPANION);
    private static final String RANGE_INCLUSIVE = "scala/collection/immutable/Range$Inclusive";
    private static final String RANGE_EXCLUSIVE = "scala/collection/immutable/Range$Exclusive";
    private static final String RANGE_INCLUSIVE_DESCRIPTOR = "L" + RANGE_INCLUSIVE + ";";
    private static final String RANGE_EXCLUSIVE_DESCRIPTOR = "L" + RANGE_EXCLUSIVE + ";";

    private static final String BIGINT = "scala/math/BigInt";
    private static final Type BIGINT_TYPE = Type.getObjectType(BIGINT);
    private static final String BIGINT_DESCRIPTOR = BIGINT_TYPE.getDescriptor();
    private static final Type SCALALOADER_NUMERICRANGE_TYPE = Type.getType(NumericRange.class);
    private static final String SCALALOADER_NUMERICRANGE = SCALALOADER_NUMERICRANGE_TYPE.getInternalName();
    private static final String SCALALOADER_NUMERICRANGE_DESCRIPTOR = SCALALOADER_NUMERICRANGE_TYPE.getDescriptor();
    private static final Type SCALALOADER_NUMERICRANGE_OFBYTE_TYPE = Type.getType(NumericRange.OfByte.class);
    private static final String SCALALOADER_NUMERICRANGE_OFBYTE = SCALALOADER_NUMERICRANGE_OFBYTE_TYPE.getInternalName();
    private static final String SCALALOADER_NUMERICRANGE_OFBYTE_DESCRIPTOR = SCALALOADER_NUMERICRANGE_OFBYTE_TYPE.getDescriptor();
    private static final Type SCALALOADER_NUMERICRANGE_OFSHORT_TYPE = Type.getType(NumericRange.OfShort.class);
    private static final String SCALALOADER_NUMERICRANGE_OFSHORT = SCALALOADER_NUMERICRANGE_OFSHORT_TYPE.getInternalName();
    private static final String SCALALOADER_NUMERICRANGE_OFSHORT_DESCRIPTOR = SCALALOADER_NUMERICRANGE_OFSHORT_TYPE.getDescriptor();
    private static final Type SCALALOADER_NUMERICRANGE_OFLONG_TYPE = Type.getType(NumericRange.OfLong.class);
    private static final String SCALALOADER_NUMERICRANGE_OFLONG = SCALALOADER_NUMERICRANGE_OFLONG_TYPE.getInternalName();
    private static final String SCALALOADER_NUMERICRANGE_OFLONG_DESCRIPTOR = SCALALOADER_NUMERICRANGE_OFLONG_TYPE.getDescriptor();
    private static final Type SCALALOADER_NUMERICRANGE_OFBIGINTEGER_TYPE = Type.getType(NumericRange.OfBigInteger.class);
    private static final String SCALALOADER_NUMERICRANGE_OFBIGINTEGER = SCALALOADER_NUMERICRANGE_OFBIGINTEGER_TYPE.getInternalName();
    private static final String SCALALOADER_NUMERICRANGE_OFBIGINTEGER_DESCRIPTOR = SCALALOADER_NUMERICRANGE_OFBIGINTEGER_TYPE.getDescriptor();

    private static final String NUMERIC = "scala/math/Numeric";
    private static final String INTEGRAL = "scala/math/Integral";


    private ScalaConversions() {
    }

    //TODO code generation for instances of scala.math.Ordering:
    //TODO https://www.scala-lang.org/api/current/scala/math/Ordering$.html
    //TODO if it's not one of the built-ins, try to get them from the companion object of the element type.
    //TODO might have to load the class, scan for fields. #funzies.
    //TODO take Scala 3 derivation into account! How does that compile to bytecode? probably an instance in the companion object.
    //TODO take into account that instances are not necessarily vals or no-args methods, they themselves could take implicit parameters!
    //TODO test this!

    static boolean isScalaCollection(final TypeSignature typeSignature, final ClassLoader pluginClassLoader) {
        final String typeName = typeSignature.internalName();

        switch (typeName) {
            //scala.collection
            case "scala/collection/AbstractIterable":
            case "scala/collection/AbstractSeq":
            case "scala/collection/AbstractSet":
            case "scala/collection/BitSet":
            case "scala/collection/IndexedSeq":
            case "scala/collection/Iterable":
            case "scala/collection/IterableOnce":
                //don't include LazyZip2, LazyZip3, LazyZip4
            case "scala/collection/LinearSeq":
            case "scala/collection/Seq":
            case "scala/collection/Set":
            case "scala/collection/SortedSet":

            //scala.collection.immutable
            case "scala/collection/immutable/AbstractSeq":
            case "scala/collection/immutable/AbstractSet":
            case "scala/collection/immutable/ArraySeq":
            case "scala/collection/immutable/BitSet":
            case "scala/collection/immutable/HashSet":
            case "scala/collection/immutable/IndexedSeq":
            case "scala/collection/immutable/Iterable":
            case "scala/collection/immutable/LazyList":
            case "scala/collection/immutable/LinearSeq":
            case "scala/collection/immutable/List":
            case "scala/collection/immutable/ListSet":
            case "scala/collection/immutable/NumericRange":
            case "scala/collection/immutable/Queue":
            case "scala/collection/immutable/Range":
            case "scala/collection/immutable/Seq":
            case "scala/collection/immutable/Set":
            case "scala/collection/immutable/SortedSet":
            case "scala/collection/immutable/Stream": //deprecated since 2.13
            case "scala/collection/immutable/TreeSet":
            case "scala/collection/immutable/Vector":
            case "scala/collection/immutable/WrappedString":

            //scala.collection.mutable
            case "scala/collection/mutable/AbstractBuffer":
            case "scala/collection/mutable/AbstractIterable":
            case "scala/collection/mutable/AbstractSeq":
            case "scala/collection/mutable/AbstractSet":
            case "scala/collection/mutable/ArrayBuffer":
            case "scala/collection/mutable/ArrayBuilder":
            case "scala/collection/mutable/ArrayDeque":
            case "scala/collection/mutable/ArraySeq":
            case "scala/collection/mutable/BitSet":
            case "scala/collection/mutable/Buffer":     //TODO do I want to include these?
            case "scala/collection/mutable/Builder":    //TODO do I want to include these?
                //don't include Growable
            case "scala/collection/mutable/HashSet":
                //don't include ImmutableBuilder
            case "scala/collection/mutable/IndexedBuffer":
            case "scala/collection/mutable/IndexedSeq":
            case "scala/collection/mutable/Iterable":
            case "scala/collection/mutable/LinkedHashSet":
            case "scala/collection/mutable/ListBuffer":
            case "scala/collection/mutable/PriorityQueue":
            case "scala/collection/mutable/Queue":
            case "scala/collection/mutable/Seq":
            case "scala/collection/mutable/Set":
            case "scala/collection/mutable/SortedSet":
            case "scala/collection/mutable/Stack":
            case "scala/collection/mutable/StringBuilder":
            case "scala/collection/mutable/TreeSet":
            case "scala/collection/mutable/UnrolledBuffer":

            //scala.collection.concurrent only contains maps.
                return true;
        }

        //not one of the built-ins: try to class-load
        try {
            Class<?> daClass = Class.forName(typeName.replace('/', '.'), false, pluginClassLoader);
            Class<?> seqClass = Class.forName("scala.collection.Seq", false, pluginClassLoader);
            Class<?> setClass = Class.forName("scala.collection.Set", false, pluginClassLoader);
            //both immutable.Seq and mutable.Seq inherit from collection.Seq, and similar for Set.
            if (seqClass.isAssignableFrom(daClass) || setClass.isAssignableFrom(daClass)) {
                return true;
            }
        } catch (ClassNotFoundException scalaPluginDoesNotDependOnScalaLibrary) {
            //for now, just do nothing. this is unreachable.
            //maybe in the future we allow scala plugins that don't use the standard library?
        }

        return false;
    }

    static boolean isScalaMap(final TypeSignature typeSignature, final ClassLoader pluginClassLoader) {
        final String typeName = typeSignature.getTypeName();

        switch (typeName) {
            //scala.collection
            case "scala/collection/AbstractMap":
            case "scala/collection/DefaultMap":
            case "scala/collection/Map":
            case "scala/collection/SortedMap":

            //scala.collection.immutable
            case "scala/collection/immutable/AbstractMap":
            case "scala/collection/immutable/HashMap":
            case "scala/collection/immutable/IntMap":
            case "scala/collection/immutable/ListMap":
            case "scala/collection/immutable/LongMap":
            case "scala/collection/immutable/Map":
            case "scala/collection/immutable/SeqMap":
            case "scala/collection/immutable/SortedMap":
            case "scala/collection/immutable/TreeMap":
            case "scala/collection/immutable/TreeSeqMap":
            case "scala/collection/immutable/VectorMap":

            //scala.collection.mutable
            case "scala/collection/mutable/AbstractMap":
            case "scala/collection/mutable/AnyRefMap":
            case "scala/collection/mutable/HashMap":
            case "scala/collection/mutable/LinkedHashMap":
            case "scala/collection/mutable/LongMap":
            case "scala/collection/mutable/Map":
            case "scala/collection/mutable/MultiMap":
            case "scala/collection/mutable/OpenHashMap":
            case "scala/collection/mutable/SeqMap":
            case "scala/collection/mutable/SortedMap":
            case "scala/collection/mutable/TreeMap":
            case "scala/collection/mutable/WeakHashMap":    //bit weird to have this case, but okay.

            //scala.collection.concurrent
            case "scala/collection/concurrent/Map":
            case "scala/collection/concurrent/TrieMap":

                return true;
        }

        //try to class-load
        try {
            Class<?> daClass = Class.forName(typeName.replace('/', '.'), false, pluginClassLoader);
            Class<?> mapClass = Class.forName("scala.collection.Map", false, pluginClassLoader);
            //both immutable.Map and mutable.Map inherit from collection.Map.
            if (mapClass.isAssignableFrom(daClass)) {
                return true;
            }
        } catch (ClassNotFoundException scalaPluginDoesNotDependOnScalaLibrary) {
            //for now, just do nothing. this is unreachable.
            //maybe in the future we allow scala plugins that don't use the standard library?
        }

        return false;
    }



    //immutable collections

    static void serializeCollection(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, LocalCounter localCounter, LocalVariableTable localVariableTable, OperandStack operandStack) {
        //this is really a best effort.
        //the standard library may evolve again in 3.1 or 3.2
        //but for now this method is compatible the 2.12 and 2.13 (and thus 3.0) standard library
        //I hope this will continue to work.

        //special-case some special collections
        switch (typeSignature.getTypeName()) {
            case WRAPPED_STRING:
                //serialize a WrappedString simply into a String
                Label startLabel = new Label(), endLabel = new Label();

                methodVisitor.visitTypeInsn(CHECKCAST, WRAPPED_STRING);     operandStack.replaceTop(WRAPPED_STRING_TYPE);
                //unwrap is not an instance method on WrappedString, instead, it is an extension method!!
                //methodVisitor.visitMethodInsn(INVOKEVIRTUAL, WRAPPED_STRING, "unwrap", "()" + STRING_DESCRIPTOR, false);    operandStack.replaceTop(STRING_TYPE);

                //WrappedString wrappedString = $valueOnTopOfTheStack
                final int wrappedStringIndex = localCounter.getSlotIndex(), wrappedStringFrameIndex = localCounter.getFrameIndex();
                LocalVariable wrappedString = new LocalVariable("wrappedString", WRAPPED_STRING_DESCRIPTOR, null, startLabel, endLabel, wrappedStringIndex, wrappedStringFrameIndex);
                localVariableTable.add(wrappedString); localCounter.add(WRAPPED_STRING_TYPE);
                methodVisitor.visitVarInsn(ASTORE, wrappedStringIndex);     operandStack.pop();
                methodVisitor.visitLabel(startLabel);

                //wrappedString.unwrap()
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/collection/immutable/WrappedString$UnwrapOp$", "MODULE$", "Lscala/collection/immutable/WrappedString$UnwrapOp$;");
                operandStack.push(Type.getType("Lscala/collection/immutable/WrappedString$UnwrapOp$;"));    //[..., UnwrapOp$]
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/collection/immutable/WrappedString$", "MODULE$", "Lscala/collection/immutable/WrappedString$;");
                operandStack.push(Type.getType("Lscala/collection/immutable/WrappedString$;"));             //[..., UnwrapOp$, WrappedString$]
                methodVisitor.visitVarInsn(ALOAD, wrappedStringIndex);
                operandStack.push(WRAPPED_STRING_TYPE);                                                     //[..., UnWrapOp$, WrappedString$, WrappedString]
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/collection/immutable/WrappedString$", "UnwrapOp", "(Lscala/collection/immutable/WrappedString;)Lscala/collection/immutable/WrappedString;", false);
                operandStack.replaceTop(2, WRAPPED_STRING_TYPE);                                //[..., UnwrapOp$, WrappedString]
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/collection/immutable/WrappedString$UnwrapOp$", "unwrap$extension", "(Lscala/collection/immutable/WrappedString;)Ljava/lang/String;", false);
                operandStack.replaceTop(2, STRING_TYPE);                                        //[..., String]
                //this seems needlessly complicated - do we need the UnwrapOp method? it does not seem to do anything besides consuming the WrappedString$ companion object.
                //this is however what the Scala 2.13.6 compiler emits!

                methodVisitor.visitLabel(endLabel);
                localVariableTable.removeFramesFromIndex(wrappedStringFrameIndex); localCounter.reset(wrappedStringIndex, wrappedStringFrameIndex);
                return;

            case RANGE:
                methodVisitor.visitTypeInsn(CHECKCAST, RANGE);              operandStack.replaceTop(RANGE_TYPE);
                //let's store it in a local variable
                final Label rangeStartLabel = new Label();
                final Label rangeEndLabel = new Label();

                final int rangeIndex = localCounter.getSlotIndex(), rangeFrameIndex = localCounter.getFrameIndex();
                final LocalVariable range = new LocalVariable("range", RANGE_DESCRIPTOR, null, rangeStartLabel, rangeEndLabel, rangeIndex, rangeFrameIndex);
                methodVisitor.visitVarInsn(ASTORE, rangeIndex);             operandStack.pop();
                localVariableTable.add(range);      localCounter.add(RANGE_TYPE);
                methodVisitor.visitLabel(rangeStartLabel);

                //load a new NumericRange.OfInteger instance
                methodVisitor.visitTypeInsn(NEW, SCALALOADER_NUMERICRANGE_OFINTEGER);       operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitInsn(DUP);                                               operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                //call range.start()
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);              operandStack.push(RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, RANGE, "start", "()I", false);     operandStack.replaceTop(Type.INT_TYPE);
                //call range.step()
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);              operandStack.push(RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, RANGE, "step", "()I", false);      operandStack.replaceTop(Type.INT_TYPE);
                //call range.end()
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);              operandStack.push(RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, RANGE, "end", "()I", false);       operandStack.replaceTop(Type.INT_TYPE);
                //call range.isInclusive()
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);              operandStack.push(RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, RANGE, "isInclusive", "()Z", false);           operandStack.replaceTop(Type.BOOLEAN_TYPE);
                //call new NumericRange.OfInteger(start, step, end, inclusive)
                methodVisitor.visitMethodInsn(INVOKESPECIAL, SCALALOADER_NUMERICRANGE_OFINTEGER, "<init>", "(IIIZ)V", false);   operandStack.pop(5);

                methodVisitor.visitLabel(rangeEndLabel);
                localVariableTable.removeFramesFromIndex(rangeFrameIndex);  localCounter.reset(rangeIndex, rangeFrameIndex);
                return;

            case NUMERIC_RANGE:
                methodVisitor.visitTypeInsn(CHECKCAST, NUMERIC_RANGE);      operandStack.replaceTop(NUMERIC_RANGE_TYPE);
                //let's store it in a local variable
                final Label numericRangeStartLabel = new Label(), numericRangeEndLabel = new Label();

                final int numericRangeIndex = localCounter.getSlotIndex(), numericRangeFrameIndex = localCounter.getFrameIndex();
                final LocalVariable numericRange = new LocalVariable("numericRange", NUMERIC_RANGE_DESCRIPTOR, null, numericRangeStartLabel, numericRangeEndLabel, numericRangeIndex, numericRangeFrameIndex);
                methodVisitor.visitVarInsn(ASTORE, numericRangeIndex);      operandStack.pop();
                localVariableTable.add(numericRange);   localCounter.add(NUMERIC_RANGE_TYPE);
                methodVisitor.visitLabel(numericRangeStartLabel);

                //frames :D
                final Object[] localsFrame = localVariableTable.frame();
                final Object[] stackFrame = operandStack.frame();

                //do some best-effort check on the type of 'start'
                final Label byteLabel = new Label(), shortLabel = new Label(), integerLabel = new Label(), longLabel = new Label(), bigIntLabel = new Label();
                //TODO make sure all of these labels are visited!

                //Byte
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "Ljava/lang/Object;", false);  operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(INSTANCEOF, "java/lang/Byte");                                          operandStack.replaceTop(Type.BOOLEAN_TYPE);
                methodVisitor.visitJumpInsn(IFNE, byteLabel);                                                               operandStack.pop();
                //Short
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start","Ljava/lang/Object;", false);   operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(INSTANCEOF, "java/lang/Short");                                         operandStack.replaceTop(Type.BOOLEAN_TYPE);
                methodVisitor.visitJumpInsn(IFNE, shortLabel);                                                              operandStack.pop();
                //Integer
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "Ljava/lang/Object;", false);  operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(INSTANCEOF, "java/lang/Integer");                                       operandStack.replaceTop(Type.BOOLEAN_TYPE);
                methodVisitor.visitJumpInsn(IFNE, integerLabel);                                                            operandStack.pop();
                //Long
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "Ljava/lang/Object;", false);  operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(INSTANCEOF, "java/lang/Long");                                          operandStack.replaceTop(Type.BOOLEAN_TYPE);
                methodVisitor.visitJumpInsn(IFNE, longLabel);                                                               operandStack.pop();
                //BigInt
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "Ljava/lang/Object;", false);  operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(INSTANCEOF, BIGINT);                                                        operandStack.replaceTop(BIGINT_TYPE);
                methodVisitor.visitJumpInsn(IFNE, bigIntLabel);                                                             operandStack.pop();

                assert Arrays.equals(localsFrame, localVariableTable.frame());
                assert Arrays.equals(stackFrame, operandStack.frame());

                final Label joinLabel = new Label();

                //Byte
                methodVisitor.visitLabel(byteLabel);
                methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
                //start, end, and step are all java.lang.Byte objects
                //call the xyz.janboerman.scalaloader.configurationserialiable.runtime.types.NumericRange.OfByte constructor!
                methodVisitor.visitTypeInsn(NEW, SCALALOADER_NUMERICRANGE_OFBYTE);                                          operandStack.push(SCALALOADER_NUMERICRANGE_OFBYTE_TYPE);
                methodVisitor.visitInsn(DUP);                                                                               operandStack.push(SCALALOADER_NUMERICRANGE_OFBYTE_TYPE);
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Byte");                                           operandStack.replaceTop(Type.getType(Byte.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "step", "()Ljava/lang/Object;", false);     operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Byte");                                           operandStack.replaceTop(Type.getType(Byte.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "end", "()Ljava/lang/Object;", false);      operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Byte");                                           operandStack.replaceTop(Type.getType(Byte.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "isInclusive", "()Z", false);               operandStack.replaceTop(Type.BOOLEAN_TYPE);
                //invoke constructor, upcast to scalaloader.NumericRange, and jump to the join label
                methodVisitor.visitMethodInsn(INVOKESPECIAL, SCALALOADER_NUMERICRANGE_OFBYTE, "<init>", "(Ljava/lang/Byte;Ljava/lang/Byte;Ljava/lang/Byte;Z)V", false);         operandStack.pop(5);
                methodVisitor.visitTypeInsn(CHECKCAST, SCALALOADER_NUMERICRANGE);                                           operandStack.replaceTop(SCALALOADER_NUMERICRANGE_TYPE);
                methodVisitor.visitJumpInsn(GOTO, joinLabel);

                //Short
                methodVisitor.visitLabel(shortLabel);
                methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
                //start, end and step are all java.lang.Short objects
                //call the xyz.janboerman.scalaloader.configurationserializable.runtime.types.NumericRange.OfShort constructor!
                methodVisitor.visitTypeInsn(NEW, SCALALOADER_NUMERICRANGE_OFSHORT);                                         operandStack.push(SCALALOADER_NUMERICRANGE_OFSHORT_TYPE);
                methodVisitor.visitInsn(DUP);                                                                               operandStack.push(SCALALOADER_NUMERICRANGE_OFSHORT_TYPE);
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Short");                                          operandStack.replaceTop(Type.getType(Short.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "step", "()Ljava/lang/Object;", false);     operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Short");                                          operandStack.replaceTop(Type.getType(Short.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "end", "()Ljava/lang/Object;", false);      operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Short");                                          operandStack.replaceTop(Type.getType(Short.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "isInclusive", "()Z", false);               operandStack.replaceTop(Type.BOOLEAN_TYPE);
                //invoke constructor, upcast to scalaloader.NumericRange, and jump to the join label
                methodVisitor.visitMethodInsn(INVOKESPECIAL, SCALALOADER_NUMERICRANGE_OFSHORT, "<init>", "(Ljava/lang/Short;Ljava/lang/Short;Ljava/lang/Short;Z)V", false);     operandStack.pop(5);
                methodVisitor.visitTypeInsn(CHECKCAST, SCALALOADER_NUMERICRANGE);                                           operandStack.replaceTop(SCALALOADER_NUMERICRANGE_TYPE);
                methodVisitor.visitJumpInsn(GOTO, joinLabel);

                //Integer
                methodVisitor.visitLabel(integerLabel);
                methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
                //start, end and step are all java.lang.Integer objects
                //call the xyz.janboerman.scalaloader.configurationserializable.runtime.types.NumericRange.OfInteger constructor!
                methodVisitor.visitTypeInsn(NEW, SCALALOADER_NUMERICRANGE_OFINTEGER);                                       operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitInsn(DUP);                                                                               operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");                                            operandStack.replaceTop(Type.getType(Integer.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "step", "()Ljava/lang/Object;", false);     operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");                                            operandStack.replaceTop(Type.getType(Integer.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "end", "()Ljava/lang/Object;", false);      operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Integer");                                            operandStack.replaceTop(Type.getType(Integer.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "isInclusive", "()Z", false);               operandStack.replaceTop(Type.BOOLEAN_TYPE);
                //invoke constructor, upcast to scalaloader.NumericRange, and jump to the join label
                methodVisitor.visitMethodInsn(INVOKESPECIAL, SCALALOADER_NUMERICRANGE_OFINTEGER, "<init>", "(Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;Z)V", false); operandStack.pop(5);
                methodVisitor.visitTypeInsn(CHECKCAST, SCALALOADER_NUMERICRANGE);                                           operandStack.replaceTop(SCALALOADER_NUMERICRANGE_TYPE);
                methodVisitor.visitJumpInsn(GOTO, joinLabel);

                //Long
                methodVisitor.visitLabel(longLabel);
                methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
                //start, end and step are all java.lang.Long objects
                //call the xyz.janboerman.scalaloader.configurationserializable.runtime.types.NumericRange.OfLong constructor!
                methodVisitor.visitTypeInsn(NEW, SCALALOADER_NUMERICRANGE_OFLONG);                                          operandStack.push(SCALALOADER_NUMERICRANGE_OFLONG_TYPE);
                methodVisitor.visitInsn(DUP);                                                                               operandStack.push(SCALALOADER_NUMERICRANGE_OFLONG_TYPE);
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Long");                                            operandStack.replaceTop(Type.getType(Long.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "step", "()Ljava/lang/Object;", false);     operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Long");                                            operandStack.replaceTop(Type.getType(Long.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "end", "()Ljava/lang/Object;", false);      operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, "java/lang/Long");                                            operandStack.replaceTop(Type.getType(Long.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "isInclusive", "()Z", false);               operandStack.replaceTop(Type.BOOLEAN_TYPE);
                //invoke constructor, upcast to scalaloader.NumericRange, and jump to the join label
                methodVisitor.visitMethodInsn(INVOKESPECIAL, SCALALOADER_NUMERICRANGE_OFLONG, "<init>", "(Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Long;Z)V", false);     operandStack.pop(5);
                methodVisitor.visitTypeInsn(CHECKCAST, SCALALOADER_NUMERICRANGE);                                           operandStack.replaceTop(SCALALOADER_NUMERICRANGE_TYPE);
                methodVisitor.visitJumpInsn(GOTO, joinLabel);

                //BigInt
                methodVisitor.visitLabel(longLabel);
                methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);
                //start, end and step are all scala.math.BigInt objects
                //call the xyz.janboerman.scalaloader.configurationserializable.runtime.types.NumericRange.OfBigInteger constructor!
                methodVisitor.visitTypeInsn(NEW, SCALALOADER_NUMERICRANGE_OFBIGINTEGER);                                    operandStack.push(SCALALOADER_NUMERICRANGE_OFBIGINTEGER_TYPE);
                methodVisitor.visitInsn(DUP);                                                                               operandStack.push(SCALALOADER_NUMERICRANGE_OFBIGINTEGER_TYPE);
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "start", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, BIGINT);                                                             operandStack.replaceTop(BIGINT_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, BIGINT, "bigInteger", "()Ljava/math/BigInteger;", false);  operandStack.replaceTop(Type.getType(java.math.BigInteger.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "step", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, BIGINT);                                                             operandStack.replaceTop(BIGINT_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, BIGINT, "bigInteger", "()Ljava/math/BigInteger;", false);  operandStack.replaceTop(Type.getType(java.math.BigInteger.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "end", "()Ljava/lang/Object;", false);    operandStack.replaceTop(OBJECT_TYPE);
                methodVisitor.visitTypeInsn(CHECKCAST, BIGINT);                                                             operandStack.replaceTop(BIGINT_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, BIGINT, "bigInteger", "()Ljava/math/BigInteger;", false);  operandStack.replaceTop(Type.getType(java.math.BigInteger.class));
                methodVisitor.visitVarInsn(ALOAD, numericRangeIndex);                                                       operandStack.push(NUMERIC_RANGE_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, NUMERIC_RANGE, "isInclusive", "()Z", false);               operandStack.replaceTop(Type.BOOLEAN_TYPE);
                //invoke constructor, upcast to scalaloader.NumericRange, and jump to the join label
                methodVisitor.visitMethodInsn(INVOKESPECIAL, SCALALOADER_NUMERICRANGE_OFBIGINTEGER, "<init>", "(Ljava/math/BigInteger;Ljava/math/BigInteger;Ljava/math/BigInteger;)V", false);   operandStack.pop(5);
                methodVisitor.visitJumpInsn(GOTO, joinLabel);


                final Object[] newStackFrame = ArrayOps.append(stackFrame, SCALALOADER_NUMERICRANGE);
                assert Arrays.equals(localsFrame, localVariableTable.frame());
                assert Arrays.equals(newStackFrame, operandStack.frame());
                methodVisitor.visitLabel(joinLabel);
                methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, newStackFrame.length, newStackFrame);

                //TODO anything else to do here? at least I need to implmeent the deserialization code still.

                methodVisitor.visitLabel(numericRangeEndLabel);
                localVariableTable.removeFramesFromIndex(numericRangeFrameIndex);   localCounter.reset(numericRangeIndex, numericRangeFrameIndex);
                return;

            //TODO NumericRange and ordered collections have the same problem - I need to summon their Integral and Ordering instances.
            //TODO usually these reside in the companion object of the type argument's class, so I need to write a bunch of bytecode
            //TODO that loads that 'gets' that companion object and gets the Integral or Ordering instance.
            //TODO which means I need to scan the companion object's class for method declarations.
            //TODO This will be a lot of fun to implement!!!

            //TODO if I really feel ambitious, I might implement recursive lookups too!
            //TODO I should probably factor this out to a separate "ImplicitSearch" class.

        }

        //TODO special-case some collections:
        //TODO  - immutable.WrappedString   --- done!
        //TODO  - immutable.Range           --- done!
        //TODO  - immutable.NumericRange    --- done! (but not yet tested)
        //TODO  - immutable.ArraySeq
        //TODO  - mutable.ArraySeq
        //TODO  - mutable.ArrayBuilder (debatable)
        //TODO


        //best effort
        final TypeSignature elementTypeSignature = typeSignature.hasTypeArguments() ? typeSignature.getTypeArgument(0) : TypeSignature.OBJECT_TYPE_SIGNATURE;

        final Label startLabel = new Label(), endLabel = new Label();

        //  scala.Iterator<E> iterator = $coll.<E>iterator();
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        final LocalVariable iterator = new LocalVariable("iterator", ITERATOR_DESCRIPTOR, "L" + ITERATOR_NAME + "<" + elementTypeSignature.toSignature() + ">;", startLabel, endLabel, iteratorIndex, iteratorFrameIndex);
        methodVisitor.visitTypeInsn(CHECKCAST, ITERABLE);               operandStack.replaceTop(ITERABLE_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, ITERABLE, "iterator", "()Lscala/collection/Iterator;", true);       operandStack.replaceTop(ITERATOR_TYPE);
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);              operandStack.pop();     localVariableTable.add(iterator);   localCounter.add(ITERATOR_TYPE);
        methodVisitor.visitLabel(startLabel);

        //  java.util.ArrayList list = new java.util.ArrayList();
        final Label javaListLabel = new Label();
        final int javaListIndex = localCounter.getSlotIndex(), javaListFrameIndex = localCounter.getFrameIndex();
        final LocalVariable javaList = new LocalVariable("list", "Ljava/util/ArrayList;", null, javaListLabel, endLabel, javaListIndex, javaListFrameIndex);
        methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");    operandStack.push(Type.getType(ArrayList.class));
        methodVisitor.visitInsn(DUP);                                   operandStack.push(Type.getType(ArrayList.class));
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);        operandStack.pop();
        methodVisitor.visitVarInsn(ASTORE, javaListIndex);              operandStack.pop();     localVariableTable.add(javaList);   localCounter.add(Type.getType(java.util.ArrayList.class));
        methodVisitor.visitLabel(javaListLabel);

        //  while
        final Label jumpBackTarget = javaListLabel, endLoopLabel = new Label();
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //      (iterator.hasNext()) {
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);               operandStack.push(ITERATOR_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, ITERATOR, "hasNext", "()Z", true);   operandStack.replaceTop(BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endLoopLabel);                operandStack.pop();
        //          list.add(serialize(iterator.next()));
        methodVisitor.visitVarInsn(ALOAD, javaListIndex);               operandStack.push(Type.getType(ArrayList.class));
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);               operandStack.push(ITERATOR_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, ITERATOR, "next", "()Ljava/lang/Object;", true);     operandStack.replaceTop(OBJECT_TYPE);
        Conversions.toSerializedType(classLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);     operandStack.replaceTop(2, BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);                                   operandStack.pop();
        //      }
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //after loop
        methodVisitor.visitLabel(endLoopLabel);
        localVariableTable.removeFramesFromIndex(localCounter.getFrameIndex());
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //javaList
        methodVisitor.visitVarInsn(ALOAD, javaListIndex);               operandStack.push(ARRAYLIST_TYPE);
        methodVisitor.visitLabel(endLabel);
        localVariableTable.removeFramesFromIndex(iteratorFrameIndex);   localCounter.reset(iteratorIndex, iteratorFrameIndex);
    }

    static void deserializeCollection(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, LocalCounter localCounter, LocalVariableTable localVariableTable, OperandStack operandStack) {
        //this is really a best effort.
        //the standard library may evolve again in 3.1 or 3.2
        //but for now this method is compatible the 2.12 and 2.13 (and thus 3.0) standard library
        //I hope this will continue to work.

        //special-case some special collections
        switch (typeSignature.getTypeName()) {
            case WRAPPED_STRING:
                //the serialized type for a WrappedString is simply its String
                methodVisitor.visitTypeInsn(CHECKCAST, STRING);         operandStack.replaceTop(STRING_TYPE);
                //[..., string]
                methodVisitor.visitTypeInsn(NEW, WRAPPED_STRING);       operandStack.push(WRAPPED_STRING_TYPE);
                //[..., string, wrapped string]
                methodVisitor.visitInsn(DUP_X1);                        operandStack.pop(2);    operandStack.push(WRAPPED_STRING_TYPE, STRING_TYPE, WRAPPED_STRING_TYPE);
                //[..., wrapped string, string, wrapped string]
                methodVisitor.visitInsn(SWAP);                          operandStack.pop(2);    operandStack.push(WRAPPED_STRING_TYPE, STRING_TYPE);
                //[..., wrapped string, wrapped string, string]
                methodVisitor.visitMethodInsn(INVOKESPECIAL, WRAPPED_STRING, "<init>", "(" + STRING_DESCRIPTOR + ")V", false);      operandStack.pop(2);
                //[..., wrapped string]
                return;

            case RANGE:
                final Label rangeStartLabel = new Label();
                final Label rangeEndLabel = new Label();

                //lets cast to NumericRange.OfInteger
                methodVisitor.visitTypeInsn(CHECKCAST, SCALALOADER_NUMERICRANGE_OFINTEGER);     operandStack.replaceTop(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                //let's store it in a local variable
                final int rangeIndex = localCounter.getSlotIndex(), rangeFrameIndex = localCounter.getFrameIndex();
                final LocalVariable range = new LocalVariable("range", SCALALOADER_NUMERICRANGE_OFINTEGER_DESCRIPTOR, null, rangeStartLabel, rangeEndLabel, rangeIndex, rangeFrameIndex);
                methodVisitor.visitVarInsn(ASTORE, rangeIndex);                                 operandStack.pop();
                localVariableTable.add(range); localCounter.add(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitLabel(rangeStartLabel);

                //load it again so we can call .start(), .end(), .step() and .inclusive()
                //load the companion object on which we will call either apply(start, end, step) or inclusive(start, end step)
                methodVisitor.visitFieldInsn(GETSTATIC, RANGE_COMPANION, MODULE$, RANGE_COMPANION_DESCRIPTOR);     operandStack.push(RANGE_COMPANION_TYPE);

                methodVisitor.visitVarInsn(ALOAD, rangeIndex);                                  operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, SCALALOADER_NUMERICRANGE_OFINTEGER, "start", "()I", false);    operandStack.replaceTop(Type.INT_TYPE);
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);                                  operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, SCALALOADER_NUMERICRANGE_OFINTEGER, "end", "()I", false);    operandStack.replaceTop(Type.INT_TYPE);
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);                                  operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, SCALALOADER_NUMERICRANGE_OFINTEGER, "step", "()I", false);    operandStack.replaceTop(Type.INT_TYPE);
                methodVisitor.visitVarInsn(ALOAD, rangeIndex);                                  operandStack.push(SCALALOADER_NUMERICRANGE_OFINTEGER_TYPE);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, SCALALOADER_NUMERICRANGE_OFINTEGER, "isInclusive", "()Z", false);    operandStack.replaceTop(Type.BOOLEAN_TYPE);

                final Label exclusiveLabel = new Label();
                final Label joinLabel = new Label();

                methodVisitor.visitJumpInsn(IFEQ, exclusiveLabel);   /*branches if the boolean on the stack is FALSE!*/  operandStack.pop();

                Object[] localFrame = localVariableTable.frame();   // [..., ScalaLoader..NumericRange.OfInteger]
                Object[] stackFrame = operandStack.frame();         // [..., scala..Range$, start, end, step]

                {   //inclusive
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, RANGE_COMPANION, "inclusive", "(III)" + RANGE_INCLUSIVE_DESCRIPTOR, false);
                    methodVisitor.visitJumpInsn(GOTO, joinLabel);
                }
                methodVisitor.visitLabel(exclusiveLabel);
                {   //exclusive
                    methodVisitor.visitFrame(F_FULL, localFrame.length, localFrame, stackFrame.length, stackFrame);
                    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, RANGE_COMPANION, "apply", "(III)" + RANGE_EXCLUSIVE_DESCRIPTOR, false);
                }
                /*effect of invokevirtual Range$.[apply/inclusive](III)Range*/                  operandStack.replaceTop(4, RANGE_TYPE);

                methodVisitor.visitLabel(joinLabel);
                //no need to update localsFrame because it hasn't changed.
                stackFrame = operandStack.frame();
                methodVisitor.visitFrame(F_FULL, localFrame.length, localFrame, stackFrame.length, stackFrame);

                localVariableTable.removeFramesFromIndex(rangeFrameIndex);  localCounter.reset(rangeIndex, rangeFrameIndex);
                methodVisitor.visitLabel(rangeEndLabel);
                return;

        }

        //TODO special-case some collections:
        //TODO  - immutable.WrappedString   --- done!
        //TODO  - immutable.Range           --- done!
        //TODO  - immutable.NumericRange    --- TODO requires ImplicitSearch for deserialization.
        //TODO                              --- TODO Also we need to generate instanceof bytecodes to check the runtime type of the elements
        //TODO                              --- TODO and use the correct component types for the start, end and step values of the NumericRange.
        //TODO  - immutable.ArraySeq
        //TODO  - mutable.ArraySeq
        //TODO  - mutable.ArrayBuilder (debatable)
        //TODO


        //best effort
        final TypeSignature elementTypeSignature = typeSignature.hasTypeArguments() ? typeSignature.getTypeArgument(0) : TypeSignature.OBJECT_TYPE_SIGNATURE;
        final String liveTypeName = typeSignature.internalName();

        final Label startLabel = new Label();
        final Label endLabel = new Label();

        //get the Iterator of the List
        final int iteratorIndex = localCounter.getSlotIndex(), iteratorFrameIndex = localCounter.getFrameIndex();
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, startLabel, endLabel, iteratorIndex, iteratorFrameIndex);
        localVariableTable.add(iterator); localCounter.add(Type.getType(java.util.Iterator.class));
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");           operandStack.replaceTop(Type.getType(List.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true);   operandStack.replaceTop(Type.getType(Iterator.class));
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                      operandStack.pop();
        methodVisitor.visitLabel(startLabel);

        //create an empty builder
        final Label builderLabel = new Label();
        final int builderIndex = localCounter.getSlotIndex(), builderFrameIndex = localCounter.getFrameIndex();
        final LocalVariable builder = new LocalVariable("builder", BUILDER_TYPE.getDescriptor(), null, builderLabel, endLabel, builderIndex, builderFrameIndex);
        localVariableTable.add(builder); localCounter.add(BUILDER_TYPE);
        generateNewBuilderCall(classLoader, methodVisitor, typeSignature, localCounter, localVariableTable, operandStack);
        methodVisitor.visitVarInsn(ASTORE, builderIndex);                       operandStack.pop();
        methodVisitor.visitLabel(builderLabel);

        //loop body!
        final Label jumpBackTarget = builderLabel;
        final Label endOfLoopLabel = new Label();
        final Object[] localsFrame = localVariableTable.frame();
        final Object[] stackFrame = operandStack.frame();
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the iterator, call next(), convert
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                       operandStack.push(Type.getObjectType("java/util/Iterator"));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);   operandStack.replaceTop(BOOLEAN_TYPE);
        methodVisitor.visitJumpInsn(IFEQ, endOfLoopLabel);                      operandStack.pop();
        //prepare builder so that we can call add later.
        methodVisitor.visitVarInsn(ALOAD, builderIndex);                        operandStack.push(BUILDER_TYPE);
        methodVisitor.visitVarInsn(ALOAD, iteratorIndex);                       operandStack.push(Type.getObjectType("java/util/Iterator"));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);                 operandStack.replaceTop(OBJECT_TYPE);
        Conversions.toLiveType(classLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localCounter, localVariableTable, operandStack);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, GROWABLE, "addOne", "(Ljava/lang/Object;)" + GROWABLE_TYPE.getDescriptor(), true);     operandStack.replaceTop(2, GROWABLE_TYPE);
        methodVisitor.visitInsn(POP);   /*pop the growable from the stack*/     operandStack.pop();
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        localVariableTable.removeFramesFromIndex(localCounter.getFrameIndex());
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitLabel(endOfLoopLabel);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the builder, call .result()
        methodVisitor.visitVarInsn(ALOAD, builderIndex);                        operandStack.push(BUILDER_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, BUILDER, "result", "()Ljava/lang/Object;", true);    operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, liveTypeName);                   operandStack.replaceTop(Type.getObjectType(liveTypeName));

        //clean up
        methodVisitor.visitLabel(endLabel);
        localVariableTable.removeFramesFromIndex(iteratorFrameIndex);   localCounter.reset(iteratorIndex, iteratorFrameIndex);
    }


    private static void generateNewBuilderCall(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, LocalCounter localCounter, LocalVariableTable localVariableTable, OperandStack operandStack) {
        final String companion = typeSignature.getTypeName() + '$';

        //push the companion object on the stack and then we will call .newBuilder(), .newBuilder(Ordering) or .newBuilder(ClassTag).
        methodVisitor.visitFieldInsn(GETSTATIC, companion, MODULE$, "L" + companion + ";");     operandStack.push(Type.getObjectType(companion));

        switch (typeSignature.getTypeName()) {
            //collection types that require an ordering
            case "scala/collection/immutable/SortedMap":
            case "scala/collection/immutable/SortedSet":
            case "scala/collection/immutable/TreeMap":
            case "scala/collection/immutable/TreeSet":

            case "scala/collection/mutable/CollisionProofHashMap":
            case "scala/collection/mutable/PriorityQueue":
            case "scala/collection/mutable/SortedMap":
            case "scala/collection/mutable/SortedSet":
            case "scala/collection/mutable/TreeMap":
            case "scala/collection/mutable/TreeSet":

            case "scala/collection/SortedMap":
            case "scala/collection/SortedSet":

                generateOrdering(classLoader, methodVisitor, typeSignature.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, companion, "newBuilder", "(Lscala/math/Ordering;)Lscala/collection/mutable/Builder;", false);
                operandStack.replaceTop(2, BUILDER_TYPE);
                break;

            //collection types that require a ClassTag
            case "scala/collection/immutable/ArraySeq":
            case "scala/collection/mutable/ArraySeq":

                generateClassTag(classLoader, methodVisitor, typeSignature.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, companion, "newBuilder", "(Lscala/reflect/ClassTag;)Lscala/collection/mutable/Builder;", false);
                operandStack.replaceTop(2, BUILDER_TYPE);
                break;

            default:
                //not a sorted or specialized collection
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, companion, "newBuilder", "()Lscala/collection/mutable/Builder;", false);
                operandStack.replaceTop(1, BUILDER_TYPE);
                break;
        }

    }

    private static void generateOrdering(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature elementType, LocalCounter localCounter, LocalVariableTable localVariableTable, OperandStack operandStack) {
        switch(elementType.internalName()) {
            //java primitives
            case "B":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Byte$", "MODULE$", "Lscala/math/Ordering$Byte$;");
                break;
            case "S":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Short$", "MODULE$", "Lscala/math/Ordering$Short$;");
                break;
            case "I":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Int$", "MODULE$", "Lscala/math/Ordering$Int$;");
                break;
            case "J":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Long$", "MODULE$", "Lscala/math/Ordering$Long$;");
                break;
            case "F":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$DeprecatedFloatOrdering$", "MODULE$", "Lscala/math/Ordering$DeprecatedFloatOrdering$;");
                break;
            case "D":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$DeprecatedDoubleOrdering$", "MODULE$", "Lscala/math/Ordering$DeprecatedDoubleOrdering$;");
                break;
            case "Z":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Boolean$", "MODULE$", "Lscala/math/Ordering$Boolean$;");
                break;
            case "C":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Char$", "MODULE$", "Lscala/math/Ordering$Char$;");
                break;
            //other basic types
            case "scala/Unit":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$Unit$", "MODULE$", "Lscala/math/Ordering$Unit$;");
                break;
            case "scala/Null":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");
                operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/Predef$", "MODULE$", "Lscala/Predef$;");
                operandStack.push(Type.getObjectType("scala/Predef$"));
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/Predef$", "$conforms", "()Lscala/Function1;", false);
                operandStack.replaceTop(Type.getObjectType("scala/Function1"));
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "ordered", "(Lscala/Function1;)Lscala/math/Ordering;", false);
                operandStack.pop(); //Ordering gets pushed at the end of the this generateOrdering method.
                break;

            //some commonly used object types
            case "java/lang/String":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$String$", "MODULE$", "Lscala/math/Ordering$String$;");
                break;
            case "scala/math/BigDecimal":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$BigDecimal$", "MODULE$", "Lscala/math/Ordering$BigDecimal$;");
                break;
            case "scala/math/BigInt":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$BigInt$", "MODULE$", "Lscala/math/Ordering$BigInt$;");
                break;

            //value wrappers
            case "scala/Option":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Option", "(Lscala/math/Ordering;)Lscala/math/Ordering;", false);                          operandStack.pop(); //Ordering gets pushed at the end of this generateOrdering method.
                break;
            case "scala/Tuple2":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple2", "(Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(2);    //Idem
                break;
            case "scala/Tuple3":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple3", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(3);   //Idem
                break;
            case "scala/Tuple4":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(3), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple4", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(4);      //Idem
                break;
            case "scala/Tuple5":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(3), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(4), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple5", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(5);     //Idem
                break;
            case "scala/Tuple6":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(3), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(4), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(5), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple6", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(6);        //Idem
                break;
            case "scala/Tuple7":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(3), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(4), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(5), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(6), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple7", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(7);   //Idem
                break;
            case "scala/Tuple8":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(3), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(4), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(5), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(6), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(7), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple8", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(8);  //Idem
                break;
            case "scala/Tuple9":
                methodVisitor.visitFieldInsn(GETSTATIC, "scala/math/Ordering$", "MODULE$", "Lscala/math/Ordering$;");   operandStack.push(Type.getObjectType("scala/math/Ordering$"));
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(0), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(1), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(2), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(3), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(4), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(5), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(6), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(7), localCounter, localVariableTable, operandStack);
                generateOrdering(classLoader, methodVisitor, elementType.getTypeArgument(8), localCounter, localVariableTable, operandStack);
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "scala/math/Ordering$", "Tuple9", "(Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;Lscala/math/Ordering;)Lscala/math/Ordering;", false);     operandStack.pop(9);     //Idem
                break;
            //Tuple1 and Tuple10-Tuple22 have no Ordering instances defined in the Ordering companion object!

            //TODO collection orderings?
            //TODO Comparable-based Orderings? I mean - I could just hardcode for java.util.UUID, java.math.BigInteger and java.math.BigDecimal I guess?
            //TODO alternatively, I could try to class-load the class? and check using Compmarable.class.isAssignableFrom(theClass)?

            default:
                //assume the Ordering instance can be found in the companion object.
                //TODO we need to inspect the fields & methods of the companion object and generate the appropriate calls!
                //TODO need to Adjust GlobalScanner or something so that CompanionObjectScanResult is part of the result?
                break;
        }

        operandStack.push(ORDERING_TYPE);
    }


    private static final void generateClassTag(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature elementType, LocalCounter localCounter, LocalVariableTable localVariableTable, OperandStack operandStack) {
        methodVisitor.visitFieldInsn(GETSTATIC, CLASSTAG_COMPANION, MODULE$, CLASSTAG_COMPANION_DESCRIPTOR);                    operandStack.push(CLASSTAG_COMPANION_TYPE);
        final String internalName = elementType.internalName();
        switch (internalName) {
            case "B":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Byte", "()" + BYTE_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "S":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Short", "()" + SHORT_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "I":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Int", "()" + INT_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "J":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Long", "()" + LONG_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "F":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Float", "()" + FLOAT_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "D":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Double", "()" + DOUBLE_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "C":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Char", "()" + CHAR_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "Z":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Boolean", "()" + BOOLEAN_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "scala/Unit":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Unit", "()" + UNIT_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "scala/Nothing":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Nothing", "()" + NOTHING_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "scala/Null":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Null", "()" + NULL_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "scala/Any":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Any", "()" + ANY_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "scala/AnyRef":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "AnyRef", "()" + ANYREF_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "scala/AnyVal":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "AnyVal", "()" + ANYVAL_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            case "java/lang/Object":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Object", "()" + OBJECT_TAG_DESCRIPTOR, false);
                operandStack.replaceTop(CLASSTAG_TYPE);
                break;
            default:
                methodVisitor.visitLdcInsn(Type.getObjectType(internalName));
                operandStack.push(Type.getType(Class.class));
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "apply", "(Ljava/lang/Class;)" + CLASSTAG_DESCRIPTOR, false);
                operandStack.replaceTop(2, CLASSTAG_TYPE);
                break;
        }

    }


}