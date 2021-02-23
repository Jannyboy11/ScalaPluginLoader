package xyz.janboerman.scalaloader.bytecode;

import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

/**
 * This class is NOT part of the public API!
 */
public class FieldDeclaration {

    public final int access;
    public final String name;
    public final String descriptor;
    public final String signature;

    public FieldDeclaration(int access, String name, String descriptor, String signature) {
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o){
        if (o == this) return true;
        if (!(o instanceof FieldDeclaration)) return false;

        FieldDeclaration that = (FieldDeclaration) o;
        return this.access == that.access
                && this.name.equals(that.name)
                && this.descriptor.equals(that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(access, name, descriptor);
    }

    @Override
    public String toString() {
        String accessString
                = (access & ACC_PRIVATE) == ACC_PRIVATE ? "private "
                : (access & ACC_PROTECTED) == ACC_PROTECTED ? "protected "
                : (access & ACC_PUBLIC) == ACC_PUBLIC ? "public "
                : "(package-private) ";
        StringBuilder stringBuilder = new StringBuilder(accessString);
        if ((access & ACC_STATIC) == ACC_STATIC) stringBuilder.append("static ");
        stringBuilder.append(name);
        stringBuilder.append(' ');
        if (signature != null) stringBuilder.append(signature); else stringBuilder.append(descriptor);
        return stringBuilder.toString();
    }
}
