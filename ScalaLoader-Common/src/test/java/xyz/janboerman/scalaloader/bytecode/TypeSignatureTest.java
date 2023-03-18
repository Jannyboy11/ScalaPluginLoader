package xyz.janboerman.scalaloader.bytecode;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import xyz.janboerman.scalaloader.compat.Compat;

public class TypeSignatureTest {

    @Test
    public void testSimpleDescriptor() {
        TypeSignature expected = new TypeSignature("java/lang/Object", Compat.emptyList());
        TypeSignature actual = TypeSignature.ofDescriptor("Ljava/lang/Object;");

        assertEquals(expected, actual);
    }

    @Test
    public void testSimpleSignature() {
        TypeSignature expected = new TypeSignature("java/lang/Object", Compat.emptyList());
        TypeSignature actual = TypeSignature.ofSignature("Ljava/lang/Object;");

        assertEquals(expected, actual);
    }


    @Test
    public void testArrayFromDescriptor() {
        TypeSignature expected = new TypeSignature("array", Compat.singletonList(new TypeSignature("I", Compat.emptyList())));
        TypeSignature actual = TypeSignature.ofDescriptor("[I");

        assertEquals(expected, actual);
    }

    @Test
    public void testArrayFromSignature() {
        TypeSignature expected = new TypeSignature("array", Compat.singletonList(new TypeSignature("java/lang/String", Compat.emptyList())));
        TypeSignature actual = TypeSignature.ofSignature("[Ljava/lang/String;");

        assertEquals(expected, actual);
    }

    @Test
    public void testListFromSignature() {
        TypeSignature expected = new TypeSignature("java/util/List", Compat.singletonList(new TypeSignature("java/lang/Integer", Compat.emptyList())));
        TypeSignature actual = TypeSignature.ofSignature("Ljava/util/List<Ljava/lang/Integer;>;");

        assertEquals(expected, actual);
    }

}
