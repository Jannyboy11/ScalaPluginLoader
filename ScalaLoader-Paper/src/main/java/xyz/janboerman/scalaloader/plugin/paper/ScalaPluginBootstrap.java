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
        ScalaPluginProviderContext scalaContext = (ScalaPluginProviderContext) context;
        ScalaPluginClassLoader classLoader = scalaContext.getPluginClassLoader();
        String main = context.getConfiguration().getMainClass();

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
