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

    // TODO can we be sure that: when the ScalaPlugin is boostrapped, the events that fire during paper plugin bootstrapping are *not* already fired?
    // TODO probably, the answer to this question: we can be sure that this is NOT the case. i.e. those events have already been fired when the ScalaPluginBootstrap runs.
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
