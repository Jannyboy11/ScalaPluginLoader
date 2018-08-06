package xyz.janboerman.scalaloader.plugin;

import java.util.logging.LogRecord;
import java.util.logging.Logger;

class ScalaPluginLogger extends Logger {

    private final String logPrefix;

    ScalaPluginLogger(ScalaPlugin scalaPlugin) {
        super(scalaPlugin.getClass().getCanonicalName(), null);
        this.logPrefix = "[" + scalaPlugin.getScalaDescription().getPrefix() + "] ";
    }

    @Override
    public void log(LogRecord logRecord) {
        logRecord.setMessage(logPrefix + logRecord.getMessage());
        super.log(logRecord);
    }
}
