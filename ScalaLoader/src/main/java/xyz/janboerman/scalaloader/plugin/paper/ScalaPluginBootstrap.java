package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.jetbrains.annotations.NotNull;

public class ScalaPluginBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull PluginProviderContext context) {
        //TODO don't think I have anything to do here.
    }

    @Override
    public @NotNull ScalaPlugin createPlugin(@NotNull PluginProviderContext context) {

        //TODO get or create ScalaPluginClassLoader, and instantiate the ScalaPlugin!

        //TODO do I want a different classloader for ScalaPaperPlugins? There are good reasons why I would want this! (the whole dependency thing is completely overhauled)

        //TODO keep in mind that that I need to do bytecode transformations! See ScalaPluginClassLoader, ClassLoaderUtils
        //TODO and I should not forget ScalaPluginUserTransformer

        throw new UnsupportedOperationException("TODO");
    }

}
