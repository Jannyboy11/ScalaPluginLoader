package xyz.janboerman.scalaloader.example.scala

//import java.util
//import org.bukkit.configuration.serialization.{ConfigurationSerializable, SerializableAs}
import java.util.UUID

import org.bukkit.Location
import xyz.janboerman.scalaloader.configurationserializable.ConfigurationSerializable

object Home {
    def apply(owner: UUID, name: String, location: Location): Home = new Home(owner, name, location)
    def unapply(arg: Home): Option[(UUID, String, Location)] = Some(arg.getOwner(), arg.getName(), arg.getLocation())

//    def valueOf(map: util.Map[String, AnyRef]): Home = {
//        val owner = UUID.fromString(map.get("owner").asInstanceOf[String])
//        val name = map.get("name").asInstanceOf[String]
//        val location = map.get("location").asInstanceOf[Location]
//        Home(owner, name, location)
//    }
}

//@SerializableAs("Home")
@ConfigurationSerializable(as = "Home")
class Home private(owner: UUID, name: String, location: Location) /*extends ConfigurationSerializable*/ {
    def getLocation(): Location = location.clone()
    def getOwner(): UUID = owner
    def getName(): String = name

//    override def serialize(): util.Map[String, AnyRef] =
//        util.Map.of("owner", getOwner().toString, "name", getName(), "location", getLocation())
}