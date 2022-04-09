package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * This class is NOT part of the public API!
 */
public final class LocalVariableTable implements Iterable<LocalVariable> {

    private int maxCount;
    private final Set<LocalVariable> localVariables;
    private final ArrayList<LocalVariable> frameData;

    public LocalVariableTable() {
        this.localVariables = new LinkedHashSet<>();
        this.frameData = new ArrayList<>(0);
        this.maxCount = 0;
    }

    public void add(LocalVariable localVariable) {
        assert localVariable != null : "can't add a null localVariable";

        final int tableSlot = localVariable.tableSlot;
        final int frameIndex = localVariable.frameIndex;

        //assert that the position of the local variable in the table is 0 or more.
        assert 0 <= tableSlot : "index in the local variable table below 0";
        //assert that the new local variable's index is not more than 1 above the currently known highest local variable.
        assert tableSlot <= maxLocals() : "local variable " + localVariable + " is more than 1 higher than the currently known highest local variable in the table " + this + ", maxLocals = " + maxLocals();
        //assert that the local variable is replaces an older one, or is added at the 'next' index.
        assert tableSlot <= localsSize() : "local variable " + localVariable + " does not 'replace' another, nor, is it the 'next' local variable in the table " + this;
        //assert that the local variable is the last one in the frame
        assert frameIndex == frameData.size() : "local variable " + localVariable + " can't be appended at the end of the frame: " + frameData;

        localVariables.add(localVariable);
        maxCount = Math.max(maxCount, tableSlot + Type.getType(localVariable.descriptor).getSize());
        addFrame(localVariable);

        //assert that the frame data is consistent
        assert frameData.stream().noneMatch(Objects::isNull) : "a local variable in the frame is null";
        assert IntStream.range(0, frameData.size()).allMatch(index -> frameData.get(index).frameIndex == index);
    }

    public void add(LocalVariable... localVariables) {
        for (LocalVariable localVariable : localVariables) {
            assert localVariable != null : "local variable can't be null!";

            this.localVariables.add(localVariable);
            this.maxCount = Math.max(maxCount, localVariable.tableSlot + Type.getType(localVariable.descriptor).getSize());
            addFrame(localVariable);
        }

        //assert that there are no gaps in the local variable table
        assert noGapsBetweenEntries();
        //assert that no local variable in the frame is null
        assert frameData.stream().allMatch(Objects::nonNull) : "a local variable in the frame is null";
        //assert that the frame data is consistent
        assert IntStream.range(0, frameData.size()).allMatch(index -> frameData.get(index).frameIndex == index);
    }

    private boolean noGapsBetweenEntries() {
        Set<Integer> indicesToGo = IntStream.range(0, localsSize()).collect(HashSet::new, Set::add, Set::addAll);
        //this is a bit expensive. can we use our own Range abstraction here, and subtract Ranges from it?
        //it doesn't really matter in production though, because assertions shouldn't be enabled there.
        for (LocalVariable local : this) {
            int size = Type.getType(local.descriptor).getSize();
            for (int i = local.tableSlot; i < local.tableSlot + size; i++) {
                indicesToGo.remove(i);  //is executed twice for long and double.
            }
        }
        return indicesToGo.isEmpty();
    }

    private void addFrame(LocalVariable localVariable) {
        assert localVariable != null : "local variable can't be null!";

        final int frameIndex = localVariable.frameIndex;
        if (frameIndex < frameData.size()) {
            //replace
            frameData.set(frameIndex, localVariable);
        } else if (frameIndex == frameData.size()) {
            //append
            frameData.add(localVariable);
        } else {
            //add nulls and replace later
            for (int i = frameData.size(); i < frameIndex; i++) {
                frameData.add(null);
            }
            addFrame(localVariable);
            assert frameData.get(frameData.size() - 1) != null : "last local in the frame cannot be null!";
        }

        //callers need to assert that our frame data is consistent.
    }

    public int maxLocals() {
        return maxCount;
    }

    private int localsSize() {
        return localVariables.stream().mapToInt(local -> Type.getType(local.descriptor).getSize()).sum();
    }

    public void removeFramesFromIndex(int frameIndex) {
        int size = frameData.size();
        for (int i = size - 1; i >= frameIndex; i--) {
            frameData.remove(i);
        }
        frameData.trimToSize();

        assert frameData.stream().noneMatch(Objects::isNull) : "there is a null localvariable in the frame data";
    }

    @Override
    public Iterator<LocalVariable> iterator() {
        return getLocalVariables().iterator();
    }

    public Set<LocalVariable> getLocalVariables() {
        return Collections.unmodifiableSet(localVariables);
    }

    public List<LocalVariable> getFrameLocals() {
        return Collections.unmodifiableList(frameData);
    }

    public Object[] frame() {
        return frameData.stream().map(localVariable -> {
            String desc = localVariable.descriptor;
            switch (desc) {
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
                    return Type.getType(desc).getInternalName();
            }
        }).toArray();
    }

    @Override
    public String toString() {
        return localVariables.toString();
    }

}
