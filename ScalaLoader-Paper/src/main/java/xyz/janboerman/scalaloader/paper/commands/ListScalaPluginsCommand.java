package xyz.janboerman.scalaloader.paper.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.janboerman.scalaloader.commands.ListScalaPlugins;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.paper.ScalaLoader;

import java.util.List;

public class ListScalaPluginsCommand extends Command implements PluginIdentifiableCommand {

    public static final String name = "listScalaPlugins";
    private static final String usage = "/listScalaPlugins";
    private static final String description = "Output the scala plugins, grouped by their version of Scala";
    private static final String permission = "scalaloader.listscalaplugins";
    private static final List<String> aliases = Compat.listOf("scalaplugins");

    private final ScalaLoader plugin;
    private final TabExecutor executor;

    public ListScalaPluginsCommand(ScalaLoader scalaLoader) {
        super(name, description, usage, aliases);
        setPermission(permission);
        this.plugin = scalaLoader;
        this.executor = new ListScalaPlugins(scalaLoader);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        return executor.onCommand(sender, this, label, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args, @Nullable Location location) throws IllegalArgumentException {
        return executor.onTabComplete(sender, this, alias, args);
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin;
    }

}
