package xyz.janboerman.scalaloader.paper.transform;

import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import static org.objectweb.asm.Opcodes.*;

import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginLoader;

import java.util.Map;

public class PaperPluginTransformer extends ClassRemapper {

    private static final String SCALAPLUGIN_CLASS = "xyz.janboerman.scalaloader.plugin.ScalaPlugin";
    private static final String SCALAPLUGIN_NAME = SCALAPLUGIN_CLASS.replace('.', '/');
    private static final String SCALAPLUGIN_DESCRIPTOR = "L" + SCALAPLUGIN_NAME + ";";
    private static final String SCALAPAPERPLUGIN_CLASS = ScalaPlugin.class.getName();
    private static final String SCALAPAPERPLUGIN_NAME = Type.getInternalName(ScalaPlugin.class);
    private static final String SCALAPAPERPLUGIN_DESCRIPTOR = Type.getDescriptor(ScalaPlugin.class);

    private static final String SCALALOADER_CLASS = "xyz.janboerman.scalaloader.ScalaLoader";
    private static final String SCALALOADER_NAME = SCALALOADER_CLASS.replace('.', '/');
    private static final String SCALALOADER_DESCRIPTOR = "L" + SCALALOADER_NAME + ";";
    private static final String SCALAPAPERLOADER_CLASS = ScalaLoader.class.getName();
    private static final String SCALAPAPERLOADER_NAME = Type.getInternalName(ScalaLoader.class);
    private static final String SCALAPAPERLOADER_DESCRIPTOR = Type.getDescriptor(ScalaLoader.class);

    private static final String SCALAPLUGINCLASSLOADER_CLASS = "xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader";
    private static final String SCALAPLUGINCLASSLOADER_NAME = SCALAPLUGINCLASSLOADER_CLASS.replace(".", "/");
    private static final String SCALAPLUGINCLASSLOADER_DESCRIPTOR = "L" + SCALAPLUGINCLASSLOADER_NAME + ";";
    private static final String SCALAPAPERPLUGINCLASSLOADER_CLASS = ScalaPluginClassLoader.class.getName();
    private static final String SCALAPAPERPLUGINCLASSLOADER_NAME = Type.getInternalName(ScalaPluginClassLoader.class);
    private static final String SCALAPAPERPLUGINCLASSLOADER_DESCRIPTOR = Type.getInternalName(ScalaPluginClassLoader.class);

    private static final String SCALAPLUGINLOADER_CLASS = "xyz.janboerman.scalaloader.plugin.ScalaPluginLoader";
    private static final String SCALAPLUGINLOADER_NAME = SCALAPLUGINLOADER_CLASS.replace('.', '/');
    private static final String SCALAPLUGINLOADER_DESCRIPTOR = "L" + SCALAPLUGINLOADER_NAME + ";";
    private static final String SCALAPAPERPLUGINLOADER_CLASS = ScalaPluginLoader.class.getName();
    private static final String SCALAPAPERPLUGINLOADER_NAME = Type.getInternalName(ScalaPluginLoader.class);
    private static final String SCALAPAPERPLUGINLOADER_DESCRIPTOR = Type.getDescriptor(ScalaPluginLoader.class);

    private static final String JAVAPLUGIN_NAME = Type.getInternalName(JavaPlugin.class);

    private static final Map<String, String> MAPPINGS = Compat.mapOf(
            Compat.mapEntry(SCALAPLUGIN_CLASS, SCALAPAPERPLUGIN_CLASS),
            Compat.mapEntry(SCALAPLUGIN_NAME, SCALAPAPERPLUGIN_NAME),
            Compat.mapEntry(SCALAPLUGIN_DESCRIPTOR, SCALAPAPERPLUGIN_DESCRIPTOR),

            Compat.mapEntry(SCALALOADER_CLASS, SCALAPAPERLOADER_CLASS),
            Compat.mapEntry(SCALALOADER_NAME, SCALAPAPERLOADER_NAME),
            Compat.mapEntry(SCALALOADER_DESCRIPTOR, SCALAPAPERLOADER_DESCRIPTOR),

            Compat.mapEntry(SCALAPLUGINCLASSLOADER_CLASS, SCALAPAPERPLUGINCLASSLOADER_CLASS),
            Compat.mapEntry(SCALAPLUGINCLASSLOADER_NAME, SCALAPAPERPLUGINCLASSLOADER_NAME),
            Compat.mapEntry(SCALAPLUGINCLASSLOADER_DESCRIPTOR, SCALAPAPERPLUGINCLASSLOADER_DESCRIPTOR),

            Compat.mapEntry(SCALAPLUGINLOADER_CLASS, SCALAPAPERPLUGINLOADER_CLASS),
            Compat.mapEntry(SCALAPLUGINLOADER_NAME, SCALAPAPERPLUGINLOADER_NAME),
            Compat.mapEntry(SCALAPLUGINLOADER_DESCRIPTOR, SCALAPAPERPLUGINLOADER_DESCRIPTOR)
    );

    public PaperPluginTransformer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, new SimpleRemapper(MAPPINGS));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MethodVisitor(AsmConstants.ASM_API, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (INVOKESTATIC == opcode && SCALAPLUGIN_NAME.equals(owner) && "getPlugin".equals(name) && "(Ljava/lang/Class;)Lxyz/janboerman/scalaloader/plugin/ScalaPlugin;".equals(descriptor) && !isInterface) {
                    super.visitMethodInsn(INVOKESTATIC, JAVAPLUGIN_NAME, "getPlugin", "(Ljava/lang/Class;)Lorg/bukkit/plugin/java/JavaPlugin;", false);
                    super.visitTypeInsn(CHECKCAST, SCALAPAPERPLUGIN_NAME);
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            }
        };
    }

}
