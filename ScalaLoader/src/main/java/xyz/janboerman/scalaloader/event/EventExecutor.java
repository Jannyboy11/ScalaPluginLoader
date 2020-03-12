package xyz.janboerman.scalaloader.event;

import org.bukkit.event.Listener;

/**
 * An event executor. Can be implemented using a lambda from your ScalaPlugin.
 *
 * @param <E> the event type
 * @param <L> the listener type
 *
 * @see EventBus
 */
@FunctionalInterface
public interface EventExecutor<L extends Listener, E extends Event> {

    /**
     * The callback method that is executed when the event is called.
     * @param listener the listener
     * @param event the event
     */
    public void execute(L listener, E event);

}
