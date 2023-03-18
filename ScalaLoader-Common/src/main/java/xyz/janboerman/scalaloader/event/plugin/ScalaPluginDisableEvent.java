package xyz.janboerman.scalaloader.event.plugin;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;

/**
 * ScalaPluginEnable event is called by the {@link IScalaPluginLoader} when {@link IScalaPlugin}s are disabled.
 * In contrast to regular {@link PluginEnableEvent} for {@link org.bukkit.plugin.java.JavaPlugin}s, this event CAN in fact be cancelled.
 */
public class ScalaPluginDisableEvent extends PluginDisableEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancel;

    public ScalaPluginDisableEvent(IScalaPlugin plugin) {
        super(plugin);
    }

    @Override
    public IScalaPlugin getPlugin() {
        return (IScalaPlugin) super.getPlugin();
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
