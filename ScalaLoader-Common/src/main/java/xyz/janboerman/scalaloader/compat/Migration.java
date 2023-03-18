package xyz.janboerman.scalaloader.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.bytecode.Called;
import static xyz.janboerman.scalaloader.compat.Compat.*;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterType;
import xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.Permission;

import java.util.Collection;

/**
 * This class is NOT part of the public API!
 */
public class Migration {

    private Migration() {}

    public static byte[] transform(byte[] byteCode, ClassLoader pluginClassLoader) {
        ClassWriter classWriter = new ClassWriter(0) {
            @Override
            protected ClassLoader getClassLoader() {
                return pluginClassLoader;
            }
        };

        ClassVisitor combinedTransformer = classWriter;

        combinedTransformer = new NumericRangeUpdater(combinedTransformer);
        combinedTransformer = new PermissionGetChildrenReplacer(combinedTransformer);
        combinedTransformer = new RuntimeConversionsReplacer(combinedTransformer);
        combinedTransformer = new PluginEventReplacer(combinedTransformer);
        //there is room for more here.

        new ClassReader(byteCode).accept(combinedTransformer, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    @Called
    public static Collection<Permission> legacyGetChildren(Permission permission) {
        return permission.getChildren().keySet();
    }
}

class NumericRangeUpdater extends ClassRemapper {

    private static final NumericRangeRemapper NUMERIC_RANGE_MIGRATION = new NumericRangeRemapper();

    NumericRangeUpdater(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, NUMERIC_RANGE_MIGRATION);
    }

    private static final class NumericRangeRemapper extends SimpleRemapper {

        private static final String CONFIGURATION_SERIALIZABLE_RUNTIME_PACKAGE = "xyz/janboerman/scalaloader/configurationserializable/runtime";
        private static final String TYPES_PACKAGE = CONFIGURATION_SERIALIZABLE_RUNTIME_PACKAGE + "/types";

        private static final String OLD_NUMERIC_RANGE = CONFIGURATION_SERIALIZABLE_RUNTIME_PACKAGE + "/NumericRange";
        private static final String NEW_NUMERIC_RANGE = TYPES_PACKAGE + "/NumericRange";
        private static final String OLD_BYTE_RANGE = OLD_NUMERIC_RANGE + "$OfByte";
        private static final String NEW_BYTE_RANGE = NEW_NUMERIC_RANGE + "$OfByte";
        private static final String OLD_SHORT_RANGE = OLD_NUMERIC_RANGE + "$OfShort";
        private static final String NEW_SHORT_RANGE = NEW_NUMERIC_RANGE + "$OfShort";
        private static final String OLD_INT_RANGE = OLD_NUMERIC_RANGE + "$OfInteger";
        private static final String NEW_INT_RANGE = NEW_NUMERIC_RANGE + "$OfInteger";
        private static final String OLD_LONG_RANGE = OLD_NUMERIC_RANGE + "$OfLong";
        private static final String NEW_LONG_RANGE = NEW_NUMERIC_RANGE + "$OfLong";
        private static final String OLD_BIG_INTEGER_RANGE = OLD_NUMERIC_RANGE + "$OfBigInteger";
        private static final String NEW_BIG_INTEGER_RANGE = NEW_NUMERIC_RANGE + "$OfBigInteger";

        private NumericRangeRemapper() {
            super(mapOf(
                    mapEntry(OLD_NUMERIC_RANGE, NEW_NUMERIC_RANGE),
                    mapEntry(OLD_BYTE_RANGE, NEW_BYTE_RANGE),
                    mapEntry(OLD_SHORT_RANGE, NEW_SHORT_RANGE),
                    mapEntry(OLD_INT_RANGE, NEW_INT_RANGE),
                    mapEntry(OLD_LONG_RANGE, NEW_LONG_RANGE),
                    mapEntry(OLD_BIG_INTEGER_RANGE, NEW_BIG_INTEGER_RANGE)
            ));
        }
    }

}

class PermissionGetChildrenReplacer extends ClassVisitor {

    private static final String SCALALOADER_PERMISSION_NAME = Type.getInternalName(ScalaPluginDescription.Permission.class);
    private static final String GETCHILDREN_NAME = "getChildren";
    private static final String GETCHILDREN_DESCRIPTOR = "()" + Type.getDescriptor(Collection.class);
    private static final String LEGACYGETCHILDREN_DESCRIPTOR = "(" + Type.getDescriptor(ScalaPluginDescription.Permission.class) + ")" + Type.getDescriptor(Collection.class);

    PermissionGetChildrenReplacer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(AsmConstants.ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (SCALALOADER_PERMISSION_NAME.equals(owner) && GETCHILDREN_NAME.equals(name) && GETCHILDREN_DESCRIPTOR.equals(descriptor)) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Migration.class), "legacyGetChildren", LEGACYGETCHILDREN_DESCRIPTOR, false);
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        };
    }

}

class RuntimeConversionsReplacer extends ClassVisitor {

    private static final String RUNTIMECONVERSIONS_NAME = Type.getInternalName(RuntimeConversions.class);
    private static final String OLD_DESCRIPTOR = "(" + AsmConstants.javaLangObject_DESCRIPTOR + Type.getDescriptor(ParameterType.class) + "Lxyz/janboerman/scalaloader/plugin/ScalaPluginClassLoader;)" + AsmConstants.javaLangObject_DESCRIPTOR;
    private static final String NEW_DESCRIPTOR = "(" + AsmConstants.javaLangObject_DESCRIPTOR + Type.getDescriptor(ParameterType.class) + Type.getDescriptor(ClassLoader.class) + ")" + AsmConstants.javaLangObject_DESCRIPTOR;

    RuntimeConversionsReplacer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(AsmConstants.ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKESTATIC
                        && RUNTIMECONVERSIONS_NAME.equals(owner)
                        && ("serialize".equals(name) || "deserialize".equals(name))  //both methods use the same signature.
                        && OLD_DESCRIPTOR.equals(descriptor)) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, RUNTIMECONVERSIONS_NAME, name, NEW_DESCRIPTOR, isInterface);
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        };
    }
}

class PluginEventReplacer extends ClassVisitor {

    private static final String ENABLE_EVENT = "xyz/janboerman/scalaloader/event/plugin/ScalaPluginEnableEvent";
    private static final String DISABLE_EVENT = "xyz/janboerman/scalaloader/event/plugin/ScalaPluginDisableEvent";
    private static final String OLD_GETPLUGIN_DESCRIPTOR = "()Lxyz/janboerman/scalaloader/plugin/ScalaPlugin;";
    private static final String NEW_GETPLUGIN_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(IScalaPlugin.class));
    private static final String GETPLUGIN_NAME = "getPlugin";

    PluginEventReplacer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        //if a plugin calls ScalaPluginEnableEvent#getPlugin() -> ScalaPlugin, replace the call by calling to ScalaPluginEnableEvent#getPlugin() -> IScalaPlugin instead.
        //to keep compatibility, cast to ScalaPlugin again. This is a best-effort approach, it will fail if the plugin in a paper.ScalaPlugin.

        return new MethodVisitor(AsmConstants.ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKEVIRTUAL
                        && (ENABLE_EVENT.equals(owner) || DISABLE_EVENT.equals(owner))
                        && GETPLUGIN_NAME.equals(name)
                        && OLD_GETPLUGIN_DESCRIPTOR.equals(descriptor)
                        && !isInterface) {
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, GETPLUGIN_NAME, NEW_GETPLUGIN_DESCRIPTOR, false);
                    super.visitTypeInsn(Opcodes.CHECKCAST, "xyz/janboerman/scalaloader/plugin/ScalaPlugin");
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        };
    }
}
