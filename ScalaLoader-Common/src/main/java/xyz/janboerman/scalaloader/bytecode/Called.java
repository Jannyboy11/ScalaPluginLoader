package xyz.janboerman.scalaloader.bytecode;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import xyz.janboerman.scalaloader.compat.Migration;
import xyz.janboerman.scalaloader.configurationserializable.runtime.*;
import xyz.janboerman.scalaloader.event.EventBus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * Indicates that methods annotated with this annotation can be called by ScalaPlugins as a result of bytecode transformation.
 * Indicates that interfaces annotated with this annotation are implemented by classes which are generated at runtime.
 * 
 * @see RuntimeConversions#serialize(Object, ParameterType, ClassLoader)
 * @see RuntimeConversions#deserialize(Object, ParameterType, ClassLoader)
 * @see EventBus#callEvent(Event)
 * @see EventBus#registerEvent(Class, Listener, EventPriority, EventExecutor, Plugin)
 * @see EventBus#registerEvent(Class, Listener, EventPriority, EventExecutor, Plugin, boolean)
 * @see ParameterType#from(Type)
 * @see ArrayParameterType#from(ParameterType, boolean)
 * @see ParameterizedParameterType#from(Class, ParameterType...)
 * @see Adapter
 * @see Migration
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Called {
}
