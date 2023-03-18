package xyz.janboerman.scalaloader.event.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

class EventExecutorTransformer extends ClassVisitor {

    private boolean implementsScalaLoaderEventExecutor;

    EventExecutorTransformer(ClassVisitor delegate) {
        super(ASM_API, delegate);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (int i = 0; i < interfaces.length; ++i) {
            if (SCALALOADER_EVENTEXECUTOR_NAME.equals(interfaces[i])) {
                interfaces[i] = BUKKIT_EVENTEXECUTOR_NAME;
                implementsScalaLoaderEventExecutor = true;
                break;
            }
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (implementsScalaLoaderEventExecutor
                && EXECUTE_NAME.equals(name)
                && SCALALOADER_EXECUTE_DESCRIPTOR.equals(descriptor)) {

            descriptor = BUKKIT_EXECUTE_DESCRIPTOR;
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}


