package xyz.janboerman.scalaloader.paper.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;

public class MainClassCallerMigrator extends ClassVisitor {

    private final String mainClassName;
    private final String asmMainClassName;

    public MainClassCallerMigrator(ClassVisitor classVisitor, String mainClassName) {
        super(AsmConstants.ASM_API, classVisitor);
        this.mainClassName = mainClassName;
        this.asmMainClassName = mainClassName.replace('.', '/');
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(AsmConstants.ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (asmMainClassName.equals(owner) && name.equals("getClassLoader")) {
                    super.visitMethodInsn(opcode, asmMainClassName, "classLoader", descriptor, isInterface);
                } else if (asmMainClassName.equals(owner) && name.equals("getPluginLoader")) {
                    super.visitMethodInsn(opcode, asmMainClassName, "pluginLoader", descriptor, isInterface);
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        };
    }

}
