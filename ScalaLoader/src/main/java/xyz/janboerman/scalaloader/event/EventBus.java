package xyz.janboerman.scalaloader.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;

/**
 * An event bus for ScalaPlugin {@link Event}s.
 * The instance of this event bus can be obtained in your ScalaPlugin's main class using {@code super.getEventBus()} or {@link ScalaPluginLoader#getEventBus()}
 *
 * @implNote JavaPlugins must not use this class!
 *
 * @see Event
 */
public class EventBus {

    private final PluginManager pluginManager;

    public EventBus(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Calls the event so, allowing listeners to take action based on the event.
     * Returns whether the event is allowed to execute.
     *
     * @param event the event
     * @return true if the event is allowed to happen, otherwise false
     */
    public boolean callEvent(org.bukkit.event.Event event) {
        pluginManager.callEvent(event);
        if (event instanceof Cancellable) {
            return !((Cancellable) event).isCancelled();
        } else {
            return true;
        }
    }

    /**
     * Calls the event, allowing listeners to take actions based on the event.
     * Returns whether the event is allowed to execute.
     *
     * @param event the event
     * @return true if the event is allowed to happen, otherwise false
     *
     * @apiNote this method is not type-safe. The argument must have a type that must be a subtype of {@link Event} or {@link org.bukkit.event.Event}.
     */
    public boolean callEvent(Object event) {
        if (event instanceof org.bukkit.event.Event) {
            return callEvent((org.bukkit.event.Event) event);
        }
        throw new RuntimeException("Called " + getClass().getName() + "#callEvent(" + Object.class.getName() + ") with an " +
                "argument that does not have a strict subtype of " + Event.class.getName() + " or " + org.bukkit.event.Event.class.getName() + "!");
    }

    /**
     * Register a listener. The EventBus will try to created {@link org.bukkit.plugin.EventExecutor}s based on methods in the listener's class that are annotated with {@link org.bukkit.event.EventHandler}.
     * These methods must have a single parameter, that is of a subtype of {@link Event} or {@link org.bukkit.event.Event}.
     *
     * @param listener the event listener
     * @param plugin the plugin for which the EventExecutors are created
     */
    public void registerEvents(Listener listener, Plugin plugin) {
        pluginManager.registerEvents(listener, plugin);
    }

    /**
     * Register an event executor that is executed when an event is called.
     *
     * @param event the event
     * @param listener the event listener
     * @param priority the priority of the executor
     * @param executor the event executor
     * @param plugin the Plugin
     * @param ignoreCancelled whether to ignore cancelled events (false == don't ignore == handle cancelled events)
     */
    public void registerEvent(Class<? extends org.bukkit.event.Event> event, Listener listener, EventPriority priority, org.bukkit.plugin.EventExecutor executor, Plugin plugin, boolean ignoreCancelled) {
        pluginManager.registerEvent(event, listener, priority, executor, plugin, ignoreCancelled);
    }

    /**
     * Register an event executor that is executed when an event is called.
     *
     * @implNote can only be called from within ScalaPlugins!
     *
     * @param event the event
     * @param listener the event listener
     * @param priority the priority of the executor
     * @param executor the event executor
     * @param plugin the Plugin
     * @param ignoreCancelled whether to ignore cancelled events (false == don't ignore == handle cancelled events)
     * @param <E> the event type
     * @param <L> the listener type
     */
    public <L extends Listener, E extends Event> void registerEvent(Class<E> event, L listener, EventPriority priority, EventExecutor<L, E> executor, Plugin plugin, boolean ignoreCancelled) {
        //for scalaplugins this methodcall is replaced by the bukkit-event overloads
        registerEvent((Class<? extends org.bukkit.event.Event>) (Class<?>) event, listener, priority, (org.bukkit.plugin.EventExecutor) executor, plugin, ignoreCancelled);
    }

    /**
     * Register an event executor that is executed when an event is called.
     *
     * @param event the event
     * @param listener the event listener
     * @param priority the priority of the executor
     * @param executor the event executor
     * @param plugin the plugin
     */
    public void registerEvent(Class<? extends org.bukkit.event.Event> event, Listener listener, EventPriority priority, org.bukkit.plugin.EventExecutor executor, Plugin plugin) {
        pluginManager.registerEvent(event, listener, priority, executor, plugin);
    }

    /**
     * Register an event executor that is executed when an event is called.
     *
     * @implNote can only be called from within ScalaPlugins!
     *
     * @param event the event
     * @param listener the event listener
     * @param priority the priority of the executor
     * @param executor the event executor
     * @param plugin the Plugin
     * @param <E> the event type
     * @param <L> the listener type
     */
    public <L extends Listener, E extends Event> void registerEvent(Class<E> event, L listener, EventPriority priority, EventExecutor<L, E> executor, Plugin plugin) {
        //for scalaplugins this methodcall is replaced by the bukkit-event overloads
        registerEvent((Class<? extends org.bukkit.event.Event>) (Class<?>) event, listener, priority, (org.bukkit.plugin.EventExecutor) executor, plugin);
    }
}
