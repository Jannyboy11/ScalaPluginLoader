package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.PaperLifecycleEventManager;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.io.File;

import org.jetbrains.annotations.NotNull;

public class ScalaPluginBootstrapContext extends ScalaPluginProviderContext implements BootstrapContext {

    private boolean allowLifecycleEventRegistration = true;
    private final LifecycleEventManager<BootstrapContext> lifecycleEventManager = new PaperLifecycleEventManager<>(this, () -> allowLifecycleEventRegistration);

    public ScalaPluginBootstrapContext(File pluginJarFile, ScalaPluginDescription description) {
        super(pluginJarFile, description);
    }

    // TODO can we be sure that: this manager is actually called when Paper calls event during the boostrap phase?
    // TODO essentially this question is the same as: is this LifecycleEventManager registered with the LifecycleEventRunner.INSTANCE?
    @Override
    public @NotNull LifecycleEventManager<BootstrapContext> getLifecycleManager() {
        return lifecycleEventManager;
    }

    @Override
    public @NotNull PluginMeta getPluginMeta() {
        return getConfiguration();
    }

    public void disallowLifecycleEventRegistration() {
        this.allowLifecycleEventRegistration = false;
    }
}
