package xyz.janboerman.scalaloader.example.scala

import java.util.UUID

import org.bukkit.Location

object Home {
    def apply(owner: UUID, name: String, location: Location): Home = new Home(owner, name, location.clone())
    def unapply(arg: Home): Option[(UUID, String, Location)] = Some(arg.owner, arg.name, arg.location.clone())
}

class Home private(private val owner: UUID, private val name: String, private val location: Location) {
    def getLocation(): Location = location.clone()
    def getOwner(): UUID = owner
    def getName(): String = name
}