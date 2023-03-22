package xyz.janboerman.scalaloader.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import xyz.janboerman.scalaloader.DebugSettings;

public class SetDebug implements CommandExecutor {

    private final DebugSettings debugSettings;

    public SetDebug(DebugSettings debugSettings) {
        this.debugSettings = debugSettings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String className = args[0];
        boolean isDebugging = debugSettings.isDebuggingClassLoadOf(className);

        if (isDebugging) {
            debugSettings.undebugClass(className);
            sender.sendMessage(ChatColor.GREEN + "The class file will no longer be dumped to the console the next time the class is loaded.");
        } else {
            debugSettings.debugClass(className);
            sender.sendMessage(ChatColor.GREEN + "The class file will be dumped to the console the next time the class is loaded.");
        }

        return true;
    }

}
