package xyz.janboerman.scalaloader.event;

/**
 * Alternative to {@link org.bukkit.event.Cancellable} that aims to reduce boilerplate.
 * If your event implements this interface instead, the ScalaPluginClassLoader will inject the following three members:
 * <ul>
 *     <li>a field: <code>private boolean $cancel;</code></li>
 *     <li>a method: <code>public boolean isCancelled() { return this.$cancel; }</code></li>
 *     <li>a method: <code>public void setCancelled(boolean cancel) { this.$cancel = cancel; }</code></li>
 * </ul>
 *
 *
 * @apiNote events that implement this interface are expected to either not override any methods at all,
 *      or implement both. If only one of the two methods is overridden, an exception is thrown by the ScalaPluginClassLoader.
 *
 * @see org.bukkit.event.Cancellable
 */
public interface Cancellable {
    /**
     * Tests whether this event is cancelled.
     *
     * @return true if the event is cancelled, otherwise false
     * @implNote if you override this method, you must also override {@link #setCancelled(boolean)}
     */
    public default boolean isCancelled() {
        throw new UnsupportedOperationException("This method should have been overridden by the event class: " + getClass().getName());
    }

    /**
     * Set the cancel status of this event.
     *
     * @param cancel true if the event should be cancelled, false if the event is allowed to happen
     * @implNote if you override this method, you must also override {@link #isCancelled()}
     */
    public default void setCancelled(boolean cancel) {
        throw new UnsupportedOperationException("This method should have been overridden by the event class: " + getClass().getName());
    }
}
/*
Ideally, we would have used a Scala trait such as the following:

trait Cancellable { self: Event =>
    private cancel: Boolean

    def isCancelled: Boolean = cancel
    def setCancelled(cancel: Boolean): Unit = this.cancel = cancel;
}

But sadly we can't do this in Java. So the next best option is to inject this field and these methods into the class using a transforming classloader.
Which gets me thinking.. would it be possible to support Mixin?! :O (Yes i'm talking about SpongePowered's Mixin project by Mumfrey)
That should not be necessary once Scala 3 is released and I have a TASTy definition of this trait tho.
I could then include that in the jar of ScalaLoader so it's available for all dotty-compiled Scala plugins.
This could be done with ASM's ClassVisitor#visitAttribute
But I thought I read something about Scala 2.13 also supporting TASTy //TODO check this.
https://github.com/lampepfl/dotty/blob/master/library/src/scala/tasty/Reflection.scala
 */