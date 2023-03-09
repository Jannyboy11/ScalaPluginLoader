package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.configuration.PluginMeta;
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
import java.util.stream.Collectors;

public class ScalaPluginMeta implements PluginMeta {

    private final ScalaPluginDescription description;

    public ScalaPluginMeta(ScalaPluginDescription description) {
        this.description = Objects.requireNonNull(description);
    }

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

}
