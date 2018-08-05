package xyz.janboerman.scalaloader.plugin;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginLoadOrder;

import java.util.*;

public final class ScalaPluginDescription {

    private final String scalaVersion;
    private String pluginName;
    private String pluginVersion;
    private PluginLoadOrder loadOrder;
    private LinkedHashSet<String> hardDependencies = new LinkedHashSet<>();
    private LinkedHashSet<String> softDependencies = new LinkedHashSet<>();
    private LinkedHashSet<String> inverseDependencies = new LinkedHashSet<>();
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

    public ScalaPluginDescription pluginName(String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public ScalaPluginDescription pluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
        return this;
    }

    public ScalaPluginDescription pluginLoadOrder(PluginLoadOrder loadOrder) {
        this.loadOrder = loadOrder;
        return this;
    }

    public PluginLoadOrder getLoadOrder() {
        return loadOrder;
    }

    public ScalaPluginDescription hardDepend(String... dependencies) {
        this.hardDependencies = new LinkedHashSet<>(Arrays.asList(dependencies));
        return this;
    }

    public ScalaPluginDescription addHardDepend(String dependency) {
        this.hardDependencies.add(dependency);
        return this;
    }

    public Set<String> getHardDependencies() {
        return Collections.unmodifiableSet(hardDependencies);
    }

    public ScalaPluginDescription softDepend(String... dependencies) {
        this.softDependencies = new LinkedHashSet<>(Arrays.asList(dependencies));
        return this;
    }

    public ScalaPluginDescription addSoftDepend(String dependency) {
        this.softDependencies.add(dependency);
        return this;
    }

    public Set<String> getSoftDependencies() {
        return Collections.unmodifiableSet(softDependencies);
    }

    public ScalaPluginDescription loadBefore(String... inverseDependencies) {
        this.inverseDependencies = new LinkedHashSet<>(Arrays.asList(inverseDependencies));
        return this;
    }

    public ScalaPluginDescription addLoadBefore(String inverseDependency) {
        this.inverseDependencies.add(inverseDependency);
        return this;
    }

    public Set<String> getInverseDependences() {
        return Collections.unmodifiableSet(inverseDependencies);
    }

    public ScalaPluginDescription permissionDefault(PermissionDefault permissionDefault) {
        this.permissionDefault = permissionDefault;
        return this;
    }

    public PermissionDefault getPermissionDefault() {
        return permissionDefault;
    }

}
