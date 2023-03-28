package xyz.janboerman.scalaloader.paper.plugin.description;

import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.ClassLoaderAccess;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import org.jetbrains.annotations.Nullable;
import xyz.janboerman.scalaloader.paper.ScalaLoader;

class ScalaLoaderGroup implements PluginClassLoaderGroup {

    private static final boolean DISABLE_CLASS_PRIORITIZATION = Boolean.getBoolean("Paper.DisableClassPrioritization");

    private DescriptionClassLoader descriptionClassLoader;

    ScalaLoaderGroup(DescriptionClassLoader descriptionClassLoader) {
        this.descriptionClassLoader = descriptionClassLoader;
    }

    @Override
    public @Nullable Class<?> getClassByName(String name, boolean resolve, ConfiguredPluginClassLoader requester) {
        if (!DISABLE_CLASS_PRIORITIZATION) {
            try {
                return requester.loadClass(name, resolve, /*checkGlobal*/false, /*checkLibraries*/true);
            } catch (ClassNotFoundException ignored) {
            }
        }

        //first, try DescriptionClassLoader
        try {
            return descriptionClassLoader.loadClass(name, resolve, false, true);
        } catch (ClassNotFoundException ignored) {
        }

        //next, try ScalaLoader
        try {
            ClassLoader scalaLoaderClassLoader = ScalaLoader.getInstance().getClass().getClassLoader();
            if (scalaLoaderClassLoader instanceof PaperPluginClassLoader paperClassLoader) {
                return paperClassLoader.loadClass(name, resolve, false, true);
            } else {
                return scalaLoaderClassLoader.loadClass(name);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    @Override
    public void remove(ConfiguredPluginClassLoader configuredPluginClassLoader) {
        throw new UnsupportedOperationException("Can't remove classloader from this group.");
    }

    @Override
    public void add(ConfiguredPluginClassLoader configuredPluginClassLoader) {
        throw new UnsupportedOperationException("Can't add classloader to this group.");
    }

    @Override
    public ClassLoaderAccess getAccess() {
        return classLoader -> {
            return classLoader == descriptionClassLoader
                    || classLoader == ScalaLoader.getInstance().getClass().getClassLoader();
        };
    }
}
