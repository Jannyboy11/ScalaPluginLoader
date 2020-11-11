package xyz.janboerman.scalaloader.event.transform;

import org.objectweb.asm.*;
import xyz.janboerman.scalaloader.event.Cancellable;


import static org.objectweb.asm.Opcodes.*;
import static xyz.janboerman.scalaloader.event.transform.EventTransformations.*;

class EventScanner extends ClassVisitor {

    private final ScanResult result = new ScanResult();

    EventScanner() {
        super(ASM_API);
    }

    ScanResult scan(ClassReader classReader) throws EventError {
        classReader.accept(this, ClassReader.EXPAND_FRAMES);

        if (result.implementsScalaLoaderCancellable && (result.hasValidSetCancelled != result.hasValidIsCancelled)) {
            throw new EventError("Event class " + result.className.replace('/', '.') + " implements " + Cancellable.class.getName() + ", "
                    + "but only overrides " + (result.hasValidSetCancelled ? "setCancelled" : "isCancelled") + ". "
                    + "You need to either override both isCancelled and setCancelled, or none of the two.");
        }

        return result;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        result.className = name;

        //replace scalaloader-event by bukkit-event
        if (SCALALOADER_EVENT_NAME.equals(superName)) {
            result.extendsScalaLoaderEvent = true;
        }

        //replace scalaloader-cancellable by bukkit-cancellable
        for (int i = 0; i < interfaces.length; ++i) {
            if (SCALALOADER_CANCELLABLE_NAME.equals(interfaces[i])) {
                result.implementsScalaLoaderCancellable = true;
                break;
            }
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if ((access & ACC_STATIC) == ACC_STATIC && HANDLERLIST_DESCRIPTOR.equals(descriptor)) {
            result.staticHandlerListFieldName = name;
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (GETHANDLERLIST_METHODNAME.equals(name) && (access & ACC_STATIC) == ACC_STATIC && GETHANDLERLIST_DESCRIPTOR.equals(descriptor)) {
            result.hasGetHandlerList = true;
        }

        else if (GETHANDLERS_METHODNAME.equals(name) && (access & ACC_STATIC) == 0 && GETHANDLERS_DESCRIPTOR.equals(descriptor)) {
            result.hasGetHandlers = true;
        }

        else if (SETCANCELLED_NAME.equals(name) && (access & ACC_STATIC) == 0 && SETCANCELLED_DESCRIPTOR.equals(descriptor)) {
            result.hasValidSetCancelled = true;

            return new MethodVisitor(ASM_API) {
                @Override
                public void visitMethodInsn(int opCode, String owner, String name, String descriptor, boolean isInterface) {
                    if (opCode == INVOKESPECIAL && SCALALOADER_CANCELLABLE_NAME.equals(owner) && SETCANCELLED_NAME.equals(name) && SETCANCELLED_DESCRIPTOR.equals(descriptor) && isInterface) {
                        //encountered a scala-compiler generated call to the default method of the interface
                        //why does scalac even output this crap? interfaces are not traits.
                        result.hasValidSetCancelled = false;
                    }
                }
            };
        }

        else if (ISCANCELLED_NAME.equals(name) && (access & ACC_STATIC) == 0 && ISCANCELLED_DESCRIPTOR.equals(descriptor)) {
            result.hasValidIsCancelled = true;

            return new MethodVisitor(ASM_API) {
                @Override
                public void visitMethodInsn(int opCode, String owner, String name, String descriptor, boolean isInterface) {
                    if (opCode == INVOKESPECIAL && SCALALOADER_CANCELLABLE_NAME.equals(owner) && ISCANCELLED_NAME.equals(name) && ISCANCELLED_DESCRIPTOR.equals(descriptor) && isInterface) {
                        //encountered a scala-compiler generated call to the default method of the interface
                        //why does scalac even output this crap? interfaces are not traits.
                        result.hasValidIsCancelled = false;
                    }
                }
            };
        }

        else if ("<init>".equals(name)) {
            return new MethodVisitor(ASM_API) {
                boolean isPrimaryConstructor = true;

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if (opcode == INVOKESPECIAL && "<init>".equals(name) && owner.equals(result.className)) {
                        //constructor is calling this(params..) instead of super(params..)
                        isPrimaryConstructor = false;
                    }
                }

                @Override
                public void visitEnd() {
                    if (isPrimaryConstructor) {
                        result.primaryConstructorDescriptors.add(descriptor);
                    }
                }
            };
        }

        else if ("<clinit>".equals(name)) {
            result.hasClassInitializer = true;
        }

        return null;
    }

}
