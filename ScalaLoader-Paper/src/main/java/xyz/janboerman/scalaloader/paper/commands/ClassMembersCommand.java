package xyz.janboerman.scalaloader.paper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.commands.ClassMembers;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.paper.ScalaLoader;

import java.util.List;

public class ClassMembersCommand extends Command implements PluginIdentifiableCommand {

    public static final String name = "classMembers";
    private static final String usage = "/classMembers";
    private static final String description = "Lists accessible fields and methods of classes.";
    private static final String permission = "scalaloader.classmembers";
    private static final List<String> aliases = Compat.emptyList();

    private final ScalaLoader plugin;
    private final CommandExecutor executor;

    public ClassMembersCommand(ScalaLoader scalaLoader) {
        super(name, description, usage, aliases);
        setPermission(permission);
        this.plugin = scalaLoader;
        this.executor = new ClassMembers();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        return executor.onCommand(sender, this, label, args);
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin;
    }
}
