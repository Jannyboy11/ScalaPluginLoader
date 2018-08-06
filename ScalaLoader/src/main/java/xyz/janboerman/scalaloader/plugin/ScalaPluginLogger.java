package xyz.janboerman.scalaloader.plugin;

import org.bukkit.plugin.PluginLogger;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
