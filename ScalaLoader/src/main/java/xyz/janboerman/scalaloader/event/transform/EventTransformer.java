package xyz.janboerman.scalaloader.event.transform;

import static org.objectweb.asm.Opcodes.*;
import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

import org.objectweb.asm.*;

class EventTransformer extends ClassVisitor {

    private final ScanResult scanResult;

    EventTransformer(ScanResult result, ClassVisitor classVisitor) {
        super(ASM_API, classVisitor);
        this.scanResult = result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (SCALALOADER_EVENT_NAME.equals(superName)) {
            superName = BUKKIT_EVENT_NAME;
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        if (scanResult.extendsScalaLoaderEvent) {
            boolean generateField = scanResult.staticHandlerListFieldName == null;
            String fieldName = generateField ? FALLBACK_HANDLERLIST_FIELD_NAME : scanResult.staticHandlerListFieldName;

            if (generateField) {
                //generate "private static final HandlerList $HANDLERS;"
                FieldVisitor fieldVisitor = super.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, fieldName, HANDLERLIST_DESCRIPTOR, null, null);
                fieldVisitor.visitEnd();
            }

            if (!scanResult.hasGetHandlerList) {
                //generate "public static HandlerList getHandlerList() { return $HANDLERS; }"
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_STATIC, GETHANDLERLIST_METHODNAME, GETHANDLERLIST_DESCRIPTOR, null, null);
                mv.visitCode();
                Label label = new Label();
                mv.visitLabel(label);
                mv.visitFieldInsn(GETSTATIC, scanResult.className, fieldName, HANDLERLIST_DESCRIPTOR);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(1, 0);
                mv.visitEnd();
            }

            if (!scanResult.hasGetHandlers) {
                //generate "public final HandlerList getHandlers() { return $HANDLERS; }"
                MethodVisitor methodVisitor = super.visitMethod(ACC_PUBLIC | ACC_FINAL, GETHANDLERS_METHODNAME, GETHANDLERS_DESCRIPTOR, null, null);
                methodVisitor.visitCode();
                Label label0 = new Label();
                methodVisitor.visitLabel(label0);
                methodVisitor.visitFieldInsn(GETSTATIC, scanResult.className, fieldName, HANDLERLIST_DESCRIPTOR);
                methodVisitor.visitInsn(ARETURN);
                Label label1 = new Label();
                methodVisitor.visitLabel(label1);
                methodVisitor.visitLocalVariable("this", 'L' + scanResult.className + ';', null, label0, label1, 0);
                methodVisitor.visitMaxs(1, 1);
                methodVisitor.visitEnd();
            }

            if (!scanResult.hasClassInitializer) {
                //generate "static { $HANDLERS = new HandlerList(); }"
                MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                mv.visitCode();
                Label label = new Label();
                mv.visitLabel(label);
                mv.visitTypeInsn(NEW, HANDLERLIST_NAME);
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, HANDLERLIST_NAME, "<init>", "()V", false);
                mv.visitFieldInsn(PUTSTATIC, scanResult.className, fieldName, HANDLERLIST_DESCRIPTOR);
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 0);
                mv.visitEnd();
            }
        }

        super.visitEnd();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (GETHANDLERS_METHODNAME.equals(name) && GETHANDLERS_DESCRIPTOR.equals(descriptor)) {
            //make getHandlers public
            access = (access | ACC_PUBLIC) & ~(ACC_PRIVATE | ACC_PROTECTED);
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        else if (GETHANDLERLIST_METHODNAME.equals(name) && GETHANDLERLIST_DESCRIPTOR.equals(descriptor)) {
            //make getHandlerList public
            access = (access | ACC_PUBLIC) & ~(ACC_PRIVATE | ACC_PROTECTED);
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        else if ("<clinit>".equals(name)
                && scanResult.extendsScalaLoaderEvent
                && scanResult.staticHandlerListFieldName == null) {
            //add initialization of $HANDLERS static field to the class initializer
            return new MethodVisitor(ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {

                @Override
                public void visitCode() {
                    Label label = new Label();
                    super.visitLabel(label);
                    super.visitTypeInsn(NEW, HANDLERLIST_NAME);
                    super.visitInsn(DUP);
                    super.visitMethodInsn(INVOKESPECIAL, HANDLERLIST_NAME, "<init>", "()V", false);
                    super.visitFieldInsn(PUTSTATIC, scanResult.className, FALLBACK_HANDLERLIST_FIELD_NAME, HANDLERLIST_DESCRIPTOR);

                    super.visitCode();
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    maxStack = Math.max(2, maxStack);
                    super.visitMaxs(maxStack, maxLocals);
                }
            };
        }

        else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

}
