package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.configuration.LifecycleEventHandlerConfiguration;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.io.File;

import org.jetbrains.annotations.NotNull;

public class ScalaPluginBootstrapContext extends ScalaPluginProviderContext implements BootstrapContext {

    //TODO should perhaps use PaperLifecycleEventManager instead, since it contains some magic knowledge about the handlerConfiguration.
    //TODO when should our booleanSupplier return false tho? I guess when all ScalaPlugins finished loading.
    //TODO check https://gist.github.com/Machine-Maker/8e3fc6063c98e81cae7cee1ac230936f
    private final LifecycleEventManager<BootstrapContext> lifecycleEventManager = new LifecycleEventManager<BootstrapContext>() {
        @Override
        public void registerEventHandler(
                @NotNull LifecycleEventHandlerConfiguration<? super BootstrapContext> handlerConfiguration) {
            //TODO this is currently not called. when should this be called? should it be called at all?
        }
    };

    public ScalaPluginBootstrapContext(File pluginJarFile, ScalaPluginDescription description) {
        super(pluginJarFile, description);
    }

    @Override
    public @NotNull LifecycleEventManager<BootstrapContext> getLifecycleManager() {
        return lifecycleEventManager;
    }

    @Override
    public @NotNull PluginMeta getPluginMeta() {
        return getConfiguration();
    }
}
