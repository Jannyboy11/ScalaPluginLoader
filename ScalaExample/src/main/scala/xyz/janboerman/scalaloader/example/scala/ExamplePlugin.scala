package xyz.janboerman.scalaloader.example.scala

import org.bukkit.ChatColor
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.event.player.PlayerJoinEvent
import xyz.janboerman.scalaloader.plugin.{ScalaPlugin, ScalaPluginDescription}
import xyz.janboerman.scalaloader.plugin.description.{Scala, ScalaVersion}

@Scala(version = ScalaVersion.v2_12_6)
object ExamplePlugin extends ScalaPlugin(new ScalaPluginDescription(
        "ScalaExample",
        "0.1-SNAPSHOT")) with Listener {

    getLogger().info("ScalaExample - I am constructed!")

    override def onLoad(): Unit = {
        getLogger.info("ScalaExample - I am loaded!")
    }

    override def onEnable(): Unit = {
        getLogger.info("ScalaExample - I am enabled!")
        getServer.getPluginManager.registerEvents(this, this)
    }

    override def onDisable(): Unit = {
        getLogger.info("ScalaExample - I am disabled!")
    }

    @EventHandler
    def onJoin(event: PlayerJoinEvent): Unit = {
        event.setJoinMessage(ChatColor.GREEN + "Howdy " + event.getPlayer.getName + "!");
    }

}

