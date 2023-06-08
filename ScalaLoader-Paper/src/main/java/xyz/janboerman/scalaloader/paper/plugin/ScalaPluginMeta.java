package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import io.papermc.paper.plugin.provider.configuration.type.DependencyConfiguration;
import io.papermc.paper.plugin.provider.configuration.type.DependencyConfiguration.LoadOrder;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginLoadOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ScalaPluginMeta extends PaperPluginMeta /*implements PluginMeta*/ {

    //TODO separate paper plugin dependency configuration?
    final ScalaPluginDescription description;

    public ScalaPluginMeta(ScalaPluginDescription description) {
        this.description = Objects.requireNonNull(description);
    }

    //PluginMeta overrides

    @Override
    public @NotNull String getDisplayName() {
        return description.getFullName();
    }

    @Override
    public @NotNull String getName() {
        return description.getName();
    }

    @Override
    public @NotNull String getMainClass() {
        return description.getMain();
    }

    @Override
    public @NotNull PluginLoadOrder getLoadOrder() {
        return description.getLoadOrder();
    }

    @Override
    public @NotNull String getVersion() {
        return description.getVersion();
    }

    @Override
    public @Nullable String getLoggerPrefix() {
        return description.getPrefix();
    }

    @Override
    public @NotNull List<String> getPluginDependencies() {
        return Compat.listCopy(description.getHardDependencies());
    }

    @Override
    public @NotNull List<String> getPluginSoftDependencies() {
        return Compat.listCopy(description.getSoftDependencies());
    }

    @Override
    public @NotNull List<String> getLoadBeforePlugins() {
        return Compat.listCopy(description.getInverseDependencies());
    }

    @Override
    public @NotNull List<String> getProvidedPlugins() {
        return Compat.listCopy(description.getProvides());
    }

    //TODO track new paper-style dependency configuration separately?
    @Override
    public Map<String, DependencyConfiguration> getServerDependencies() {
        Map<String, DependencyConfiguration> res = new HashMap<>();

        for (String pluginName : getPluginDependencies()) {
            res.put(pluginName, new DependencyConfiguration(LoadOrder.BEFORE, true, true));
        }
        for (String pluginName : getPluginSoftDependencies()) {
            res.put(pluginName, new DependencyConfiguration(LoadOrder.OMIT, false, true));
        }
        for (String pluginName : getLoadBeforePlugins()) {
            res.put(pluginName, new DependencyConfiguration(LoadOrder.AFTER, true, true));
        }

        return res;
    }

    //TODO track new paper-style dependency configuration separately?
    @Override
    public Map<String, DependencyConfiguration> getBoostrapDependencies() {
        Map<String, DependencyConfiguration> res = new HashMap<>();

        for (String pluginName : getPluginDependencies()) {
            res.put(pluginName, new DependencyConfiguration(LoadOrder.BEFORE, true, true));
        }

        return res;
    }

    @Override
    public @NotNull List<String> getAuthors() {
        return description.getAuthors();
    }

    @Override
    public @NotNull List<String> getContributors() {
        return Compat.listCopy(description.getContributors());
    }

    @Override
    public @Nullable String getDescription() {
        return description.getDescription();
    }

    @Override
    public @Nullable String getWebsite() {
        return description.getWebsite();
    }

    @Override
    public @NotNull List<Permission> getPermissions() {
        return getPermissions(description.getPermissions());
    }

    @Override
    public @NotNull PermissionDefault getPermissionDefault() {
        return description.getPermissionDefault();
    }

    @Override
    public @Nullable String getAPIVersion() {
        return description.getApiVersion();
    }

    private static List<Permission> getPermissions(Collection<ScalaPluginDescription.Permission> permissions) {
        List<Permission> result = new ArrayList<>(permissions.size());
        for (ScalaPluginDescription.Permission permission : permissions) {
            result.add(toBukkitPermission(permission));
        }
        return result;
    }

    private static Permission toBukkitPermission(ScalaPluginDescription.Permission permission) {
        String name = permission.getName();
        String description = permission.getDescription().orElse(null);
        PermissionDefault permissionDefault = permission.getDefault().orElse(null);
        Map<String, Boolean> children = getChildren(permission.getChildren());
        return new Permission(name, description, permissionDefault, children);
    }

    private static Map<String, Boolean> getChildren(Map<ScalaPluginDescription.Permission, Boolean> children) {
        Map<String, Boolean> result = new HashMap<>();
        for (Map.Entry<ScalaPluginDescription.Permission, Boolean> child : children.entrySet()) {
            ScalaPluginDescription.Permission childPermission = child.getKey();
            Boolean enabled = child.getValue();
            result.put(childPermission.getName(), enabled);
        }
        return result;
    }

    //PaperPluginMeta overrides

    @Override
    public String getBootstrapper() {
        String bootstrapper = description.getBootstrapperName();
        if (bootstrapper == null)
            bootstrapper = ScalaPluginBootstrap.class.getName();
        return bootstrapper;
    }

    @Override
    public String getLoader() {
        return ScalaPluginLoader.class.getName();
    }

    @Override
    public boolean hasOpenClassloader() {
        return description.hasOpenClassLoader();
    }

    //only exists on Folia.
    /*@Override*/ public boolean isFoliaSupported() {
        return description.isFoliaSupported();
    }

    //ScalaPluginMeta-specific

    public String getScalaVersion() {
        return description.getScalaVersion();
    }

    public Set<String> getMavenDependencies() {
        return description.getMavenDependencies();
    }

}
