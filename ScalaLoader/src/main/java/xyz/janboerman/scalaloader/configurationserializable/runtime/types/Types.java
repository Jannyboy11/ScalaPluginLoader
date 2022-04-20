package xyz.janboerman.scalaloader.configurationserializable.runtime.types;

import xyz.janboerman.scalaloader.bytecode.OperandStack;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ArrayParameterType;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterType;

import org.objectweb.asm.*;
import static org.objectweb.asm.Opcodes.*;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterizedParameterType;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.util.List;

class Types {

    static final String PREFIX_USING_DOTS = "xyz.janboerman.scalaloader.configurationserializable.runtime.types.generated.";
    static final String PREFIX_USING_SLASHES = PREFIX_USING_DOTS.replace('.', '/');

    private Types() {}

    //adapted from Conversions#genScalaPluginClassLoader
    static void genScalaPluginClassLoader(MethodVisitor methodVisitor, ScalaPluginClassLoader plugin, OperandStack operandStack) {
        String main = plugin.getMainClassName();
        Type mainType = Type.getType("L" + main.replace('.', '/') + ";");

        methodVisitor.visitLdcInsn(mainType);
        operandStack.push(Type.getType(Class.class));
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Class.class), "getClassLoader", "()" + Type.getDescriptor(ClassLoader.class), false);
        operandStack.replaceTop(Type.getType(ClassLoader.class));
        methodVisitor.visitTypeInsn(CHECKCAST, Type.getInternalName(ScalaPluginClassLoader.class));
        operandStack.replaceTop(Type.getType(ScalaPluginClassLoader.class));
    }

    //adapted from Conversions#genParameterType
    static void genParameterType(MethodVisitor methodVisitor, ParameterType parameterType, OperandStack operandStack) {
        if (parameterType instanceof ArrayParameterType) {
            ArrayParameterType apt = (ArrayParameterType) parameterType;

            //load component type
            genParameterType(methodVisitor, apt.getComponentType(), operandStack);
            //load 'false' (not var-args)
            methodVisitor.visitInsn(ICONST_0);                                                      operandStack.push(Type.BOOLEAN_TYPE);
            //invoke
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    Type.getInternalName(ArrayParameterType.class),
                    "from",
                    "(" + Type.getDescriptor(ParameterType.class) + "Z)" + Type.getDescriptor(ArrayParameterType.class),
                    false);                                                                 operandStack.replaceTop(2, Type.getType(ArrayParameterType.class));

        } else if (parameterType instanceof ParameterizedParameterType) {
            ParameterizedParameterType ppt = (ParameterizedParameterType) parameterType;
            List<? extends ParameterType> typeArguments = ppt.getTypeParameters();

            //load raw type
            methodVisitor.visitLdcInsn(Type.getType(ppt.getRawType()));                             operandStack.push(Type.getType(Class.class));
            //load type arguments
            methodVisitor.visitIntInsn(BIPUSH, typeArguments.size());                               operandStack.push(Type.INT_TYPE);
            methodVisitor.visitTypeInsn(ANEWARRAY, Type.getInternalName(ParameterType.class));      operandStack.replaceTop(Type.getType(ParameterType[].class));
            for (int i = 0; i < typeArguments.size(); i++) {
                methodVisitor.visitInsn(DUP);                                                       operandStack.push(Type.getType(ParameterType[].class));
                ParameterType typeArgument = typeArguments.get(i);
                methodVisitor.visitIntInsn(BIPUSH, i);                                              operandStack.push(Type.INT_TYPE);
                genParameterType(methodVisitor, typeArgument, operandStack);
                methodVisitor.visitInsn(AASTORE);                                                   operandStack.pop(2);
            }
            //invoke
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    Type.getInternalName(ParameterizedParameterType.class),
                    "from",
                    "(" + Type.getDescriptor(Class.class) + Type.getDescriptor(ParameterType[].class) + ")" + Type.getDescriptor(ParameterizedParameterType.class),
                    false);                                                                 operandStack.replaceTop(2, Type.getType(ParameterizedParameterType.class));
        } else {
            //just a plain ParameterType

            //load raw type
            methodVisitor.visitLdcInsn(Type.getType(parameterType.getRawType()));                   operandStack.push(Type.getType(Class.class));
            //invoke
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    Type.getInternalName(ParameterType.class),
                    "from",
                    "(" + Type.getDescriptor(java.lang.reflect.Type.class) + ")" + Type.getDescriptor(ParameterType.class),
                    false);                                                                 operandStack.replaceTop(Type.getType(ParameterType.class));
        }
    }

}
