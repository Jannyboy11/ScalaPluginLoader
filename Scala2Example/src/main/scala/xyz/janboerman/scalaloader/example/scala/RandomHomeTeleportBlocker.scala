package xyz.janboerman.scalaloader.example.scala

import org.bukkit.event.{EventHandler, Listener}

object RandomHomeTeleportBlocker extends Listener {
    @EventHandler
    def onTeleportHome(event: HomeTeleportEvent): Unit = {
        event.setCancelled(Math.random() < 0.5);
    }
}
