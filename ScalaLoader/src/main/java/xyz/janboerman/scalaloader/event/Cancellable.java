package xyz.janboerman.scalaloader.event;

import xyz.janboerman.scalaloader.bytecode.Replaced;

/**
 * Alternative to {@link org.bukkit.event.Cancellable} that aims to reduce boilerplate.
 * If your event implements this interface instead, the ScalaPluginClassLoader will inject the following three members:
 * <ul>
 *     <li>a field: <code>private boolean $cancel;</code></li>
 *     <li>a method: <code>public boolean isCancelled() { return this.$cancel; }</code></li>
 *     <li>a method: <code>public void setCancelled(boolean cancel) { this.$cancel = cancel; }</code></li>
 * </ul>
 * You can think of it as having the same effect as extending the following trait:
 * <pre>
 *     <code>
 *      trait Cancellable extends org.bukkit.event.Cancellable { self: Event =>
 *          private var $cancel: Boolean
 *
 *          override def isCancelled: Boolean = this.$cancel
 *          override def setCancelled(cancel: Boolean): Unit = this.$cancel = cancel;
 *      }
 *     </code>
 * </pre>
 *
 * @apiNote events that implement this interface are expected to either not override any methods at all,
 *      or implement both. If only one of the two methods is overridden, an {@link xyz.janboerman.scalaloader.event.transform.EventError} is thrown by the ScalaPluginClassLoader.
 * @apiNote JavaPlugins must not ever define events that implement this interface.
 *
 * @see org.bukkit.event.Cancellable
 * @see Event
 * @see EventBus
 */
@Replaced
public interface Cancellable {
    /**
     * Tests whether this event is cancelled.
     *
     * @return true if the event is cancelled, otherwise false
     * @implNote if you override this method, you must also override {@link #setCancelled(boolean)}
     */
    @Replaced
    public default boolean isCancelled() {
        throw new UnsupportedOperationException("This method should have been overridden by the event class: " + getClass().getName());
    }

    /**
     * Set the cancel status of this event.
     *
     * @param cancel true if the event should be cancelled, false if the event is allowed to happen
     * @implNote if you override this method, you must also override {@link #isCancelled()}
     */
    @Replaced
    public default void setCancelled(boolean cancel) {
        throw new UnsupportedOperationException("This method should have been overridden by the event class: " + getClass().getName());
    }
}
