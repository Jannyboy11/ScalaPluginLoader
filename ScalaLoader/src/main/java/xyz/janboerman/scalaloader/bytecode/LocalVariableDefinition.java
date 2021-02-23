package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Label;

import java.util.Objects;

/**
 * This class is NOT part of the public API!
 */
public class LocalVariableDefinition {

    public final String name;
    public final String descriptor;
    public final String signature;
    public final Label startLabel;
    public final Label endLabel;
    public final int tableIndex;

    public LocalVariableDefinition(
            String name,
            String descriptor,
            String signature,
            Label startLabel,
            Label endLabel,
            int tableIndex
    ) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.startLabel = startLabel;
        this.endLabel = endLabel;
        this.tableIndex = tableIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LocalVariableDefinition)) return false;

        LocalVariableDefinition that = (LocalVariableDefinition) o;
        return Objects.equals(this.name, that.name)
                && Objects.equals(this.descriptor, that.descriptor)
                && this.tableIndex == that.tableIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, descriptor, tableIndex);
    }

    @Override
    public String toString() {
        return "var " + name + ": " + (signature == null ? descriptor : signature) + " @ " + tableIndex;
    }
}
