package xyz.janboerman.scalaloader.plugin.paper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.commands.SetDebug;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.paper.ScalaLoader;

public class SetDebugCommand extends Command implements PluginIdentifiableCommand {

    public static final String name = "setDebug";
    private static final String usage = "/setDebug <class name>";
    private static final String description = """
            Adds a class name to the debug configuration, causing the classloader to dump the transformed class to the logs/console
            the next time the class is loaded.
            """;
    private static final String permission = "scalaloader.setdebug";

    private final ScalaLoader plugin;
    private final CommandExecutor executor;

    public SetDebugCommand(ScalaLoader scalaLoader) {
        super(name, description, usage, Compat.emptyList());
        setPermission(permission);
        this.plugin = scalaLoader;
        this.executor = new SetDebug(scalaLoader.getDebugSettings());
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
