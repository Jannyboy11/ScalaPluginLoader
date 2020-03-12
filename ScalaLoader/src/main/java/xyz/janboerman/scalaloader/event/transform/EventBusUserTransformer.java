package xyz.janboerman.scalaloader.event.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

class EventBusUserTransformer extends ClassVisitor {
    EventBusUserTransformer(ClassVisitor delegate) {
        super(ASM_API, delegate);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if ("xyz/janboerman/scalaloader/event/EventBus".equals(owner) && "callEvent".equals(name) && "(Ljava/lang/Object;)Z".equals(descriptor)) {
                    descriptor = "(Lorg/bukkit/event/Event;)Z";
                }

                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        };
    }

}
