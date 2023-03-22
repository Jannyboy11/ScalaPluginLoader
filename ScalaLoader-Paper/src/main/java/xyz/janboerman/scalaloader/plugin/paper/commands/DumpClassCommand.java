package xyz.janboerman.scalaloader.plugin.paper.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.janboerman.scalaloader.commands.DumpClass;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.paper.ScalaLoader;

import java.util.List;

public class DumpClassCommand extends Command implements PluginIdentifiableCommand {

    public static final String name = "dumpClass";
    private static final String usage = "/dumpClass <plugin> <class file> <format>?";
    private static final String description = """
            Dumps a class definition to the console/logs.
            The class can be printed in ASM format or in the regular text format for Java bytecode.
            """;
    private static final String permission = "scalaloader.dumpclass";

    private final ScalaLoader plugin;
    private final TabExecutor executor;

    public DumpClassCommand(ScalaLoader scalaLoader) {
        super(name, description, usage, Compat.emptyList());
        setPermission(permission);
        this.plugin = scalaLoader;
        this.executor = new DumpClass(scalaLoader);
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
