package xyz.janboerman.scalaloader.plugin;

import org.bukkit.Server;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * The ScalaPluginLogger - an alternative to {@link org.bukkit.plugin.PluginLogger}
 * that doesn't need a ScalaPlugin's {@link org.bukkit.plugin.PluginDescriptionFile} to work.
 */
class ScalaPluginLogger extends Logger {

    private final String logPrefix;

    ScalaPluginLogger(ScalaPlugin scalaPlugin) {
        super(scalaPlugin.getClass().getCanonicalName(), null);
        this.logPrefix = "[" + scalaPlugin.getScalaDescription().getPrefix() + "] ";
        Server server = scalaPlugin.getServer();
        if (server != null) {   //IntelliJ's got it wrong here because ScalaPlugin#getServer() can in fact return null in rare circumstances!
            setParent(server.getLogger());
        }
        setLevel(Level.ALL);
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(logPrefix + logRecord.getMessage());
        super.log(logRecord);
    }
}
