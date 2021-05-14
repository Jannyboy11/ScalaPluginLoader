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
            } else if (isJavaUtilMap(typeSignature, pluginClassLoader)) {
                mapToSerializedType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localVariables);
                return;
            }
            /*TODO else if (isScalaCollection(typeSignature, pluginClassLoader)) {
                //TODO this needs to support both mutable and immutable collections!
            }*/
            /*TODO else if (isScalaMap(typeSignature, pluginClassLoader)) {

            }*/
        }

        //TODO just like conversion for arrays (including: tuples, Option, Either, Try)

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

    private static void mapToSerializedType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {
        final String rawTypeName = typeSignature.getTypeName();

        final TypeSignature keyTypeSignature = typeSignature.getTypeArgument(0);
        final TypeSignature valueTypeSignature = typeSignature.getTypeArgument(1);
        int localVariableIndex = localVariableTable.frameSize();
        final Type keyType = Type.getObjectType(keyTypeSignature.internalName());
        final Type valueType = Type.getObjectType(valueTypeSignature.internalName());

        //strategy: just create a new LinkedHashMap and convert the elements!

        final Label startLabel = new Label(), endLabel = new Label();

        //store the existing map in a local variable
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/Map");                operandStack.replaceTop(MAP_TYPE);
        final int oldMapIndex = localVariableIndex++;
        final LocalVariable oldMap = new LocalVariable("liveMap", "Ljava/util/Map;", null, startLabel, endLabel, oldMapIndex);
        localVariableTable.add(oldMap);
        methodVisitor.visitVarInsn(ASTORE, oldMapIndex);                            operandStack.pop();
        methodVisitor.visitLabel(startLabel);

        //create the new LinkedHashmap and store it in a local variable
        final Label newMapLabel = new Label();
        methodVisitor.visitTypeInsn(NEW, "java/util/LinkedHashMap");            operandStack.push(LINKEDHASHMAP_TYPE);
        methodVisitor.visitInsn(DUP);                                               operandStack.push(LINKEDHASHMAP_TYPE);
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false);      operandStack.pop();
        final int newMapIndex = localVariableIndex++;
        final LocalVariable newMap = new LocalVariable("serializedMap", "Ljava/util/Map;", null, newMapLabel, endLabel, newMapIndex);
        localVariableTable.add(newMap);
        methodVisitor.visitVarInsn(ASTORE, newMapIndex);                            operandStack.pop();
        methodVisitor.visitLabel(newMapLabel);

        //get the entry set iterator
        final Label iteratorLabel = new Label();
        methodVisitor.visitVarInsn(ALOAD, oldMapIndex);                             operandStack.push(MAP_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, "entrySet", "()Ljava/util/Set;", true);    operandStack.replaceTop(SET_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, SET_NAME, "iterator", "()Ljava/util/Iterator;", true);       operandStack.replaceTop(ITERATOR_TYPE);
        final int iteratorIndex = localVariableIndex++;
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex);
        localVariableTable.add(iterator);
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
        final int entryIndex = localVariableIndex++;
        final LocalVariable entry = new LocalVariable("entry", "Ljava/util/Map$Entry;", null, jumpBackTarget, endLoopLabel, entryIndex);
        localVariableTable.add(entry);
        methodVisitor.visitVarInsn(ASTORE, entryIndex);                             operandStack.pop();
        methodVisitor.visitLabel(entryLabel);

        //resultMap.put(serialize(entry.getKey()), serialize(entry.getValue()));
        //load resultMap
        methodVisitor.visitVarInsn(ALOAD, newMapIndex);                             operandStack.push(MAP_TYPE);
        //serialize(entry(getKey())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                              operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getKey", "()Ljava/lang/Object;", true);     operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, keyTypeSignature.internalName());    operandStack.replaceTop(keyType);
        toSerializedType(pluginClassLoader, methodVisitor, keyTypeSignature.toDescriptor(), keyTypeSignature.toSignature(), localVariableTable, operandStack);
        //serialize(entry(getValue())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                              operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getValue", "()Ljava/lang/Object;", true);   operandStack.replaceTop(OBJECT_TYPE);
        methodVisitor.visitTypeInsn(CHECKCAST, valueTypeSignature.internalName());  operandStack.replaceTop(valueType);
        toSerializedType(pluginClassLoader, methodVisitor, valueTypeSignature.toDescriptor(), valueTypeSignature.toSignature(), localVariableTable, operandStack);
        //call resultMap.put
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_PUT_NAME, MAP_PUT_DESCRIPTOR, true);           operandStack.replaceTop(3, OBJECT_TYPE);
        methodVisitor.visitInsn(POP);   /*pop the result from resultMap.put (which is the old value for the key)*/      operandStack.pop();
        //go back to loop start.
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        methodVisitor.visitLabel(endLoopLabel);
        localVariableTable.removeFramesFromIndex(entryIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the map containing the serialized keys and values
        methodVisitor.visitVarInsn(ALOAD, newMapIndex);                             operandStack.push(MAP_TYPE);
        localVariableTable.removeFramesFromIndex(oldMapIndex);
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
            } else if (isJavaUtilMap(typeSignature, pluginClassLoader)) {
                mapToLiveType(pluginClassLoader, methodVisitor, typeSignature, operandStack, localVariables);
                return;
            }
            //TODO else if Scala collection
            //TODO else if Scala Map
        }

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

            //it probably was a class already.
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
        methodVisitor.visitLabel(startLabel);

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
        methodVisitor.visitLabel(endLabel);                                             localVariableTable.removeFramesFromIndex(serializedCollectionIndex);    //the lowest index that we generated!
    }

    private static void mapToLiveType(ScalaPluginClassLoader pluginClassLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, OperandStack operandStack, LocalVariableTable localVariableTable) {

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
        int localVariableIndex = localVariableTable.frameSize();
        final Label startlabel = new Label();
        final Label endLabel = new Label();
        methodVisitor.visitLabel(startlabel);

        //store it in a local variable
        final int serializedMapIndex = localVariableIndex++;
        final LocalVariable serializedMap = new LocalVariable("serializedMap", MAP_DESCRIPTOR, null, startlabel, endLabel, serializedMapIndex);
        localVariableTable.add(serializedMap);
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
        final int liveMapIndex = localVariableIndex++;
        final LocalVariable liveMap = new LocalVariable("liveMap", "L" + implementationClassName + ";", null, liveMapLabel, endLabel, liveMapIndex);
        localVariableTable.add(liveMap);
        methodVisitor.visitVarInsn(ASTORE, liveMapIndex);                   operandStack.pop();

        //get the iterator and store it in a local variable
        final Label iteratorLabel = new Label();
        methodVisitor.visitLabel(iteratorLabel);
        methodVisitor.visitVarInsn(ALOAD, serializedMapIndex);              operandStack.push(MAP_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, "entrySet", "()Ljava/util/Set;", true);    operandStack.replaceTop(SET_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, SET_NAME, "iterator", "()Ljava/util/Iterator;", true);       operandStack.replaceTop(ITERATOR_TYPE);
        final int iteratorIndex = localVariableIndex++;
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, iteratorLabel, endLabel, iteratorIndex);
        localVariableTable.add(iterator);
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
        final int entryIndex = localVariableIndex++;
        final LocalVariable entry = new LocalVariable("entry", "Ljava/util/Map$Entry;", null, jumpBackTarget, endOfLoopLabel, entryIndex);
        localVariableTable.add(entry);
        methodVisitor.visitVarInsn(ASTORE, entryIndex);                 operandStack.pop();
        //prepare live map so that we can call .put on it later!
        methodVisitor.visitVarInsn(ALOAD, liveMapIndex);                operandStack.push(MAP_TYPE);
        //deserialize(entry.getKey())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                  operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getKey", "()Ljava/lang/Object;", true);    operandStack.replaceTop(OBJECT_TYPE);
        toLiveType(pluginClassLoader, methodVisitor, keyTypeSignature.toDescriptor(), keyTypeSignature.toSignature(), localVariableTable, operandStack);
        //deserialize(entry.getValue())
        methodVisitor.visitVarInsn(ALOAD, entryIndex);                  operandStack.push(MAP$ENTRY_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP$ENTRY_NAME, "getValue", "()Ljava/lang/Object;", true);       operandStack.replaceTop(OBJECT_TYPE);
        toLiveType(pluginClassLoader, methodVisitor, valueTypeSignature.toDescriptor(), valueTypeSignature.toSignature(), localVariableTable, operandStack);
        //call liveMap.put
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, MAP_NAME, MAP_PUT_NAME, MAP_PUT_DESCRIPTOR, true);                           operandStack.replaceTop(3, OBJECT_TYPE);
        methodVisitor.visitInsn(POP);   /*get rid of the old value in the map*/     operandStack.pop();
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //end of loop
        localVariableTable.removeFramesFromIndex(entryIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitLabel(endOfLoopLabel);
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //load the result
        methodVisitor.visitVarInsn(ALOAD, liveMapIndex);                operandStack.push(Type.getObjectType(implementationClassName));
        methodVisitor.visitLabel(endLabel);
        localVariableTable.removeFramesFromIndex(serializedMapIndex);   //the lowest index that we generated!
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

class ScalaConversions {

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


    private ScalaConversions() {
    }

    private static final void loadClassTag(String internalName, MethodVisitor methodVisitor, OperandStack operandStack) {
        methodVisitor.visitFieldInsn(GETSTATIC, CLASSTAG_COMPANION, MODULE$, CLASSTAG_COMPANION_DESCRIPTOR);                    operandStack.push(CLASSTAG_COMPANION_TYPE);
        switch (internalName) {
            case "B":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Byte", "()" + BYTE_TAG_DESCRIPTOR, false);
                break;
            case "S":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Short", "()" + SHORT_TAG_DESCRIPTOR, false);
                break;
            case "I":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Int", "()" + INT_TAG_DESCRIPTOR, false);
                break;
            case "J":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Long", "()" + LONG_TAG_DESCRIPTOR, false);
                break;
            case "F":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Float", "()" + FLOAT_TAG_DESCRIPTOR, false);
                break;
            case "D":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Double", "()" + DOUBLE_TAG_DESCRIPTOR, false);
                break;
            case "C":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Char", "()" + CHAR_TAG_DESCRIPTOR, false);
                break;
            case "Z":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Boolean", "()" + BOOLEAN_TAG_DESCRIPTOR, false);
                break;
            case "scala/Unit":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Unit", "()" + UNIT_TAG_DESCRIPTOR, false);
                break;
            case "scala/Nothing":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Nothing", "()" + NOTHING_TAG_DESCRIPTOR, false);
                break;
            case "scala/Null":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Null", "()" + NULL_TAG_DESCRIPTOR, false);
                break;
            case "scala/Any":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Any", "()" + ANY_TAG_DESCRIPTOR, false);
                break;
            case "scala/AnyRef":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "AnyRef", "()" + ANYREF_TAG_DESCRIPTOR, false);
                break;
            case "scala/AnyVal":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "AnyVal", "()" + ANYVAL_TAG_DESCRIPTOR, false);
                break;
            case "java/lang/Object":
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "Object", "()" + OBJECT_TAG_DESCRIPTOR, false);
                break;
            default:
                methodVisitor.visitLdcInsn(Type.getObjectType(internalName));       operandStack.push(Type.getType(Class.class));
                methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASSTAG_COMPANION, "apply", "(Ljava/lang/Class;)" + CLASSTAG_DESCRIPTOR, false);
                break;
        }                                                                                                                       operandStack.replaceTop(CLASSTAG_TYPE);
    }

    //TODO code generation for instances of scala.math.Ordering:
    //TODO https://www.scala-lang.org/api/current/scala/math/Ordering$.html
    //TODO if it's not one of the built-ins, try to get them from the companion object of the element type.
    //TODO might have to load the class, scan for fields. #funzies.
    //TODO take Scala 3 derivation into account! How does that compile to bytecode? probably an instance in the companion object.
    //TODO take into account that instances are not necessarily vals or no-args methods, they themselves could take implicit parameters!
    //TODO test this!

    static boolean isScalaCollection(final TypeSignature typeSignature, final ClassLoader pluginClassLoader) {
        final String typeName = typeSignature.getTypeName();

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
            case "scala/collection/mutable/Buffer":
            case "scala/collection/mutable/Builder":
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

            //scala.collection/mutable
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
        } catch(ClassNotFoundException scalaPluginDoesNotDependOnScalaLibrary) {
            //for now, just do nothing. this is unreachable.
            //maybe in the future we allow scala plugins that don't use the standard library?
        }

        return false;
    }



    //immutable collections

    static void serializeCollection(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, LocalVariableTable localVariableTable, OperandStack operandStack) {
        //this is really a best effort.
        //the standard library may evolve again in 3.1 or 3.2
        //but for now this method is compatible the 2.12 and 2.13 (and thus 3.0) standard library
        //I hope this will continue to work.

        assert typeSignature.hasTypeArguments(1) : "trying to serialize scala collection without type arguments. Type signature = " + typeSignature;
        final TypeSignature elementTypeSignature = typeSignature.getTypeArgument(0);

        int localVariableIndex = localVariableTable.frameSize();
        final Label startLabel = new Label(), endLabel = new Label();

        //  scala.Iterator<E> iterator = $coll.<E>iterator();
        final int iteratorIndex = localVariableIndex++;
        final LocalVariable iterator = new LocalVariable("iterator", ITERATOR_DESCRIPTOR, "L" + ITERATOR_NAME + "<" + elementTypeSignature.toSignature() + ">;", startLabel, endLabel, iteratorIndex);
        methodVisitor.visitTypeInsn(CHECKCAST, ITERABLE);               operandStack.replaceTop(ITERABLE_TYPE);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, ITERABLE, "iterator", "()scala/collection/Iterator;", true);       operandStack.replaceTop(ITERATOR_TYPE);
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);              operandStack.pop();     localVariableTable.add(iterator);
        methodVisitor.visitLabel(startLabel);

        //  java.util.ArrayList list = new java.util.ArrayList();
        final Label javaListLabel = new Label();
        final int javaListIndex = localVariableIndex++;
        final LocalVariable javaList = new LocalVariable("list", "Ljava/util/ArrayList", null, javaListLabel, endLabel, javaListIndex);
        methodVisitor.visitTypeInsn(NEW, "java/util/ArrayList");    operandStack.push(Type.getType(ArrayList.class));
        methodVisitor.visitInsn(DUP);                                   operandStack.push(Type.getType(ArrayList.class));
        methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);        operandStack.pop();
        methodVisitor.visitVarInsn(ASTORE, javaListIndex);              operandStack.pop();     localVariableTable.add(javaList);
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
        Conversions.toSerializedType(classLoader, methodVisitor, elementTypeSignature.toDescriptor(), elementTypeSignature.toSignature(), localVariableTable, operandStack);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);     operandStack.replaceTop(2, BOOLEAN_TYPE);
        methodVisitor.visitInsn(POP);                                   operandStack.pop();
        //      }
        methodVisitor.visitJumpInsn(GOTO, jumpBackTarget);

        //after loop
        methodVisitor.visitLabel(endLoopLabel);
        localVariableTable.removeFramesFromIndex(localVariableIndex);
        assert Arrays.equals(localsFrame, localVariableTable.frame()) : "local variables differ!";
        assert Arrays.equals(stackFrame, operandStack.frame()) : "stack operands differ!";
        methodVisitor.visitFrame(F_FULL, localsFrame.length, localsFrame, stackFrame.length, stackFrame);

        //javaList
        methodVisitor.visitVarInsn(ALOAD, javaListIndex);
        methodVisitor.visitLabel(endLabel);
    }

    static void deserializeCollection(ScalaPluginClassLoader classLoader, MethodVisitor methodVisitor, TypeSignature typeSignature, LocalVariableTable localVariableTable, OperandStack operandStack) {
        final TypeSignature elementTypeSignature = typeSignature.getTypeArgument(0);

        int localVariableIndex = localVariableTable.frameSize();

        final Label startLabel = new Label();
        final Label endLabel = new Label();

        //get the Iterator of the List
        final int iteratorIndex = localVariableIndex++;
        final LocalVariable iterator = new LocalVariable("iterator", "Ljava/util/Iterator;", null, startLabel, endLabel, iteratorIndex);
        methodVisitor.visitTypeInsn(CHECKCAST, "java/util/List");           operandStack.replaceTop(Type.getType(List.class));
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true);   operandStack.replaceTop(Type.getType(Iterator.class));
        methodVisitor.visitVarInsn(ASTORE, iteratorIndex);                      operandStack.pop();
        localVariableTable.add(iterator);
        methodVisitor.visitLabel(startLabel);

        final String companion = typeSignature.getTypeName() + '$';
        methodVisitor.visitFieldInsn(GETSTATIC, companion, MODULE$, "L" + companion + ";");     operandStack.push(Type.getObjectType(companion));
        //TODO instantiate the new scala collection builder. (call the .newBuilder() method on the companion object of the scala collection)
        //TODO this is different for some of the ordered collections - we need to provide an Order[X] instance!
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, companion, "newBuilder", "()Lscala/collection/mutable/Builder", false);



        //TODO ^^^^^^^^^^^ might need to supply a ClassTag, or use a different class because the declared class is Abstract.
        //TODO ^^^^^^^^^^^ might need to supply an Order[X].
        //TODO loop through the list (using the java.util.Iterator)
        //TODO convert the elements, append to the builder
        //TODO call builder.result()
        //TODO done!

        methodVisitor.visitLabel(endLabel);
    }

    //mutable collections

    //other standard datatypes

    //TODO helper method for getting Order instances on the operand stack!
    
}