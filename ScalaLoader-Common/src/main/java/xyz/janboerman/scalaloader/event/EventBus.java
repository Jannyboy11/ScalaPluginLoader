package xyz.janboerman.scalaloader.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import xyz.janboerman.scalaloader.bytecode.Called;
import xyz.janboerman.scalaloader.bytecode.Replaced;
import xyz.janboerman.scalaloader.event.transform.EventError;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoader;

/**
 * An event bus for ScalaPlugin {@link Event}s.
 * The instance of this event bus can be obtained in your ScalaPlugin's main class using {@code super.getEventBus()} or {@link ScalaPluginLoader#getEventBus()}
 *
 * @apiNote JavaPlugins should not use this class, they are better off using Bukkit's PluginManager api.
 * @implNote Some of the internals of this class rely on the fact that the bytecode of ScalaPlugins is transformed using
 *      {@link xyz.janboerman.scalaloader.event.transform.EventTransformations#transform(byte[], ClassLoader)} before they are "defined" by the classloader.
 *
 * @see Event
 */
public class EventBus {

    private final PluginManager pluginManager;

    /**
     * Construct the event bus.
     *
     * @deprecated not meant to be constructed explicitly. Use {@link ScalaPlugin#getEventBus()} or {@link ScalaPluginLoader#getEventBus()}.
     * @param pluginManager the server's PluginManager
     */
    @Deprecated
    public EventBus(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Calls the event so, allowing listeners to take action based on the event.
     * Returns whether the event is allowed to execute.
     *
     * @param event the event
     * @return true if the event is allowed to happen, otherwise false
     * @apiNote if the event does not implement {@link Cancellable}, true is always returned.
     */
    @Called
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
     * @apiNote this method is not type-safe. The argument's type must be a subtype of either {@link Event} or {@link org.bukkit.event.Event}.
     * @apiNote if the event does not implement {@link xyz.janboerman.scalaloader.event.Cancellable} or {@link Cancellable}, true is always returned.
     * @throws RuntimeException if the type of the passed argument is not a subtype of {@link Event} or {@link org.bukkit.event.Event}.
     */
    @Replaced
    public boolean callEvent(Object event) {
        if (event instanceof org.bukkit.event.Event) {
            return callEvent((org.bukkit.event.Event) event);
        }
        throw new RuntimeException("Called " + getClass().getName() + "#callEvent(" + Object.class.getName() + ") with an " +
                "argument that does not have a type that is a subtype of " + Event.class.getName() + " or " + org.bukkit.event.Event.class.getName() + "!");
    }

    /**
     * Register a listener. The EventBus will try to create {@link org.bukkit.plugin.EventExecutor}s based on methods in the listener's class that are annotated with {@link org.bukkit.event.EventHandler}.
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
     * @param ignoreCancelled whether to ignore cancelled events (false == don't ignore == handle cancelled events too)
     */
    @Called
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
     * @param ignoreCancelled whether to ignore cancelled events (false == don't ignore == handle cancelled events too)
     * @param <E> the event type
     * @param <L> the listener type
     */
    @Replaced
    public <L extends Listener, E extends Event> void registerEvent(Class<E> event, L listener, EventPriority priority, EventExecutor<L, E> executor, Plugin plugin, boolean ignoreCancelled) {
        if (executor instanceof org.bukkit.plugin.EventExecutor) {
            registerEvent((Class<? extends org.bukkit.event.Event>) (Class<?>) event, listener, priority, (org.bukkit.plugin.EventExecutor) executor, plugin, ignoreCancelled);
        } else {
            throw new EventError("Cannot implement " + EventExecutor.class.getName() + " from your JavaPlugin!, use Bukkit's EventHandler reflection API instead!");
        }
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
    @Called
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
    @Replaced
    public <L extends Listener, E extends Event> void registerEvent(Class<E> event, L listener, EventPriority priority, EventExecutor<L, E> executor, Plugin plugin) {
        if (executor instanceof org.bukkit.plugin.EventExecutor) {
            registerEvent((Class<? extends org.bukkit.event.Event>) (Class<?>) event, listener, priority, (org.bukkit.plugin.EventExecutor) executor, plugin);
        } else {
            throw new EventError("Cannot implement " + EventExecutor.class.getName() + " from your JavaPlugin!, use Bukkit's EventHandler reflection API instead!");
        }
    }
}
