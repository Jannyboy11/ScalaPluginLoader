package xyz.janboerman.scalaloader.bytecode;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import xyz.janboerman.scalaloader.compat.Compat;
import static xyz.janboerman.scalaloader.util.BoolOps.implies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class is NOT part of the public API!
 */
public final class TypeSignature {

    private static final String ARRAY = "array";
    private static final TypeSignature OBJECT_TYPE_SIGNATURE = new TypeSignature("java/lang/Object", Compat.emptyList());

    private final String typeName;
    private final List<TypeSignature> typeArguments;

    public TypeSignature(String typeName, List<TypeSignature> typeArguments) {
        assert typeName != null : "type name cannot be null";
        assert typeArguments != null : "type arguments cannot be null";
        assert implies(ARRAY.equals(typeName), typeArguments.size() == 1) : "array type signature must have exactly one type argument";

        this.typeName = typeName;
        this.typeArguments = typeArguments;
    }

    public boolean isArray() {
        return ARRAY.equals(getTypeName());
    }

    public boolean hasTypeArguments() {
        return !typeArguments.isEmpty();
    }

    public boolean hasTypeArguments(int n) {
        if (n == 1) {
            return hasTypeArguments();
        } else {
            return typeArguments.size() >= n;
        }
    }

    public static TypeSignature ofDescriptor(String descriptor) {
        if (descriptor.startsWith("[")) {
            return new TypeSignature(ARRAY, Compat.singletonList(ofDescriptor(descriptor.substring(1))));
        } else if (descriptor.startsWith("T")) {
            return OBJECT_TYPE_SIGNATURE;
        } else {
            return new TypeSignature(Type.getType(descriptor).getInternalName(), Compat.emptyList());
        }
    }

    public static TypeSignature ofSignature(String signature) {
        SignatureReader signatureReader = new SignatureReader(signature);
        MySignatureVisitor signatureVisitor = new MySignatureVisitor();
        signatureReader.acceptType(signatureVisitor);   //use acceptType instead of accept.
        return toTypeSignature(signatureVisitor);
    }

    private static TypeSignature toTypeSignature(MySignatureVisitor mySignatureVisitor) {
        if ("java/lang/Object".equals(mySignatureVisitor.rawTypeName)) {
            return OBJECT_TYPE_SIGNATURE;
        } else {
            String name = mySignatureVisitor.rawTypeName;
            List<TypeSignature> typeArguments = mySignatureVisitor.typeArgs.stream().map(TypeSignature::toTypeSignature).collect(Collectors.toList());
            return new TypeSignature(name, typeArguments);
        }
    }

    /**
     * The raw type (without type arguments) of this type.
     * If this TypeSignature was constructed from an array signature, the raw type returned here is "array"
     * @return "array" or the raw type without type arguments.
     */
    public String getTypeName() {
        return typeName;
    }

    public String internalName() {
        if (ARRAY.equals(typeName)) {
            return '[' + getTypeArgument(0).internalName();
        }

        return typeName;
    }

    /**
     * The type arguments types of this type.
     * If this TypeSignature was constructed from an array signature, the type arguments only contains one element.
     * @return the type arguments, or the array component type.
     */
    public List<TypeSignature> getTypeArguments() {
        return Collections.unmodifiableList(typeArguments);
    }

    public int countTypeArguments() {
        return typeArguments.size();
    }

    public TypeSignature getTypeArgument(int index) {
        return typeArguments.get(index);
    }

    public final String toDescriptor() {
        String typeName = getTypeName();
        switch (typeName) {
            case ARRAY:
                return "[" + getTypeArguments().get(0).toDescriptor();
            case "B":
            case "S":
            case "I":
            case "J":
            case "F":
            case "D":
            case "Z":
            case "C":
            case "V":
                return typeName;
            default:
                return 'L' + typeName + ';';
        }
    }

    public final String toSignature() {
        String typeName = getTypeName();
        switch (typeName) {
            case ARRAY:
                return "[" + getTypeArgument(0).toSignature();
            case "B":
            case "S":
            case "I":
            case "J":
            case "F":
            case "D":
            case "Z":
            case "C":
            case "V":
                return typeName;
            default:
                List<TypeSignature> typeArgumentSignatures = getTypeArguments();
                String typeArguments = typeArgumentSignatures.isEmpty() ? "" : getTypeArguments().stream()
                        .map(TypeSignature::toSignature)
                        .collect(Collectors.joining("", "<", ">"));
                return "L" + typeName + typeArguments + ";";
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTypeName(), getTypeArguments());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof TypeSignature)) return false;

        TypeSignature that = (TypeSignature) obj;
        return Objects.equals(this.getTypeName(), that.getTypeName())
                && Objects.equals(this.getTypeArguments(), that.getTypeArguments());
    }

    @Override
    public String toString() {
        return "TypeSignature(typeName=" + getTypeName() + ", typeArguments=" + getTypeArguments() + ")";
    }

    //TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )

    private static class MySignatureVisitor extends SignatureVisitor {

        private String rawTypeName;
        private final List<MySignatureVisitor> typeArgs = new ArrayList<>(0);

        private MySignatureVisitor() {
            super(AsmConstants.ASM_API);
        }

        @Override
        public void visitBaseType(char descriptor) {
            rawTypeName = "" + descriptor;  //one of the primitive types, or 'V' for void.

            super.visitBaseType(descriptor);
        }

        @Override
        public void visitTypeVariable(String name) {
            //this can happen for example in a field that has the same type T as the type parameter of the class.
            rawTypeName = "java/lang/Object";   //best effort.

            super.visitTypeVariable(name);
        }

        @Override
        public SignatureVisitor visitArrayType() {
            rawTypeName = ARRAY;
            MySignatureVisitor child = new MySignatureVisitor();
            typeArgs.add(child);

            return child;
        }

        @Override
        public void visitClassType(String name) {
            rawTypeName = name;

            super.visitClassType(name);
        }

        @Override
        public void visitInnerClassType(String name) {
            rawTypeName += '$' + name;
            typeArgs.clear();   //reset - only count the type arguments of the inner class.

            super.visitInnerClassType(name);
        }

        @Override
        public void visitTypeArgument() {
            super.visitTypeArgument();
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            //'+' is extends, '-' is super, '=' is instanceof.

            MySignatureVisitor child = new MySignatureVisitor();
            typeArgs.add(child);

            return child;
        }

        @Override
        public void visitEnd() {
            assert this.rawTypeName != null : "end of type signature visitor, didn't set a type name";

            super.visitEnd();
        }
    }

}
