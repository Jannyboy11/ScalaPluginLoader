package xyz.janboerman.scalaloader.plugin;

import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;

public final class ScalaPluginDescription {

    private final String pluginName;
    private final String pluginVersion;

    private String pluginDescription;
    private List<String> authors = new LinkedList<>();
    private String website;
    private String apiVersion;
    private PluginLoadOrder loadOrder;
    private LinkedHashSet<String> hardDependencies = new LinkedHashSet<>();
    private LinkedHashSet<String> softDependencies = new LinkedHashSet<>();
    private LinkedHashSet<String> inverseDependencies = new LinkedHashSet<>();
    private PermissionDefault permissionDefault = Permission.DEFAULT_PERMISSION;

    //TODO commands, permissions

    //TODO awareness??
    
    public ScalaPluginDescription(String pluginName, String pluginVersion) {
        this.pluginName = Objects.requireNonNull(pluginName, "Plugin name cannot be null!");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "Plugin scalaVersion cannot be null!");
    }

    public String getName() {
        return pluginName;
    }

    public String getVersion() {
        return pluginVersion;
    }

    public ScalaPluginDescription description(String pluginDescription) {
        this.pluginDescription = pluginDescription;
        return this;
    }

    public String getDescription() {
        return pluginDescription;
    }

    public ScalaPluginDescription authors(String... authors) {
        this.authors = Arrays.asList(authors);
        return this;
    }

    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    public ScalaPluginDescription loadOrder(PluginLoadOrder loadOrder) {
        this.loadOrder = loadOrder;
        return this;
    }

    public PluginLoadOrder getLoadOrder() {
        return loadOrder;
    }

    public ScalaPluginDescription website(String website) {
        this.website = website;
        return this;
    }

    public String getWebsite() {
        return website;
    }

    void setApiVersion(String bukkitApiVersion) {
        this.apiVersion = bukkitApiVersion;
    }

    public String getApiVersion() {
        return apiVersion;
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

    public PluginDescriptionFile toPluginDescriptionFile() {
        Map<String, Object> pluginData = new HashMap<>();

        pluginData.put("name", pluginName);
        pluginData.put("version", pluginVersion);
        if (pluginDescription != null) pluginData.put("description", pluginDescription);
        if (authors != null && !authors.isEmpty()) pluginData.put("authors", authors);
        if (website != null) pluginData.put("website", getWebsite());
        if (apiVersion != null) pluginData.put("api-version", apiVersion);
        if (loadOrder != null) pluginData.put("load", loadOrder);
        if (hardDependencies != null && !hardDependencies.isEmpty()) pluginData.put("depend", new ArrayList<>(hardDependencies));
        if (softDependencies != null && !softDependencies.isEmpty()) pluginData.put("softdepend", new ArrayList<>(softDependencies));
        if (inverseDependencies != null && !inverseDependencies.isEmpty()) pluginData.put("loadbefore", new ArrayList<>(inverseDependencies));
        if (permissionDefault != null) pluginData.put("default-permission", permissionDefault);

        //TODO commands, permissions

        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        Yaml yaml = new Yaml();
        yaml.dump(pluginData, new OutputStreamWriter(pipedOutputStream));
        try {
            PipedInputStream yamlImputStream = new PipedInputStream(pipedOutputStream);
            return new PluginDescriptionFile(yamlImputStream);
        } catch (IOException | InvalidDescriptionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
