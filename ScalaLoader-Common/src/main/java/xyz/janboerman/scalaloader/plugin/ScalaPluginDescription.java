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
public class ScalaPluginDescription {

    private static final PermissionDefault PERMISSION_DEFAULT = org.bukkit.permissions.Permission.DEFAULT_PERMISSION;

    private final String pluginName;
    private final String pluginVersion;

    private String apiVersion;
    private String main;
    private Map<String, Object> addYaml;
    private String scalaVersion;
    private boolean foliaSupported;

    private String pluginDescription;
    private List<String> authors = new LinkedList<>();
    private LinkedHashSet<String> contributors = new LinkedHashSet<>();
    private String prefix;
    private String website;
    private PluginLoadOrder loadOrder;
    private final LinkedHashSet<String> hardDependencies = new LinkedHashSet<>();
    private final LinkedHashSet<String> softDependencies = new LinkedHashSet<>(); { addSoftDepend("ScalaLoader"); }
    private final LinkedHashSet<String> inverseDependencies = new LinkedHashSet<>();
    private final LinkedHashSet<String> provides = new LinkedHashSet<>();
    private final LinkedHashSet<String> mavenDependencies = new LinkedHashSet<>();
    private PermissionDefault permissionDefault = PERMISSION_DEFAULT;
    private final LinkedHashSet<Command> commands = new LinkedHashSet<>();
    private final LinkedHashSet<Permission> permissions = new LinkedHashSet<>();

    //awareness?? use a List<PluginAwareness> ??
    //idea: use awareness for Scala version!! That would only work if the Yaml instance from PluginDescriptionFile was accessible.
    //see: https://hub.spigotmc.org/jira/browse/SPIGOT-6410
    
    public ScalaPluginDescription(String pluginName, String pluginVersion) {
        this.pluginName = Objects.requireNonNull(pluginName, "Plugin name cannot be null!");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "Plugin scalaVersion cannot be null!");
    }

    /** @deprecated internal use only */
    @Deprecated
    public void setApiVersion(String bukkitApiVersion) {
        if (bukkitApiVersion == null) return;
        this.apiVersion = bukkitApiVersion;
    }

    /** @deprecated internal use only */
    @Deprecated
    public void setMain(String mainClass) {
        if (mainClass == null) return;
        this.main = mainClass;
    }

    protected void addYaml(Map<String, Object> yaml) {
        this.addYaml = yaml;
    }

    /** @deprecated internal use only */
    @Deprecated
    public void setScalaVersion(String scalaVersion) {
        if (scalaVersion == null) return;
        this.scalaVersion = scalaVersion;
    }

    /** @deprecated internal use only */
    @Deprecated
    public String getScalaVersion() {
        return scalaVersion;
    }

    public ScalaPluginDescription foliaSupported() {
        return setFoliaSupported(true);
    }

    public ScalaPluginDescription setFoliaSupported(boolean supportFolia) {
        this.foliaSupported = foliaSupported;
        return this;
    }

    public boolean isFoliaSupported() {
        return foliaSupported;
    }

    /** @deprecated internal use only */
    @Deprecated
    public String getMain() {
        return main;
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
        this.authors = new ArrayList<>(Compat.listOf(authors));
        return this;
    }

    public ScalaPluginDescription addAuthor(String author) {
        this.authors.add(author);
        return this;
    }

    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    public ScalaPluginDescription contributors(String... contributors) {
        this.contributors = new LinkedHashSet<>(Compat.listOf(contributors));
        return this;
    }

    public ScalaPluginDescription addContributor(String contributor) {
        this.contributors.add(contributor);
        return this;
    }

    public Collection<String> getContributors() {
        return Collections.unmodifiableCollection(contributors);
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

    public ScalaPluginDescription moveHardDependencyToSoftDependency(String dependency) {
        if (hardDependencies.remove(dependency)) {
            addSoftDepend(dependency);
        }
        return this;
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

    /**
     * Sets the maven dependencies of the ScalaPlugin. This method only supports maven dependencies available on Maven Central.
     * @param mavenDependencies the dependencies in GAV format, e.g. "com.example.foo:foo:1.0"
     * @return this ScalaPluginDescription
     */
    public ScalaPluginDescription mavenDependencies(String... mavenDependencies) {
        this.mavenDependencies.clear();
        Collections.addAll(this.mavenDependencies, mavenDependencies);
        return this;
    }

    /**
     * Adds a maven dependency to the ScalaPlugin. This method only supports maven dependencies available on Maven Central.
     * @param mavenDependency the dependency in GAV format, e.g. "com.example.foo:foo:1.0"
     * @return this ScalaPluginDescription
     */
    public ScalaPluginDescription addMavenDependency(String mavenDependency) {
        this.mavenDependencies.add(mavenDependency);
        return this;
    }

    public Set<String> getMavenDependencies() {
        return Collections.unmodifiableSet(mavenDependencies);
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

        if (foliaSupported) pluginData.put("folia-supported", true);
        if (pluginDescription != null) pluginData.put("description", pluginDescription);
        if (authors != null && !authors.isEmpty()) pluginData.put("authors", Compat.listCopy(authors));
        if (contributors != null && !contributors.isEmpty()) pluginData.put("contributors", Compat.listCopy(contributors));
        if (website != null) pluginData.put("website", getWebsite());
        if (prefix != null) pluginData.put("prefix", prefix);
        if (apiVersion != null) pluginData.put("api-version", apiVersion);
        if (scalaVersion != null) pluginData.put("scala-version", scalaVersion);
        if (loadOrder != null) pluginData.put("load", loadOrder.name());
        if (permissionDefault != null) pluginData.put("default-permission", permissionDefault.name());
        if (!hardDependencies.isEmpty()) pluginData.put("depend", Compat.listCopy(hardDependencies));
        if (!softDependencies.isEmpty()) pluginData.put("softdepend", Compat.listCopy(softDependencies));
        if (!inverseDependencies.isEmpty()) pluginData.put("loadbefore", Compat.listCopy(inverseDependencies));
        if (!provides.isEmpty()) pluginData.put("provides", Compat.listCopy(provides));
        if (!mavenDependencies.isEmpty()) pluginData.put("libraries", Compat.listCopy(mavenDependencies));
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
        String pluginYaml = yaml.dump(pluginData); //this can be quite a large string though. Preferably I'd use a YamlReader, however I can't find any class that does that (yet).

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
        Map<Permission, Boolean> children = permission.getChildren();
        if (!children.isEmpty()) {
            Map<String, Object> childrenMap = new HashMap<>();
            for (Map.Entry<Permission, Boolean> entry : children.entrySet()) {
                Permission child = entry.getKey();
                Boolean enabled = entry.getValue();
                childrenMap.put(child.getName(), createPermissionMap(child, permissionDefault(permissionDefault, !enabled.booleanValue())));
            }
            currentPermission.put("children", childrenMap);
        }

        return currentPermission;
    }

    private static PermissionDefault permissionDefault(PermissionDefault permissionDefault, boolean inverse) {
        if (inverse) {
            switch (permissionDefault) {
                case NOT_OP: return PermissionDefault.OP;
                case OP: return PermissionDefault.NOT_OP;
                case TRUE: return PermissionDefault.FALSE;
                case FALSE: return PermissionDefault.TRUE;
                default:
                    throw new RuntimeException("Unknown PermissionDefault: " + permissionDefault);
            }
        } else {
            return permissionDefault;
        }
    }

    public void readFromPluginYamlData(Map<String, Object> pluginYaml) {
        addYaml(pluginYaml);

        Object apiVersion = pluginYaml.get("api-version");
        if (apiVersion != null)
            setApiVersion(String.valueOf(apiVersion));
        Object scalaVersion = pluginYaml.get("scala-version");
        if (scalaVersion != null)
            setScalaVersion(String.valueOf(scalaVersion));
        Object mainClass = pluginYaml.get("main");
        if (mainClass != null)
            setMain(mainClass.toString());
        Object foliaSupported = pluginYaml.get("folia-supported");
        if (foliaSupported != null)
            setFoliaSupported(Boolean.parseBoolean(foliaSupported.toString()));
        description((String) pluginYaml.get("description"));
        String author = (String) pluginYaml.get("author");
        if (author != null)
            addAuthor(author);
        Iterable authors = (Iterable) pluginYaml.get("authors");
        if (authors != null)
            for (Object auth : authors)
                if (auth != null)
                    addAuthor(auth.toString());
        Iterable contributors = (Iterable) pluginYaml.get("contributors");
        if (contributors != null)
            for (Object contrib : contributors)
                if (contrib != null)
                    addContributor(contrib.toString());
        website((String) pluginYaml.get("website"));
        prefix((String) pluginYaml.get("prefix"));
        String load = (String) pluginYaml.get("load");
        if (load != null)
            loadOrder(PluginLoadOrder.valueOf(load));
        String defaultPermissionDefault = (String) pluginYaml.get("default-permission");
        if (defaultPermissionDefault != null)
            permissionDefault(PermissionDefault.getByName(defaultPermissionDefault));
        List<String> depend = (List<String>) pluginYaml.get("depend");
        if (depend != null)
            for (String dep : depend)
                if (dep != null)
                    addHardDepend(dep);
        List<String> softDepend = (List<String>) pluginYaml.get("softdepend");
        if (softDepend != null)
            for (String softDep : softDepend)
                if (softDep != null)
                    addSoftDepend(softDep);
        List<String> inverseDepend = (List<String>) pluginYaml.get("loadbefore");
        if (inverseDepend != null)
            for (String inverseDep : inverseDepend)
                if (inverseDep != null)
                    addLoadBefore(inverseDep);
        List<Map<String, Object>> paperDependencies = (List<Map<String, Object>>) pluginYaml.get("dependencies");
        if (paperDependencies != null)
            for (Map<String, Object> paperDependency : paperDependencies)
                if (Boolean.parseBoolean(paperDependency.get("required").toString()))
                    addHardDepend(paperDependency.get("name").toString());
                else
                    addSoftDepend(paperDependency.get("name").toString());
        List<String> provides = (List<String>) pluginYaml.get("provides");
        if (provides != null)
            provides(provides.toArray(new String[0]));
        List<String> mavenDeps = (List<String>) pluginYaml.get("libraries");
        if (mavenDeps != null)
            mavenDependencies(mavenDeps.toArray(new String[0]));
        Map<String, Map<String, Object>> commands = (Map) pluginYaml.get("commands");
        if (commands != null) {
            for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
                String cmdName = entry.getKey();
                Map<String, Object> value = entry.getValue();
                Command cmd = new Command(cmdName);
                cmd.description((String) value.get("description"));
                cmd.usage((String) value.get("usage"));
                cmd.permission((String) value.get("permission"));
                cmd.permissionMessage((String) value.get("permission-message"));
                Iterable aliases = (Iterable) value.get("aliases");
                for (Object alias : aliases) cmd.addAlias(alias.toString());
                addCommand(cmd);
            }
        }
        Map<String, Map<String, Object>> permissions = (Map) pluginYaml.get("permissions");
        if (permissions != null) {
            for (Map.Entry<String, Map<String, Object>> entry : permissions.entrySet()) {
                String permissionName = entry.getKey();
                Map<String, Object> properties = entry.getValue();
                Permission perm = makePermission(permissionName, properties);
                addPermission(perm);
            }
        }
    }

    private static Permission makePermission(String name, Map<String, Object> properties) {
        Permission perm = new Permission(name);

        perm.description((String) properties.get("description"));
        String def = (String) properties.get("default");
        if (def != null) perm.permissionDefault(PermissionDefault.getByName(def));
        Object children = properties.get("children");
        if (children instanceof Iterable) {
            Iterable kids = (Iterable) children;
            for (Object kid : kids) {
                perm.addChild(new Permission(kid.toString()));
            }
        } else if (children instanceof Map) {
            Map<String, Map<String, Object>> kids = (Map) children;
            for (Map.Entry<String, Map<String, Object>> kid : kids.entrySet()) {
                perm.addChild(makePermission(kid.getKey(), kid.getValue()));
            }
        }

        return perm;
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
        private LinkedHashMap<Permission, Boolean> children;

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
            for (Permission child : children) {
                addChild(child);
            }
            return this;
        }

        public Permission addChild(Permission child) {
            addChild(child, true);
            return this;
        }

        public Permission addChild(Permission child, boolean enabled) {
            if (children == null) children = new LinkedHashMap<>();
            children.put(child, enabled);
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

        public Map<Permission, Boolean> getChildren() {
            return children == null ? Compat.emptyMap() : Collections.unmodifiableMap(children);
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
