package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import xyz.janboerman.scalaloader.configurationserializable.InjectionPoint;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;
import xyz.janboerman.scalaloader.plugin.TransformerRegistry;

public class PluginTransformer extends ClassVisitor {

    private final InjectionPoint injectionPoint;
    private final String configurationSerializableClassName;    //using slashes as separator, e.g.: com/example/Foo
    private final String mainClassName;                         //using dots as separator, e.g.:    com.example.Bar
    private final boolean serializableClassIsInterface;

    private boolean hasOnEnable;
    private boolean hasOnLoad;
    private boolean hasConstructor;
    private boolean hasClassInitializer;

    private boolean weAreTransformingTheMainClass;

    PluginTransformer(ClassVisitor delegate, InjectionPoint injectionPoint, String configSerType, boolean configSerIsInterface, String mainClassName) {
        super(ASM_API, delegate);
        this.injectionPoint = injectionPoint;
        this.configurationSerializableClassName = configSerType;
        this.serializableClassIsInterface = configSerIsInterface;
        this.mainClassName = mainClassName; //TODO probably not needed anymore since the new TransformerRegistry changes!
    }

    public static void addTo(TransformerRegistry transformerRegistry, GlobalScanResult scanResult) {
        if (scanResult.annotatedByConfigurationSerializable
                || scanResult.annotatedByDelegateSerialization) {

            //TODO I could check for the InjectionPoint here already.

            transformerRegistry.addMainClassTransformer((delegate, mainClassName) ->
                    new PluginTransformer(delegate, scanResult.registerAt, scanResult.className, scanResult.isInterface, mainClassName));
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (name.equals(mainClassName.replace('.', '/'))) {
            weAreTransformingTheMainClass = true;
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor superVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (weAreTransformingTheMainClass) {
            if ("onEnable".equals(name) && "()V".equals(descriptor)) {
                hasOnEnable = true;

                if (injectionPoint == InjectionPoint.PLUGIN_ONENABLE) {
                    return new MethodVisitor(ASM_API, superVisitor) {
                        @Override
                        public void visitCode() {
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, serializableClassIsInterface);
                            super.visitCode();
                        }
                    };
                }
            }

            else if ("onLoad".equals(name) && "()V".equals((descriptor))) {
                hasOnLoad = true;

                if (injectionPoint == InjectionPoint.PLUGIN_ONLOAD) {
                    return new MethodVisitor(ASM_API, superVisitor) {
                        @Override
                        public void visitCode() {
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, serializableClassIsInterface);
                            super.visitCode();
                        }
                    };
                }
            }

            else if ("<init>".equals(name)) {
                hasConstructor = true;

                if (injectionPoint == InjectionPoint.PLUGIN_CONSTRUCTOR) {
                    return new MethodVisitor(ASM_API, superVisitor) {
                        @Override
                        public void visitCode() {
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, serializableClassIsInterface);
                            super.visitCode();
                        }
                    };
                }
            }

            else if ("<clinit>".equals(name)) {
                hasClassInitializer = true;

                if (injectionPoint == InjectionPoint.PLUGIN_CLASS_INTIALIZER) {
                    return new MethodVisitor(ASM_API, superVisitor) {
                        @Override
                        public void visitCode() {
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, serializableClassIsInterface);
                            super.visitCode();
                        }
                    };
                }
            }
        }

        return superVisitor;
    }

    @Override
    public void visitEnd() {
        if (weAreTransformingTheMainClass) {
            MethodVisitor methodVisitor = null;
            if (injectionPoint == InjectionPoint.PLUGIN_ONENABLE && !hasOnEnable) {
                methodVisitor = visitMethod(ACC_PUBLIC, "onEnable", "()V", null, null);
            } else if (injectionPoint == InjectionPoint.PLUGIN_ONLOAD && !hasOnLoad) {
                methodVisitor = visitMethod(ACC_PUBLIC, "onLoad", "()V", null, null);
            } else if (injectionPoint == InjectionPoint.PLUGIN_CONSTRUCTOR && !hasConstructor) {
                methodVisitor = visitMethod(ACC_PUBLIC, CONSTRUCTOR_NAME, "()V", null, null);
            } else if (injectionPoint == InjectionPoint.PLUGIN_CLASS_INTIALIZER && !hasClassInitializer) {
                methodVisitor = visitMethod(ACC_PUBLIC | ACC_STATIC, CLASS_INIT_NAME, "()V", null, null);
            }

            if (methodVisitor != null) {
                methodVisitor.visitCode();
                methodVisitor.visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, serializableClassIsInterface);
                methodVisitor.visitInsn(RETURN);
                methodVisitor.visitMaxs(0, 0);
                methodVisitor.visitEnd();
            }
        }

        super.visitEnd();
    }


}
