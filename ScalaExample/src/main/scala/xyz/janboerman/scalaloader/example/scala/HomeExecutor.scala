package xyz.janboerman.scalaloader.example.scala

import org.bukkit.command.{Command, CommandExecutor, CommandSender, TabExecutor}
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

//simple home command that manages one home per player.
object HomeExecutor extends TabExecutor {

    override def onCommand(sender: CommandSender, command: Command, label: String, args: Array[String]): Boolean = {
        if (!sender.isInstanceOf[Player]) {
            sender.sendMessage("You can only use this command as a player.")
            return true
        }

        val player = sender.asInstanceOf[Player];
        if (args.length == 0) return false;

        val playerId = player.getUniqueId
        val action = args(0)
        action match {
            case "set" =>
                val home = Home(playerId, "home", player.getLocation)
                HomeManager.addHome(playerId, home)
                player.sendMessage("Home set!")
                HomeManager.saveHome(playerId, home)
            case "tp" => HomeManager.getHome(playerId) match {
                case Some(home) =>
                    if (ExamplePlugin.getEventBus.callEvent(HomeTeleportEvent(player, home))) {
                        player.teleport(home.getLocation())
                        player.sendMessage("Welcome home!");
                    } else {
                        player.sendMessage("Some plugin prevented you from teleporting to your home!")
                    }
                case None => player.sendMessage("You don't have a home yet.")
            }
            case _ => return false
        }

        true
    }

    override def onTabComplete(sender: CommandSender, command: Command, label: String, args: Array[String]): java.util.List[String] = {
        if (args.isEmpty) return java.util.List.of("set", "tp")
        else if (args.length == 1) {
            val firstArg = args(0)
            if (StringUtil.startsWithIgnoreCase("set", firstArg)) return java.util.List.of[String]("set")
            else if (StringUtil.startsWithIgnoreCase("tp", firstArg)) return java.util.List.of[String]("tp")
        }

        java.util.List.of()
    }
}
