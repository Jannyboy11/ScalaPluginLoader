package xyz.janboerman.scalaloader.paper.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.janboerman.scalaloader.commands.ResetScalaUrls;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.paper.ScalaLoader;

import java.util.List;

public class ResetScalaUrlsCommand extends Command implements PluginIdentifiableCommand {

    public static final String name = "resetScalaUrls";
    private static final String usage = "/resetScalaUrls all|[<scala scalaVersion>]";
    private static final String description = """
            Resets the config URL of the built-in scala version.
                    Useful if you overwrote some of the urls.
                    If 'all' is provided as a scala version, this command resets the URLs of all the built-in scala versions.
                    """;
    private static final String permission = "scalaloader.resetscalaurls";
    //no alises

    private final ScalaLoader plugin;
    private final TabExecutor executor;

    public ResetScalaUrlsCommand(ScalaLoader scalaLoader) {
        super(name, description, usage, Compat.emptyList());
        setPermission(permission);
        this.plugin = scalaLoader;
        this.executor = new ResetScalaUrls(scalaLoader);

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
