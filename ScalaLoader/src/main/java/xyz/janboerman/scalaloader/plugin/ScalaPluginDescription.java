package xyz.janboerman.scalaloader.plugin;

import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoadOrder;
import org.yaml.snakeyaml.Yaml;
import xyz.janboerman.scalaloader.compat.Compat;

import java.io.*;
import java.util.*;

/**
 * The Java API for configuring a {@link ScalaPlugin}. This API is meant as a compile-time type-safe alternative to the error-prone plugin.yml files.
 * An instance of this class is used by {@link ScalaPlugin#ScalaPlugin(ScalaPluginDescription)}.
 */
public final class ScalaPluginDescription {

    private static final PermissionDefault PERMISSION_DEFAULT = org.bukkit.permissions.Permission.DEFAULT_PERMISSION;

    private final String pluginName;
    private final String pluginVersion;

    private String apiVersion;
    private String main;
    private Map<String, Object> addYaml;

    private String pluginDescription;
    private List<String> authors = new LinkedList<>();
    private String prefix;
    private String website;
    private PluginLoadOrder loadOrder;
    private final LinkedHashSet<String> hardDependencies = new LinkedHashSet<>();
    private final LinkedHashSet<String> softDependencies = new LinkedHashSet<>(); { addSoftDepend("ScalaLoader"); }
    private final LinkedHashSet<String> inverseDependencies = new LinkedHashSet<>();
    private final LinkedHashSet<String> provides = new LinkedHashSet<>();
    private PermissionDefault permissionDefault = PERMISSION_DEFAULT;

    private final LinkedHashSet<Command> commands = new LinkedHashSet<>();
    private final LinkedHashSet<Permission> permissions = new LinkedHashSet<>();

    //TODO awareness?? use a List<PluginAwareness> ??
    //TODO idea: use awareness for Scala version!! That would only work if the Yaml instance from PluginDescriptionFile was accessible.
    //TODO see: https://hub.spigotmc.org/jira/browse/SPIGOT-6410
    
    public ScalaPluginDescription(String pluginName, String pluginVersion) {
        this.pluginName = Objects.requireNonNull(pluginName, "Plugin name cannot be null!");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "Plugin scalaVersion cannot be null!");
    }


    protected void setApiVersion(String bukkitApiVersion) {
        this.apiVersion = bukkitApiVersion;
    }

    protected void setMain(String mainClass) {
        this.main = mainClass;
    }

    protected void addYaml(Map<String, Object> yaml) {
        this.addYaml = yaml;
    }

    public String getName() {
        return pluginName;
    }

    public String getVersion() {
        return pluginVersion;
    }

    public String getFullName() {
        return getName() + " v" + getVersion();
    }

    public ScalaPluginDescription description(String pluginDescription) {
        this.pluginDescription = pluginDescription;
        return this;
    }

    public String getDescription() {
        return pluginDescription;
    }

    public ScalaPluginDescription authors(String... authors) {
        this.authors = Compat.listOf(authors);
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

    public ScalaPluginDescription prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public String getPrefix() {
        return prefix == null ? getName() : prefix;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public ScalaPluginDescription hardDepend(String... dependencies) {
        this.hardDependencies.clear();
        Collections.addAll(this.hardDependencies, dependencies);
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
        this.softDependencies.clear();
        this.softDependencies.add("ScalaLoader");
        Collections.addAll(this.softDependencies, dependencies);
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
        this.inverseDependencies.clear();
        Collections.addAll(this.inverseDependencies, inverseDependencies);
        return this;
    }

    public ScalaPluginDescription addLoadBefore(String inverseDependency) {
        this.inverseDependencies.add(inverseDependency);
        return this;
    }

    public Set<String> getInverseDependencies() {
        return Collections.unmodifiableSet(inverseDependencies);
    }

    public ScalaPluginDescription provides(String... pluginApis) {
        this.provides.clear();
        Collections.addAll(this.provides, pluginApis);
        return this;
    }

    public ScalaPluginDescription addProvides(String pluginApi) {
        this.provides.add(pluginApi);
        return this;
    }

    public Set<String> getProvides() {
        return Collections.unmodifiableSet(provides);
    }

    public ScalaPluginDescription permissionDefault(PermissionDefault permissionDefault) {
        this.permissionDefault = permissionDefault;
        return this;
    }

    public PermissionDefault getPermissionDefault() {
        return permissionDefault;
    }

    public ScalaPluginDescription commands(Command... commands) {
        this.commands.clear();
        Collections.addAll(this.commands, commands);
        return this;
    }

    public ScalaPluginDescription addCommand(Command command) {
        this.commands.add(command);
        return this;
    }

    public Collection<Command> getCommands() {
        return Collections.unmodifiableSet(commands);
    }

    public ScalaPluginDescription permissions(Permission... permissions) {
        this.permissions.clear();
        Collections.addAll(this.permissions, permissions);
        return this;
    }

    public ScalaPluginDescription addPermission(Permission permission) {
        this.permissions.add(permission);
        return this;
    }

    public Collection<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    public PluginDescriptionFile toPluginDescriptionFile() {
        Map<String, Object> pluginData = new HashMap<>();

        if (addYaml != null) pluginData.putAll(addYaml);

        pluginData.put("name", pluginName);
        pluginData.put("version", pluginVersion);
        pluginData.put("main", main);

        if (pluginDescription != null) pluginData.put("description", pluginDescription);
        if (authors != null && !authors.isEmpty()) pluginData.put("authors", authors);
        if (website != null) pluginData.put("website", getWebsite());
        if (prefix != null) pluginData.put("prefix", prefix);
        if (apiVersion != null) pluginData.put("api-version", apiVersion);
        if (loadOrder != null) pluginData.put("load", loadOrder.name());
        if (permissionDefault != null) pluginData.put("default-permission", permissionDefault.name());
        if (!hardDependencies.isEmpty()) pluginData.put("depend", Compat.listCopy(hardDependencies));
        if (!softDependencies.isEmpty()) pluginData.put("softdepend", Compat.listCopy(softDependencies));
        if (!inverseDependencies.isEmpty()) pluginData.put("loadbefore", Compat.listCopy(inverseDependencies));
        if (!provides.isEmpty()) pluginData.put("provides", Compat.listCopy(provides));
        if (!commands.isEmpty()) {
            Map<String, Map<String, Object>> commandsMap = new HashMap<>();
            for (Command command : getCommands()) {
                Map<String, Object> currentCommand = new HashMap<>();

                command.getDescription().ifPresent(description -> currentCommand.put("description", description));
                command.getUsage().ifPresent(usage -> currentCommand.put("usage", usage));
                command.getPermission().ifPresent(permission -> currentCommand.put("permission", permission));
                command.getPermissionMessage().ifPresent(permissionMessage -> currentCommand.put("permission-message", permissionMessage));
                Collection<String> aliases = command.getAliases();
                if (!aliases.isEmpty()) currentCommand.put("aliases", Compat.listCopy(aliases));

                commandsMap.put(command.getName(), currentCommand);
            }
            pluginData.put("commands", commandsMap);
        }
        if (!permissions.isEmpty()) {
            Map<String, Map<String, Object>> permissionsMap = new HashMap<>();
            for (Permission permission : getPermissions()) {
                Map<String, Object> currentPermission = createPermissionMap(permission, getPermissionDefault());
                permissionsMap.put(permission.getName(), currentPermission);
            }
            pluginData.put("permissions", permissionsMap);
        }

        Yaml yaml = new Yaml();
        String pluginYaml = yaml.dump(pluginData); //this can be quite a large string though. but whatever - Compact Strings to the rescue!

        try {
            return new PluginDescriptionFile(new StringReader(pluginYaml));
        } catch (InvalidDescriptionException impossibru) {
            impossibru.printStackTrace();
            return null;
        }
    }

    //recursively traverses the permissions to create a permission map. The map includes the permission properties (not the permission name)
    private static Map<String, Object> createPermissionMap(Permission permission, PermissionDefault parentDefault) {
        Map<String, Object> currentPermission = new HashMap<>();

        permission.getDescription().ifPresent(description -> currentPermission.put("description", description));
        PermissionDefault permissionDefault = permission.getDefault().orElse(parentDefault);
        currentPermission.put("default", permissionDefault.name());
        Collection<Permission> children = permission.getChildren();
        if (!children.isEmpty()) {
            Map<String, Object> childrenMap = new HashMap<>();
            for (Permission child : children) {
                childrenMap.put(child.getName(), createPermissionMap(child, permissionDefault));
            }
            currentPermission.put("children", childrenMap);
        }

        return currentPermission;
    }


    public static class Command {

        private final String name;
        private String description;
        private String usage;
        private LinkedHashSet<String> aliases;
        private String permission;
        private String permissionMessage;

        public Command(String name) {
            this.name = Objects.requireNonNull(name, "Command name cannot be null");
        }

        public Command description(String description) {
            this.description = description;
            return this;
        }

        public Command usage(String usage) {
            this.usage = usage;
            return this;
        }

        public Command aliases(String... aliases) {
            this.aliases = new LinkedHashSet<>(Arrays.asList(aliases));
            return this;
        }

        public Command addAlias(String alias) {
            if (alias == null) aliases = new LinkedHashSet<>();
            this.aliases.add(alias);
            return this;
        }

        public Command permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Command permissionMessage(String permissionMessage) {
            this.permissionMessage = permissionMessage;
            return this;
        }

        public String getName() {
            return name;
        }

        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        public Optional<String> getUsage() {
            return Optional.ofNullable(usage);
        }

        public Collection<String> getAliases() {
            return aliases == null ? Compat.emptyList() : Collections.unmodifiableSet(aliases);
        }

        public Optional<String> getPermission() {
            return Optional.ofNullable(permission);
        }

        public Optional<String> getPermissionMessage() {
            return Optional.ofNullable(permissionMessage);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Command)) return false;
            Command that = (Command) other;
            return Objects.equals(this.getName(), that.getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }

    public static class Permission {

        private final String name;
        private String description;
        private PermissionDefault permissionDefault;
        private LinkedHashSet<Permission> children;

        public Permission(String name) {
            this.name = Objects.requireNonNull(name, "Permission name cannot be null");
        }

        public Permission description(String description) {
            this.description = description;
            return this;
        }

        public Permission permissionDefault(PermissionDefault permissionDefault) {
            this.permissionDefault = permissionDefault;
            return this;
        }

        public Permission children(Permission... children) {
            this.children = new LinkedHashSet<>(Arrays.asList(children));
            return this;
        }

        public Permission addChild(Permission child) {
            if (children == null) children = new LinkedHashSet<>();
            children.add(child);
            return this;
        }

        public String getName() {
            return name;
        }

        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        public Optional<PermissionDefault> getDefault() {
            return Optional.ofNullable(permissionDefault);
        }

        public Collection<Permission> getChildren() {
            return children == null ? Compat.emptySet() : Collections.unmodifiableSet(children);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Permission)) return false;
            Permission that = (Permission) other;
            return Objects.equals(this.getName(), that.getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

    }

}
