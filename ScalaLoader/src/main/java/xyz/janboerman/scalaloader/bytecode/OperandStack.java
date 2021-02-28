package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is NOT part of the public API!
 */
public final class OperandStack {

    private int maxCount;
    private final ArrayList<Type> operandTypes;

    public OperandStack() {
        this.operandTypes = new ArrayList<>(0);
        this.maxCount = 0;
    }

    public void push(Type type) {
        if (!Type.VOID_TYPE.equals(type)) {
            this.operandTypes.add(type);
            this.maxCount = Math.max(this.maxCount, stackSize());
        }
    }

    public void push(Type... types) {
        for (Type type : types) {
            if (!Type.VOID_TYPE.equals(type)) {
                this.operandTypes.add(type);
            }
        }

        this.maxCount = Math.max(this.maxCount, stackSize());
    }

    public Type replaceTop(Type type) {
        int lastIndex = operandTypes.size() - 1;
        Type lastElement = operandTypes.get(lastIndex);

        if (Type.VOID_TYPE.equals(type)) {
            operandTypes.remove(lastIndex);
        } else {
            operandTypes.set(lastIndex, type);
            maxCount = Math.max(maxCount, stackSize());
        }

        return lastElement;
    }

    public void replaceTop(int popAmount, Type type){
        shrink(popAmount);
        push(type);
    }

    public void shrink(int subtractAmount) {
        for (int i = 0; i < subtractAmount && !operandTypes.isEmpty(); i++) {
            int lastIndex = operandTypes.size() - 1;
            operandTypes.remove(lastIndex);
        }
    }

    public Type pop() {
        assert !operandTypes.isEmpty() : "can't pop from an empty stack";

        int lastIndex = operandTypes.size() - 1;
        Type operandType = operandTypes.remove(lastIndex);
        return operandType;
    }

    public void pop(int amount) {
        final int initialSize = operandTypes.size();
        assert initialSize >= amount;

        for (int i = 0; i < amount; i++) {
            operandTypes.remove(initialSize - 1 - i);
        }
    }

    /**
     * Get the size of the stack. This method takes "type categories" into account, so longs and doubles count as 2.
     * @return the stack size
     */
    public int stackSize() {
        int size = 0;
        for (Type operandType : operandTypes) {
            size += operandType.getSize();
        }
        return size;
    }

    /**
     * Get the number of operands on the stack. This method does not take "type categories" into account, so every item counts as 1.
     * @return the number of operands
     */
    public int operandsSize() {
        return operandTypes.size();
    }

    /**
     * The largest size (as determined by {@link #stackSize()}) that this stack has been in its lifetime.
     * @return the maximum stack size
     */
    public int maxStack() {
        return maxCount;
    }

    public List<Type> getOperands() {
        return Collections.unmodifiableList(operandTypes);
    }

    public Object[] frame() {
        return operandTypes.stream().map(type -> {
            String internalName = type.getInternalName();
            switch (internalName) {
                case "B":
                case "S":
                case "I":
                case "C":
                case "Z":
                    return Opcodes.INTEGER;
                case "J":
                    return Opcodes.LONG;
                case "F":
                    return Opcodes.FLOAT;
                case "D":
                    return Opcodes.DOUBLE;
                case "V":
                    return Opcodes.TOP;
                default:
                    return internalName;
            }
        }).toArray();
    }

//    private static void test() {
//        byte Byte = 0;
//        short Short = 0;
//        int Int = 0;
//        long Long = 0;
//        float Float = 0;
//        double Double = 0;
//        char Char = 0;
//        boolean Boolean = false;
//        while (Byte < 10) {
//            Byte++;
//            Short++;
//            Int++;
//            Long++;
//            Float++;
//            Double++;
//            Char++;
//            Boolean = !Boolean;
//        }
//    }
    //methodVisitor.visitFrame(Opcodes.F_FULL, 8, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.INTEGER, Opcodes.LONG, Opcodes.FLOAT, Opcodes.DOUBLE, Opcodes.INTEGER, Opcodes.INTEGER}, 0, new Object[]{});
    //this tells us that for the visitFrame method, longs and doubles count as 1.
}
