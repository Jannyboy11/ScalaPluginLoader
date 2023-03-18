package xyz.janboerman.scalaloader.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ResetScalaUrls implements TabExecutor {

    private final IScalaLoader scalaLoader;

    public ResetScalaUrls(IScalaLoader scalaLoader) {
        this.scalaLoader = scalaLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        String version = args[0];
        ScalaVersion scalaVersion;
        if ("all".equals(version)) {
            scalaLoader.saveScalaVersionsToConfig(Arrays.stream(ScalaVersion.values())
                    .map(PluginScalaVersion::fromScalaVersion)
                    .toArray(PluginScalaVersion[]::new));
            sender.sendMessage(ChatColor.GREEN + "All URLs for built-in scala versions were reset!");
        } else if ((scalaVersion = ScalaVersion.fromVersionString(version)) != null) {
            scalaLoader.saveScalaVersionsToConfig(PluginScalaVersion.fromScalaVersion(scalaVersion));
            sender.sendMessage(ChatColor.GREEN + "URLs for scala version " + scalaVersion.getVersion() + " were reset.");
        } else {
            sender.sendMessage(new String[] {
                    ChatColor.RED + "Unrecognized scala version: " + version + ".",
                    ChatColor.RED + "Please use one of the following: " + onTabComplete(sender, command, label, new String[0]) + "."
            });
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            LinkedList<String> scalaVersions = Arrays.stream(ScalaVersion.values())
                    .map(ScalaVersion::getVersion)
                    .collect(Collectors.toCollection(LinkedList::new));

            scalaVersions.addFirst("all");
            return scalaVersions;
        }

        return Compat.emptyList();
    }

}
