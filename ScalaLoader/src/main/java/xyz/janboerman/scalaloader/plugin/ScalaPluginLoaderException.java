package xyz.janboerman.scalaloader.plugin;

import org.bukkit.plugin.InvalidPluginException;

public class ScalaPluginLoaderException extends InvalidPluginException {

    public ScalaPluginLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScalaPluginLoaderException(String message) {
        super(message);
    }
}
