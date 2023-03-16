package xyz.janboerman.scalaloader.plugin.paper.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.plugin.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPluginLoader;

import java.util.Map;

public class PaperPluginTransformer extends ClassRemapper {

    private static final String SCALAPLUGIN_CLASS = xyz.janboerman.scalaloader.plugin.ScalaPlugin.class.getName();
    private static final String SCALAPLUGIN_NAME = Type.getInternalName(xyz.janboerman.scalaloader.plugin.ScalaPlugin.class);
    private static final String SCALAPLUGIN_DESCRIPTOR = Type.getDescriptor(xyz.janboerman.scalaloader.plugin.ScalaPlugin.class);
    private static final String SCALAPAPERPLUGIN_CLASS = ScalaPlugin.class.getName();
    private static final String SCALAPAPERPLUGIN_NAME = Type.getInternalName(ScalaPlugin.class);
    private static final String SCALAPAPERPLUGIN_DESCRIPTOR = Type.getDescriptor(ScalaPlugin.class);

    private static final String SCALALOADER_CLASS = xyz.janboerman.scalaloader.ScalaLoader.class.getName();
    private static final String SCALALOADER_NAME = Type.getInternalName(xyz.janboerman.scalaloader.ScalaLoader.class);
    private static final String SCALALOADER_DESCRIPTOR = Type.getDescriptor(xyz.janboerman.scalaloader.ScalaLoader.class);
    private static final String SCALAPAPERLOADER_CLASS = ScalaLoader.class.getName();
    private static final String SCALAPAPERLOADER_NAME = Type.getInternalName(ScalaLoader.class);
    private static final String SCALAPAPERLOADER_DESCRIPTOR = Type.getDescriptor(ScalaLoader.class);

    private static final String SCALAPLUGINCLASSLOADER_CLASS = xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader.class.getName();
    private static final String SCALAPLUGINCLASSLOADER_NAME = Type.getInternalName(xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader.class);
    private static final String SCALAPLUGINCLASSLOADER_DESCRIPTOR = Type.getDescriptor(xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader.class);
    private static final String SCALAPAPERPLUGINCLASSLOADER_CLASS = ScalaPluginClassLoader.class.getName();
    private static final String SCALAPAPERPLUGINCLASSLOADER_NAME = Type.getInternalName(ScalaPluginClassLoader.class);
    private static final String SCALAPAPERPLUGINCLASSLOADER_DESCRIPTOR = Type.getInternalName(ScalaPluginClassLoader.class);

    private static final String SCALAPLUGINLOADER_CLASS = xyz.janboerman.scalaloader.plugin.ScalaPluginLoader.class.getName();
    private static final String SCALAPLUGINLOADER_NAME = Type.getInternalName(xyz.janboerman.scalaloader.plugin.ScalaPluginLoader.class);
    private static final String SCALAPLUGINLOADER_DESCRIPTOR = Type.getDescriptor(xyz.janboerman.scalaloader.plugin.ScalaPluginLoader.class);
    private static final String SCALAPAPERPLUGINLOADER_CLASS = ScalaPluginLoader.class.getName();
    private static final String SCALAPAPERPLUGINLOADER_NAME = Type.getInternalName(ScalaPluginLoader.class);
    private static final String SCALAPAPERPLUGINLOADER_DESCRIPTOR = Type.getDescriptor(ScalaPluginLoader.class);

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

    //TODO let the ScalaPluginClassLoader call this. (actually, call this in ClassLoaderUtils, only if we are loaded as a PaperPlugin)
    //TODO alternatively: implement a custom PaperScalaPluginClassLoader.
    PaperPluginTransformer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, new SimpleRemapper(MAPPINGS));
    }

}
