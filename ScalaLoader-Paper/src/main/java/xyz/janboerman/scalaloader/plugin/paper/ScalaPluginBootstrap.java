package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.plugin.ScalaCompatMap;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.paper.description.ScalaDependency;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

/*  According to https://docs.papermc.io/paper/dev/getting-started/paper-plugins#bootstrapper:
 *  The purpose of a bootstrapper is override how a plugin is initiated.
 */
public class ScalaPluginBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull PluginProviderContext context) {
        //context provides access to:
        //  - configuration     (ScalaPluginMeta)
        //  - logger            (ComponentLogger)
        //  - data directory    (Path)

        //Reset the Scala version to the latest known compatible version.
        ScalaCompatMap<ScalaDependency> compatMap = ScalaLoader.getInstance().getScalaVersions();
        ScalaPluginMeta scalaPluginMeta = (ScalaPluginMeta) context.getConfiguration();
        scalaPluginMeta.description.setScalaVersion(compatMap.getLatestVersion(scalaPluginMeta.getScalaVersion()).getVersionString());

    }

    @Override
    public @NotNull ScalaPlugin createPlugin(@NotNull PluginProviderContext context) {

        //TODO do I want a different classloader for ScalaPaperPlugins? There are good reasons why I would want this! (the whole dependency thing is completely overhauled)
        //TODO might not need a different classloader, because Paper's PaperSimplePluginClassLoader uses the ServiceLoader api to load a ClassLaoderBytecodeModifier!
        //TODO the bytecode modifier that will be used, is the modifier that is found first by the classloader's getResources method.
        //TODO there seems to be a bug in Paper's current implementation: it does not uses the Platforms bytecode modifier yet (UnsafeValues#processClass or Commodore#convert)

        //TODO keep in mind that that I need to do bytecode transformations! See ScalaPluginClassLoader, ClassLoaderUtils
        //TODO and I should not forget ScalaPluginUserTransformer

        ScalaPluginMeta description = (ScalaPluginMeta) context.getConfiguration();

        //TODO set up PaperPluginParent?

        ScalaPluginClassLoader classLoader = null; //TODO new ScalaPluginClassLoader();
        String main = description.getMainClass();

        try {
            Class<? extends ScalaPlugin> scalaPluginClazz = (Class<? extends ScalaPlugin>) Class.forName(main, false, classLoader);
            ScalaPlugin plugin = ScalaLoaderUtils.createScalaPluginInstance(scalaPluginClazz);

            ScalaLoader.getInstance().addScalaPlugin(plugin);

            return plugin;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find plugin main class: " + main, e);
        } catch (ScalaPluginLoaderException e) {
            throw new RuntimeException("Could create plugin instance for plugin class: " + main, e);
        }
    }

}
