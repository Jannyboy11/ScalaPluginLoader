package xyz.janboerman.scalaloader.configurationserializable.transform;

import java.util.Collection;
import java.util.EnumSet;

public class Foo {

    private static void main(String[] args) {
        String[] strings = new String[100];
        strings[99] = "hi";
    }

    enum TestEnum {
        FOO, BAR;
    }

    /*
    {
        methodVisitor = classWriter.visitMethod(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "test", "()V", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(37, label0);
        methodVisitor.visitLdcInsn(Type.getType("Lxyz/janboerman/scalaloader/configurationserializable/transform/Foo$TestEnum;"));
        methodVisitor.visitMethodInsn(INVOKESTATIC, "java/util/EnumSet", "noneOf", "(Ljava/lang/Class;)Ljava/util/EnumSet;", false);
        methodVisitor.visitVarInsn(ASTORE, 0);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(38, label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitVarInsn(ASTORE, 1);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(39, label2);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator", "()Ljava/util/Iterator;", true);
        methodVisitor.visitVarInsn(ASTORE, 2);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitFrame(Opcodes.F_APPEND, 3, new Object[]{"java/util/EnumSet", "java/util/Collection", "java/util/Iterator"}, 0, null);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        Label label4 = new Label();
        methodVisitor.visitJumpInsn(IFEQ, label4);
        methodVisitor.visitVarInsn(ALOAD, 2);
        methodVisitor.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
        methodVisitor.visitTypeInsn(CHECKCAST, "xyz/janboerman/scalaloader/configurationserializable/transform/Foo$TestEnum");
        methodVisitor.visitVarInsn(ASTORE, 3);
        Label label5 = new Label();
        methodVisitor.visitLabel(label5);
        methodVisitor.visitLineNumber(41, label5);
        methodVisitor.visitJumpInsn(GOTO, label3);
        methodVisitor.visitLabel(label4);
        methodVisitor.visitLineNumber(42, label4);
        methodVisitor.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
        methodVisitor.visitInsn(RETURN);
        Label label6 = new Label();
        methodVisitor.visitLabel(label6);
        methodVisitor.visitLocalVariable("enumSet", "Ljava/util/EnumSet;", "Ljava/util/EnumSet<Lxyz/janboerman/scalaloader/configurationserializable/transform/Foo$TestEnum;>;", label1, label6, 0);
        methodVisitor.visitLocalVariable("coll", "Ljava/util/Collection;", "Ljava/util/Collection<Lxyz/janboerman/scalaloader/configurationserializable/transform/Foo$TestEnum;>;", label2, label6, 1);
        methodVisitor.visitMaxs(1, 4);
        methodVisitor.visitEnd();
    }
     */
    private static final void test() {
        EnumSet<TestEnum> enumSet = EnumSet.noneOf(TestEnum.class);
        Collection<TestEnum> coll = enumSet;
        for (TestEnum e : coll) {

        }
    }
}
