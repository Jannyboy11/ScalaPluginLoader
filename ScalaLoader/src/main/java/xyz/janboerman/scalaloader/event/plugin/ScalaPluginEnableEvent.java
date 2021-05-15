package xyz.janboerman.scalaloader.event.plugin;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.PluginEnableEvent;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;

/**
 * ScalaPluginEnable event is called by the {@link ScalaPluginLoader} when {@link ScalaPlugin}s are enabled.
 * In contrast to regular {@link PluginEnableEvent} for {@link org.bukkit.plugin.java.JavaPlugin}s, this event CAN in fact be cancelled.
 */
public class ScalaPluginEnableEvent extends PluginEnableEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancel;

    public ScalaPluginEnableEvent(ScalaPlugin plugin) {
        super(plugin);
    }

    @Override
    public ScalaPlugin getPlugin() {
        return (ScalaPlugin) super.getPlugin();
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
