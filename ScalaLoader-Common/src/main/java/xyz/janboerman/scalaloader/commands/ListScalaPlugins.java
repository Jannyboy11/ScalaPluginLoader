package xyz.janboerman.scalaloader.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

public class ListScalaPlugins implements TabExecutor {

    private IScalaLoader scalaLoader;

    public ListScalaPlugins(IScalaLoader scalaLoader) {
        this.scalaLoader = scalaLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Collection<? extends IScalaPlugin> scalaPlugins = scalaLoader.getScalaPlugins();
        if (scalaPlugins.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No ScalaPlugins loaded.");
            return true;
        }

        Map<ScalaRelease, List<IScalaPlugin>> pluginNamesByScalaRelease = new TreeMap<>(Comparator.reverseOrder());
        for (IScalaPlugin scalaPlugin : scalaPlugins) {
            pluginNamesByScalaRelease.computeIfAbsent(scalaPlugin.getScalaRelease(), k -> new ArrayList<>(2)).add(scalaPlugin);
        }

        sender.sendMessage(ChatColor.GREEN + "=== ScalaPlugins by major Scala version ===");
        for (Map.Entry<ScalaRelease, List<IScalaPlugin>> entry : pluginNamesByScalaRelease.entrySet()) {
            ScalaRelease scalaRelease = entry.getKey();
            List<IScalaPlugin> plugins = entry.getValue();

            StringJoiner pluginsPart = new StringJoiner(", ");
            for (IScalaPlugin scalaPlugin : plugins) {
                pluginsPart.add((scalaPlugin.isEnabled() ? ChatColor.AQUA : ChatColor.RED) + scalaPlugin.getName()
                        + ChatColor.AQUA + " (" + ChatColor.DARK_AQUA + scalaPlugin.getDeclaredScalaVersion() + ChatColor.AQUA + ")");
            }

            sender.sendMessage(ChatColor.AQUA + "[" + ChatColor.DARK_AQUA + scalaRelease.getCompatVersion() + ".x" + ChatColor.AQUA + "] " + pluginsPart);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Compat.emptyList();
    }
}
