package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.objectweb.asm.Opcodes.*;

/**
 * This class is NOT part of the public API!
 */
public final class MethodHeader {

    public final int access;
    public final String name;
    public final String descriptor;
    public final String signature;
    public final String[] exceptions;

    private Type[] argumentTypes;
    private Type returnType;

    private String returnSignature;
    private String[] parameterSignatures;
    boolean initSignatures;

    public MethodHeader(int access, String name, String descriptor, String signature, String[] exceptions) {
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.exceptions = exceptions;
    }

    public String getReturnDescriptor() {
        if (returnType == null)
            returnType = Type.getReturnType(descriptor);

        return returnType.getDescriptor();
    }

    public String getParameterDescriptor(int index) {
        if (argumentTypes == null)
            argumentTypes = Type.getArgumentTypes(descriptor);

        return argumentTypes[index].getDescriptor();
    }

    public String getReturnSignature() {
        if (!initSignatures) initSignatures();
        if (returnSignature == null)
            return null;

        return returnSignature;
    }

    public String getParameterSignature(int index) {
        if (!initSignatures) initSignatures();
        if (parameterSignatures == null)
            return null;

        return parameterSignatures[index];
    }

    private void initSignatures() {
        initSignatures = true;
        if (signature == null)
            return;

        SignatureReader signatureReader = new SignatureReader(signature);
        signatureReader.accept(new SignatureVisitor(AsmConstants.ASM_API) {
            List<String> parameterSignatures;

            @Override
            public SignatureVisitor visitParameterType() {
                return new SignatureWriter() {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        if (parameterSignatures == null) parameterSignatures = new ArrayList<>(1);
                        parameterSignatures.add(super.toString());
                    }
                };
            }

            @Override
            public SignatureVisitor visitReturnType() {
                return new SignatureWriter() {
                    @Override
                    public void visitEnd() {
                        super.visitEnd();
                        returnSignature = super.toString();
                    }
                };
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                if (parameterSignatures != null) {
                    MethodHeader.this.parameterSignatures = parameterSignatures.toArray(new String[0]);  //use String[]::new in Java 11+
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MethodHeader)) return false;

        MethodHeader that = (MethodHeader) o;
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
        if (exceptions != null) {
            stringBuilder.append("throws ");
            stringBuilder.append(Arrays.toString(exceptions));
        }
        return stringBuilder.toString();
    }

}
