package xyz.janboerman.scalaloader.event;

import org.bukkit.event.Listener;
import xyz.janboerman.scalaloader.bytecode.Replaced;

/**
 * An event executor. Can be implemented using a lambda expression in your ScalaPlugin.
 *
 * @implNote <p>    This interface CANNOT be implemented by class that are not loaded through the {@link xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader},
 *                  which basically means you can't use this from a JavaPlugin. Use Bukkit's Listener/EventHandler reflection api instead!
 *           </p>
 * <pre>
 *     <code>
 *         public class MyListener implements org.bukkit.event.Listener {
 *            {@literal @} org.bukkit.event.EventHandler
 *            public void onEvent(SomeScalaPluginEvent event) {
 *                //use the event
 *            }
 *         }
 *     </code>
 * </pre>
 *           <p>    Alternatively you can use Bukkit's EventExecutor api like so:
 *           </p>
 * <pre>
 *     <code>
 *         org.bukkit.plugin.EventExecutor = (listener, bukkitEvent) -{@literal >} {
 *             SomeScalaPluginEvent event = (SomeScalaPluginEvent) (Object) bukkitEvent;
 *             //use the event
 *         }
 *     </code>
 * </pre>
 *           <p>    This will work because at runtime all events defined by ScalaPlugins will extend org.bukkit.event.Event instead of xyz.janboerman.scalaloader.event.Event.
 *                  DO NOT EVER CAST TO xyz.janboerman.scalaloader.event.Event, use java.lang.Object instead!
 *           </p>
 *
 * @param <E> the event type
 * @param <L> the listener type
 *
 * @see EventBus
 */
@FunctionalInterface
@Replaced
public interface EventExecutor<L extends Listener, E extends Event> {

    /**
     * The callback method that is executed when the event is called.
     * @param listener the listener
     * @param event the event
     */
    @Replaced
    public void execute(L listener, E event);

}
