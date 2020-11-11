package xyz.janboerman.scalaloader.configurationserializable.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;

import static xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable.InjectionPoint;
import static xyz.janboerman.scalaloader.configurationserializable.transform.ConfigurationSerializableTransformations.*;

public class PluginTransformer extends ClassVisitor {

    private final InjectionPoint injectionPoint;
    private final String configurationSerializableClassName;
    private final String mainClassName;

    private boolean hasOnEnable;
    private boolean hasOnLoad;
    private boolean hasConstructor;
    private boolean hasClassInitializer;

    private boolean weAreTransformingTheMainClass;

    PluginTransformer(ClassVisitor delegate, InjectionPoint injectionPoint, String configSerType, String mainClassName) {
        super(ASM_API, delegate);
        this.injectionPoint = injectionPoint;
        this.configurationSerializableClassName = configSerType;
        this.mainClassName = mainClassName;
    }

    public static PluginTransformer of(ClassVisitor delegate, GlobalScanResult globalScanResult, String mainClassName) {
        if (!globalScanResult.annotatedByConfigurationSerializable) return null;

        return new PluginTransformer(delegate, globalScanResult.registerAt, globalScanResult.className, mainClassName);
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
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, false);
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
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, false);
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
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, false);
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
                            visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, false);
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
                methodVisitor.visitMethodInsn(INVOKESTATIC, configurationSerializableClassName, REGISTER_NAME, REGISTER_DESCRIPTOR, false);
                methodVisitor.visitInsn(RETURN);
                methodVisitor.visitMaxs(0, 0);
                methodVisitor.visitEnd();
            }
        }

        super.visitEnd();
    }


}
