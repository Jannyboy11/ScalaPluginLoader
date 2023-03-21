package xyz.janboerman.scalaloader.plugin.paper.transform;


import org.bukkit.plugin.java.JavaPlugin;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import xyz.janboerman.scalaloader.bytecode.AsmConstants;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.paper.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.paper.description.DescriptionPlugin;

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

    private static final Map<String, String> MAPPINGS = Compat.mapOf(
            Compat.mapEntry(SCALAPLUGIN_CLASS, DESCRIPTIONPLUGIN_CLASS),
            Compat.mapEntry(SCALAPLUGIN_NAME, DESCRIPTIONPLUGIN_NAME),
            Compat.mapEntry(SCALAPLUGIN_DESCRIPTOR, DESCRIPTIONPLUGIN_DESCRIPTOR),

            Compat.mapEntry(SCALAPAPERPLUGIN_CLASS, DESCRIPTIONPLUGIN_CLASS),
            Compat.mapEntry(SCALAPAPERPLUGIN_NAME, DESCRIPTIONPLUGIN_NAME),
            Compat.mapEntry(SCALAPAPERPLUGIN_DESCRIPTOR, DESCRIPTIONPLUGIN_DESCRIPTOR)
    );

    //TODO let the ScalaPluginClassLoader call this. (actually, call this in ClassLoaderUtils, only if we are loaded as a PaperPlugin)
    //TODO alternatively: implement a custom PaperScalaPluginClassLoader.
    MainClassBootstrapTransformer(ClassVisitor delegate) {
        super(AsmConstants.ASM_API, delegate, new SimpleRemapper(MAPPINGS));
    }


}
