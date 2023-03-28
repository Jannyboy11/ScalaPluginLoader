package xyz.janboerman.scalaloader.paper.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionPlugin;

import java.util.Map;

public class MainClassBootstrapTransformer extends ClassRemapper {

    private static final String SCALAPLUGIN_CLASS = "xyz.janboerman.scalaloader.plugin.ScalaPlugin";
    private static final String SCALAPLUGIN_NAME = SCALAPLUGIN_CLASS.replace('.', '/');
    private static final String SCALAPLUGIN_DESCRIPTOR = "L" + SCALAPLUGIN_NAME + ";";

    private static final String SCALAPAPERPLUGIN_CLASS = ScalaPlugin.class.getName();
    private static final String SCALAPAPERPLUGIN_NAME = Type.getInternalName(ScalaPlugin.class);
    private static final String SCALAPAPERPLUGIN_DESCRIPTOR = Type.getDescriptor(ScalaPlugin.class);

    private static final String DESCRIPTIONPLUGIN_CLASS = DescriptionPlugin.class.getName();
    private static final String DESCRIPTIONPLUGIN_NAME = Type.getInternalName(DescriptionPlugin.class);
    private static final String DESCRIPTIONPLUGIN_DESCRIPTOR = Type.getDescriptor(DescriptionPlugin.class);

    private static final String SCALALOADER_CLASS = "xyz.janboerman.scalaloader.ScalaLoader";
    private static final String SCALALOADER_NAME = SCALALOADER_CLASS.replace('.', '/');
    private static final String SCALALOADER_DESCRIPTOR = "L" + SCALALOADER_NAME + ";";
    private static final String SCALAPAPERLOADER_CLASS = ScalaLoader.class.getName();
    private static final String SCALAPAPERLOADER_NAME = Type.getInternalName(ScalaLoader.class);
    private static final String SCALAPAPERLOADER_DESCRIPTOR = Type.getDescriptor(ScalaLoader.class);

    private static final Map<String, String> MAPPINGS = Compat.mapOf(
            Compat.mapEntry(SCALAPLUGIN_CLASS, DESCRIPTIONPLUGIN_CLASS),
            Compat.mapEntry(SCALAPLUGIN_NAME, DESCRIPTIONPLUGIN_NAME),
            Compat.mapEntry(SCALAPLUGIN_DESCRIPTOR, DESCRIPTIONPLUGIN_DESCRIPTOR),

            Compat.mapEntry(SCALAPAPERPLUGIN_CLASS, DESCRIPTIONPLUGIN_CLASS),
            Compat.mapEntry(SCALAPAPERPLUGIN_NAME, DESCRIPTIONPLUGIN_NAME),
            Compat.mapEntry(SCALAPAPERPLUGIN_DESCRIPTOR, DESCRIPTIONPLUGIN_DESCRIPTOR),

            Compat.mapEntry(SCALALOADER_CLASS, SCALAPAPERLOADER_CLASS),
            Compat.mapEntry(SCALALOADER_NAME, SCALAPAPERLOADER_NAME),
            Compat.mapEntry(SCALALOADER_DESCRIPTOR, SCALAPAPERLOADER_DESCRIPTOR)
    );

    public MainClassBootstrapTransformer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, new SimpleRemapper(MAPPINGS));
    }

}
