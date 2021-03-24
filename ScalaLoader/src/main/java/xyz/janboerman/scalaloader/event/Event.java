package xyz.janboerman.scalaloader.event;

import org.bukkit.Server;
import xyz.janboerman.scalaloader.bytecode.Replaced;
import xyz.janboerman.scalaloader.event.transform.EventError;

/**
 * <p>
 *  An alternative to {@link org.bukkit.event.Event} for ScalaPlugins.
 *
 *  This class aims to reduce boilerplate for ScalaPlugin events, allowing you to omit the {@code public HandlerList getHandlers()}
 *  and {@code public static HanderList getHandlerList()} from your event definition.
 *
 *  At runtime subclasses of this event will be transformed by the class classloader to extend {@link org.bukkit.event.Event} instead
 *  and the HanderList-related methods will be generated. If a static HandlerList field is absent, then that will be generated too.
 * </p>
 *
 * <p>
 *  Listening to events can still be done using Bukkit's EventHandler/Listener api, for example:
 * </p>
 * <pre><code>
 *  import org.bukkit.event.EventPriority
 *  import xyz.janboerman.scalaloader.event.{Event, Cancellable}
 *  import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
 *  import xyz.janboerman.scalaloader.plugin.description.{Scala, ScalaVersion, Api, ApiVersion}
 *
 *  case class HomeTeleportEvent(player: Player, home: Location) extends Event with Cancellable
 *
 *  object HomeTeleportListener extends Listener {
 *      {@literal @}EventHandler(priority = EventPriority.MONITOR)
 *      def onHomeTeleport(event: HomeTeleportEvent): Unit = {
 *          event.player.sendMessage("Welcome home!")
 *      }
 *  }
 *
 *  {@literal @}Scala(version = ScalaVersion.v1_13_1)
 *  {@literal @}Api(ApiVersion.V1_15)
 *  object MyPlugin extends ScalaPlugin(new ScalaPluginDescription("MyPlugin", "1.0")) {
 *      override def onEnable(): Unit = {
 *          getServer.getPluginManager.registerEvents(HomeTeleportListener, this)
 *      }
 *  }
 * </code></pre>
 * <p>
 *  Alternatively, it's also possible to register your event Listener using a lambda, there's a {@link EventExecutor} too for ScalaLoader Events.
 * </p>
 *
 * @implNote because transformations are done at the classloader-level, JavaPlugins cannot extend this class because they use a
 * different classloader implementation. JavaPlugins can listen to instances of subclasses of {@link Event}s just fine through Bukkit's
 * EventHandler reflection api. It is only the {@link Event} base type that is toxic, don't ever use it explicitly in a JavaPlugin.
 *
 * @see Cancellable
 * @see EventBus
 */
@Replaced
public abstract class Event {

    /**
     * Constructor that allows you to specify whether this event is called and executed asynchronously.
     * @implNote can only be used by ScalaPlugins!
     * @param asynchronous true if the event is asynchronous, false if the event is executed in the server thread.
     */
    @Replaced
    public Event(boolean asynchronous) {
        throw new EventError("Cannot extend " + Event.class.getName() + " from within a JavaPlugin");
    }

    /**
     * Construct an event that is executed in the server thread.
     * @implNote can only be used by ScalaPlugins!
     */
    @Replaced
    public Event() {
        throw new EventError("Cannot extend " + Event.class.getName() + " from within a JavaPlugin");
    }

    /**
     * Tests whether the event is performed asynchronously - meaning it is executed outside of the server's primary thread.
     * @return true if the event is asynchronous, otherwise false
     *
     * @see Server#isPrimaryThread()
     * @see org.bukkit.scheduler.BukkitScheduler
     */
    @Replaced
    public boolean isAsynchronous() {
        throw new EventError("Somehow " + getClass().getName() + " was not transformed to a subclass of org.bukkit.event.Event. This is a bug in ScalaLoader!");
    }

    /**
     * Get the event's name. The default implementation returns the name of the class of the event.
     * @return the name of the event
     */
    @Replaced
    public String getEventName() {
        throw new EventError("Somehow " + getClass().getName() + " was not transformed to a subclass of org.bukkit.event.Event. This is a bug in ScalaLoader!");
    }

}
