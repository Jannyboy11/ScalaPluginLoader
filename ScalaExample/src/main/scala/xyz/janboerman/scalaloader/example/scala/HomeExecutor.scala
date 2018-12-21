package xyz.janboerman.scalaloader.example.scala

import java.util.UUID

import org.bukkit.command.{Command, CommandExecutor, CommandSender}
import org.bukkit.entity.Player

import scala.collection.mutable

//simple home command that manages one home per player.
object HomeExecutor extends CommandExecutor {

    //normally you wouldn't store the homes in the command class, and you would implement saving/loading from a database/config
    private val homes = new mutable.HashMap[UUID, Home]()

    override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
        if (!sender.isInstanceOf[Player]) {
            sender.sendMessage("You can only use this command as a player.")
            return true
        }

        val player = sender.asInstanceOf[Player];
        if (args.length == 0) return false;

        val action = args(0)
        action match {
            case "set" => homes.put(player.getUniqueId, Home(player.getUniqueId, "home", player.getLocation))
            case "tp" => homes.get(player.getUniqueId) match {
                case Some(home) => player.teleport(home.getLocation())
                case None => player.sendMessage("You don't have a home yet.")
            }
            case _ => return false
        }

        true
    }
}
