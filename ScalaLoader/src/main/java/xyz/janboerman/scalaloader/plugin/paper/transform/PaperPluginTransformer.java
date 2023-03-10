package xyz.janboerman.scalaloader.plugin.paper.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.plugin.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPlugin;

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

    private static final Map<String, String> MAPPINGS = Compat.mapOf(
            Compat.mapEntry(SCALAPLUGIN_CLASS, SCALAPAPERPLUGIN_CLASS),
            Compat.mapEntry(SCALAPLUGIN_NAME, SCALAPAPERPLUGIN_NAME),
            Compat.mapEntry(SCALAPLUGIN_DESCRIPTOR, SCALAPAPERPLUGIN_DESCRIPTOR),

            Compat.mapEntry(SCALALOADER_CLASS, SCALAPAPERLOADER_CLASS),
            Compat.mapEntry(SCALALOADER_NAME, SCALAPAPERLOADER_NAME),
            Compat.mapEntry(SCALALOADER_DESCRIPTOR, SCALAPAPERLOADER_DESCRIPTOR)
    );

    //TODO let the ScalaPluginClassLoader call this. (actually, call this in ClassLoaderUtils, only if we are loaded as a PaperPlugin)
    //TODO alternatively: implement a custom PaperScalaPluginClassLoader.
    PaperPluginTransformer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, new SimpleRemapper(MAPPINGS));
    }

}
