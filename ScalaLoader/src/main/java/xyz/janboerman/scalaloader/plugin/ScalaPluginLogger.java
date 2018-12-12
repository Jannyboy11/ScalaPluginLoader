package xyz.janboerman.scalaloader.plugin;

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
        setParent(scalaPlugin.getServer().getLogger());
        setLevel(Level.ALL);
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(logPrefix + logRecord.getMessage());
        super.log(logRecord);
    }
}
