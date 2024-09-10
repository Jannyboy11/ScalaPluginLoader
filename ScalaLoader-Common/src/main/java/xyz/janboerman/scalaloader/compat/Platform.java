package xyz.janboerman.scalaloader.compat;

import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.plugin.PluginDescriptionFile;
import static xyz.janboerman.scalaloader.compat.Compat.getPackageName;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;

import java.io.StringReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Set;

/**
 * This class is NOT part of the public API!
 */
public class Platform {

    private static final String FAKE_PLUGIN_NAME = "Fake";

    protected Platform() {
    }

    public static final CraftBukkitPlatform CRAFTBUKKIT = new CraftBukkitPlatform();
    public static final GlowstonePlatform GLOWSTONE = new GlowstonePlatform();
    private static final Platform UNKNOWN = new Platform();

    private Boolean conversionMethodExists = null;

    @SuppressWarnings("deprecation")
    public <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] transform(String jarEntryPath, byte[] original, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
        if (conversionMethodExists == null || conversionMethodExists) {
            try {
                Server server = currentPluginClassLoader.getServer();
                UnsafeValues unsafeValues = server.getUnsafe();
                String fakeDescription = "name: " + FAKE_PLUGIN_NAME + System.lineSeparator() +
                        "version: 1.0" + System.lineSeparator() +
                        "main: xyz.janboerman.scalaloader.FakePlugin" + System.lineSeparator();
                ApiVersion apiVersion = currentPluginClassLoader.getApiVersion();
                if (apiVersion != ApiVersion.LEGACY) {
                    //If api-version is not set, this will be ApiVersion.latest(). We assume all ScalaPlugins are made in the post-1.13 era.
                    fakeDescription += "api-version: " + apiVersion.getVersionString() + System.lineSeparator();
                }

                PluginDescriptionFile pluginDescriptionFile = new PluginDescriptionFile(new StringReader(fakeDescription));

                byte[] processed = unsafeValues.processClass(pluginDescriptionFile, jarEntryPath, original);
                conversionMethodExists = true;
                return processed;
            } catch (NoSuchMethodError e) {
                //UnsafeValues#processClass does not exist, just return the original class bytes
                conversionMethodExists = false;
            }
        }

        return original;
    }

    public static Platform detect(Server server) {
        if (server.getClass().getName().startsWith("org.bukkit.craftbukkit.")) {
            return Platform.CRAFTBUKKIT;
        } else if (server.getClass().getName().startsWith("net.glowstone.")) {
            return Platform.GLOWSTONE;
        } else {
            return Platform.UNKNOWN;
        }
    }

    // built-in implementations:

    public static class CraftBukkitPlatform extends Platform {

        private static final Class<?> API_VERSION_CLASS;
        static {
            Class<?> apiVersionClass;
            try {
                apiVersionClass = Class.forName("org.bukkit.craftbukkit.util.ApiVersion");
            } catch (ClassNotFoundException e) {
                apiVersionClass = null;
            }
            API_VERSION_CLASS = apiVersionClass;
        }

        private CraftBukkitPlatform() {}

        private MethodHandle commodoreConvert = null;
        private boolean attempted = false;

        public <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] transformNative(Server craftServer, byte[] classBytes, ScalaPluginClassLoader pluginClassLoader) throws Throwable {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            if (commodoreConvert == null) {
                attempted = true;
                try {
                    // public static byte[] convert(byte[] b, final String pluginName, final ApiVersion pluginVersion, final Set<String> activeCompatibilities)
                    Class<?> commodoreClass = Class.forName(getPackageName(craftServer.getClass()) + ".util.Commodore");
                    String methodName = "convert";
                    MethodType methodType = MethodType.methodType(byte[].class,
                            new Class<?>[] { byte[].class, String.class, API_VERSION_CLASS, Set.class });
                    commodoreConvert = lookup.findStatic(commodoreClass, methodName, methodType);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException ignored) {
                    //impossible
                }
            }

            if (commodoreConvert != null) {
                String pluginName = getPluginName(pluginClassLoader);
                try {
                    MethodHandle getOrCreateVersion = lookup.findStatic(API_VERSION_CLASS, "getOrCreateVersion", MethodType.methodType(API_VERSION_CLASS, String.class));
                    Object apiVersion = getOrCreateVersion.invoke(pluginClassLoader.getApiVersion().getVersionString());

                    Set activeCompatibilities = Collections.emptySet();
                    try {
                        MethodHandle compatibilitiesGetter = lookup.findGetter(craftServer.getClass(), "activeCompatibilities", Set.class);
                        activeCompatibilities = (Set) compatibilitiesGetter.invoke(craftServer);
                    } catch (Exception couldNotDetermineActiveCompatibilities) {
                    }

                    classBytes = (byte[]) commodoreConvert.invoke(classBytes, pluginName, apiVersion, activeCompatibilities);
                } catch (NoSuchMethodException | IllegalAccessException ignored) {
                }
            }

            return classBytes;
        }

        public byte[] transformNative(Server craftServer, byte[] classBytes, boolean modern) throws Throwable {
            if (!attempted) {
                attempted = true;
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    // public static byte[] convert(byte[] b, boolean isModern)
                    Class<?> commodoreClass = Class.forName(getPackageName(craftServer.getClass()) + ".util.Commodore");
                    String methodName = "convert";
                    MethodType methodType = MethodType.methodType(byte[].class,
                            new Class<?>[] { byte[].class, boolean.class });
                    commodoreConvert = lookup.findStatic(commodoreClass, methodName, methodType);
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException ignored) {
                    //running on craftbukkit 1.12.2 or earlier
                }
            }

            if (commodoreConvert != null) {
                classBytes = (byte[]) commodoreConvert.invoke(classBytes, modern);
            }

            return classBytes;
        }

        @Override
        public <ScalaPluginClassLoader extends ClassLoader & IScalaPluginClassLoader> byte[] transform(String jarEntryPath, byte[] classBytes, ScalaPluginClassLoader pluginClassLoader) throws Throwable {
            if (API_VERSION_CLASS != null) {
                return transformNative(pluginClassLoader.getServer(), classBytes, pluginClassLoader);
            } else {
                return transformNative(pluginClassLoader.getServer(), classBytes, pluginClassLoader.getApiVersion() != ApiVersion.LEGACY);
            }
        }

        private static String getPluginName(IScalaPluginClassLoader classLoader) {
            IScalaPlugin plugin = classLoader.getPlugin();
            if (plugin == null) {
                return FAKE_PLUGIN_NAME;
            } else {
                return plugin.getName();
            }
        }
    }

    public static class GlowstonePlatform extends Platform {

        private GlowstonePlatform() {}

//        @Override
//        public byte[] transform(String jarEntryPath, byte[] original, ScalaPluginClassLoader currentPluginClassLoader) throws Throwable {
//            GlowServer glowServer = (GlowServer) currentPluginClassLoader.getServer();
//            GlowUnsafeValues glowUnsafeValues = (GlowUnsafeValues) glowServer.getUnsafe();
//            glowUnsafeValues.processClass() -- not yet implemented in the GlowStone 1.16 branch
//        }
    }

    // Folia
    private static final boolean FOLIA;
    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

}
