package xyz.janboerman.scalaloader.bytecode;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import xyz.janboerman.scalaloader.configurationserializable.runtime.ParameterType;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that methods annotated with this annotation can be called by ScalaPlugins as a result of bytecode transformation.
 * 
 * @see xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions#serialize(Object, ParameterType, ScalaPluginClassLoader)
 * @see xyz.janboerman.scalaloader.configurationserializable.runtime.RuntimeConversions#deserialize(Object, ParameterType, ScalaPluginClassLoader)
 * @see xyz.janboerman.scalaloader.event.EventBus#callEvent(Event)
 * @see xyz.janboerman.scalaloader.event.EventBus#registerEvent(Class, Listener, EventPriority, EventExecutor, Plugin);
 * @see xyz.janboerman.scalaloader.event.EventBus#registerEvent(Class, Listener, EventPriority, EventExecutor, Plugin, boolean)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Called {
}
