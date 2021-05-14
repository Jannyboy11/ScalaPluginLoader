package xyz.janboerman.scalaloader.bytecode;

import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import xyz.janboerman.scalaloader.event.EventExecutor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the class annotated with this annotation will not be used by ScalaPlugins as a result of bytecode transformation.
 * Calls to methods annotated with this method will be replaced by calls to different methods.
 * For classes, a different class will be used.
 *
 * @see xyz.janboerman.scalaloader.event.Event
 * @see xyz.janboerman.scalaloader.event.Cancellable
 * @see xyz.janboerman.scalaloader.event.EventExecutor
 * @see xyz.janboerman.scalaloader.event.EventBus#callEvent(Object)
 * @see xyz.janboerman.scalaloader.event.EventBus#registerEvent(Class, Listener, EventPriority, EventExecutor, Plugin)
 * @see xyz.janboerman.scalaloader.event.EventBus#registerEvent(Class, Listener, EventPriority, EventExecutor, Plugin, boolean)
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Replaced {
}
