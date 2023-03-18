package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Label;

import java.util.Objects;

/**
 * This class is NOT part of the public API!
 */
public final class LocalVariable {

    public final String name;
    public final String descriptor;
    public final String signature;
    public final Label startLabel;
    public final Label endLabel;
    public final int tableSlot;
    public final int frameIndex;

    public LocalVariable(
            String name,
            String descriptor,
            String signature,
            Label startLabel,
            Label endLabel,
            int tableSlot,
            int frameIndex
    ) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.startLabel = startLabel;
        this.endLabel = endLabel;
        this.tableSlot = tableSlot;
        this.frameIndex = frameIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LocalVariable)) return false;

        LocalVariable that = (LocalVariable) o;
        return Objects.equals(this.name, that.name)
                && Objects.equals(this.descriptor, that.descriptor)
                && this.tableSlot == that.tableSlot
                && this.frameIndex == that.frameIndex
                && Objects.equals(this.startLabel, that.startLabel)
                && Objects.equals(this.endLabel, that.endLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, descriptor, tableSlot, frameIndex, startLabel, endLabel);
    }

    @Override
    public String toString() {
        return "LocalVariable{" + name + ": " + (signature == null ? descriptor : signature) + " @ " + tableSlot + "/" + frameIndex + "}";
    }
}
