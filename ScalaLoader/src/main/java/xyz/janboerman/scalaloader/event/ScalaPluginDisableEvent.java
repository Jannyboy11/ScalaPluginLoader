package xyz.janboerman.scalaloader.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.PluginDisableEvent;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;

public class ScalaPluginDisableEvent extends PluginDisableEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancel;

    public ScalaPluginDisableEvent(ScalaPlugin plugin) {
        super(plugin);
    }

    @Override
    public ScalaPlugin getPlugin() {
        return (ScalaPlugin) super.getPlugin();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
