package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Type;

/**
 * This class is NOT part of the public API!
 */
public final class LocalCounter implements Cloneable {

    private int nextSlot;
    private int nextFrame;

    public LocalCounter() {
    }

    public LocalCounter(int slotIndex, int frameIndex) {
        reset(slotIndex, frameIndex);
    }

    public int getSlotIndex() {
        return nextSlot;
    }

    public int getFrameIndex() {
        return nextFrame;
    }

    public void reset(int slotIndex, int frameIndex) {
        this.nextSlot = slotIndex;
        this.nextFrame = frameIndex;
    }

    public void add(Type type) {
        assert !Type.VOID_TYPE.equals(type) : "Tried to store a variable of type 'void' in the local variable table";

        nextSlot += type.getSize();
        nextFrame += 1;
    }

    @Override
    public LocalCounter clone() {
        return new LocalCounter(nextSlot, nextFrame);
    }

    @Override
    public String toString() {
        return "LocalCounter[slot=" + getSlotIndex() + ",frame=" + getFrameIndex() + "]";
    }
}
