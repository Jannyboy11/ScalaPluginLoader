package xyz.janboerman.scalaloader.example.scala

import org.bukkit.ChatColor
import org.bukkit.command.{CommandSender, Command}
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.permissions.PermissionDefault
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription.{Command => SPCommand, Permission => SPPermission}
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_12_7)
object ExamplePlugin
    extends ScalaPlugin(new ScalaPluginDescription("ScalaExample", "0.6-SNAPSHOT")
        .addCommand(new SPCommand("foo").permission("scalaexample.foo"))
        .addCommand(new SPCommand("home").permission("scalaexample.home").usage("/home set|tp"))
        .permissions(new SPPermission("scalaexample.foo").permissionDefault(PermissionDefault.TRUE)))
    with Listener {

    getLogger.info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")
        getServer.getPluginManager.registerEvents(this, this)
        getCommand("home").setExecutor(HomeExecutor)
    }

    override def onDisable(): Unit = {
        getLogger.info("ScalaExample - I am disabled!")
    }

    @EventHandler
    def onJoin(event: PlayerJoinEvent): Unit =
        event.setJoinMessage(ChatColor.GREEN + "Howdy " + event.getPlayer.getName + "!")

    override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
        sender.sendMessage("Executed foo command!")
        true
    }

    def getInt() = 42

}

