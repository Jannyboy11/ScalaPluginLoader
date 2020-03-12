package xyz.janboerman.scalaloader.event.transform;

import static org.objectweb.asm.Opcodes.*;
import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

import org.objectweb.asm.*;

class CancellableTransformer extends ClassVisitor {

    private final ScanResult scanResult;

    CancellableTransformer(ScanResult scanResult, ClassVisitor delegate) {
        super(ASM_API, delegate);
        this.scanResult = scanResult;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
       for (int i = 0; i < interfaces.length; ++i) {
            if (SCALALOADER_CANCELLABLE_NAME.equals(interfaces[i])) {
                interfaces[i] = BUKKIT_CANCELLABLE_NAME;
            }
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (scanResult.implementsScalaLoaderCancellable && !scanResult.hasValidIsCancelled && ISCANCELLED_NAME.equals(name) && ISCANCELLED_DESCRIPTOR.equals(descriptor)) {
            return null; //if the scanresult says isCancelled is invalid, then pretend it doesn't exist
        }

        else if (scanResult.implementsScalaLoaderCancellable && !scanResult.hasValidSetCancelled && SETCANCELLED_NAME.equals(name) && SETCANCELLED_DESCRIPTOR.equals(descriptor)) {
            return null; //if the scanresult says setCancelled is invalid, then pretend it doesn't exist
        }

        else if ("<init>".equals(name)
                && scanResult.implementsScalaLoaderCancellable
                && scanResult.primaryConstructorDescriptors.contains(descriptor)
                && !scanResult.hasValidSetCancelled) {

            //initialize the $cancel field to false

            return new MethodVisitor(ASM7, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                @Override
                public void visitMethodInsn(int opCode, String owner, String name, String descriptor, boolean isInterface) {
                    super.visitMethodInsn(opCode, owner, name, descriptor, isInterface);

                    if (opCode == INVOKESPECIAL) {
                        //inject "this.$cancel = false;" after the super constructor call
                        Label label = new Label();
                        super.visitLabel(label);
                        super.visitVarInsn(ALOAD, 0);   //loads 'this' onto the stack
                        super.visitInsn(ICONST_0);           //loads 'false' onto the stack
                        super.visitFieldInsn(PUTFIELD, scanResult.className, FALLBACK_CANCEL_FIELD_NAME, "Z");
                    }
                }
            };
        }

        else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {
        if (scanResult.implementsScalaLoaderCancellable) {
            assert scanResult.hasValidIsCancelled == scanResult.hasValidSetCancelled;
            boolean generate = !scanResult.hasValidSetCancelled;

            if (generate) {
                {   //generate "private boolean $cancel;"
                    FieldVisitor fv = super.visitField(ACC_PRIVATE, FALLBACK_CANCEL_FIELD_NAME, "Z", null, null);
                    fv.visitEnd();
                }

                //  $cancel field is initialised to false in the primary constructors

                {   //generate "public boolean isCancelled() { return $cancel; }"
                    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, ISCANCELLED_NAME, ISCANCELLED_DESCRIPTOR, null, null);
                    mv.visitCode();
                    Label label0 = new Label();
                    mv.visitLabel(label0);
                    mv.visitVarInsn(ALOAD, 0);                                                      //load 'this' onto the stack
                    mv.visitFieldInsn(GETFIELD, scanResult.className, FALLBACK_CANCEL_FIELD_NAME, "Z"); //load the $cancel field onto the stack
                    mv.visitInsn(IRETURN);
                    Label label1 = new Label();
                    mv.visitLabel(label1);
                    mv.visitLocalVariable("this", "L" + scanResult.className + ';', null, label0, label1, 0);
                    mv.visitMaxs(1, 1);
                    mv.visitEnd();
                }

                {   //generate "public void setCancelled(boolean cancel) { $cancel = cancel; }"
                    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, SETCANCELLED_NAME, SETCANCELLED_DESCRIPTOR, null, null);
                    mv.visitCode();
                    Label label0 = new Label();
                    mv.visitLabel(label0);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitFieldInsn(PUTFIELD, scanResult.className, FALLBACK_CANCEL_FIELD_NAME, "Z");
                    Label label1 = new Label();
                    mv.visitLabel(label1);
                    mv.visitInsn(RETURN);
                    Label label2 = new Label();
                    mv.visitLabel(label2);
                    mv.visitLocalVariable("this", 'L' + scanResult.className + ';', null, label0, label2, 0);
                    mv.visitLocalVariable(FALLBACK_CANCEL_FIELD_NAME, "Z", null, label0, label2, 1);
                    mv.visitMaxs(2, 2);
                    mv.visitEnd();
                }
            }
        }

        super.visitEnd();
    }

}
