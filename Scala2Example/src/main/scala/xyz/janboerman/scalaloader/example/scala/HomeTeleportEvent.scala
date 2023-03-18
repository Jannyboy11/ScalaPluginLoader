package xyz.janboerman.scalaloader.example.scala

import org.bukkit.entity.Player
import xyz.janboerman.scalaloader.event.{Cancellable, Event}

case class HomeTeleportEvent(player: Player, home: Home) extends Event with Cancellable