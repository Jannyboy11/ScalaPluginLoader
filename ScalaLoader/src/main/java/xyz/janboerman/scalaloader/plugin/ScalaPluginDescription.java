package xyz.janboerman.scalaloader.plugin;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginLoadOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ScalaPluginDescription {

    private final String scalaVersion;
    private String pluginName;
    private String pluginVersion;
    private PluginLoadOrder loadOrder;
    private List<String> hardDependencies = new ArrayList<>();
    private List<String> softDependencies = new ArrayList<>();
    private List<String> loadBefores = new ArrayList<>();
    private PermissionDefault permissionDefault = Permission.DEFAULT_PERMISSION;

    //TODO commands, permissions

    public ScalaPluginDescription(String scalaVersion) {
        this.scalaVersion = scalaVersion;
    }

    public ScalaPluginDescription(String scalaVersion, String pluginName, String pluginVersion) {
        this.scalaVersion = Objects.requireNonNull(scalaVersion, "Scala scalaVersion cannot be null!");
        this.pluginName = Objects.requireNonNull(pluginName, "Plugin name cannot be null!");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "Plugin scalaVersion cannot be null!");
    }

    public String getScalaVersion() {
        return scalaVersion;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }


}
