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

/**
 * This class is NOT part of the public API!
 */
public class Platform {

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
                String fakeDescription = "name: Fake" + System.lineSeparator() +
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

        private CraftBukkitPlatform() {}

        private MethodHandle commodoreConvert = null;
        private boolean attempted = false;

        public byte[] transformNative(Server craftServer, byte[] classBytes, boolean modern) throws Throwable {
            if (!attempted) {
                attempted = true;
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    Class<?> commodoreClass = Class.forName(getPackageName(craftServer.getClass()) + ".util.Commodore");
                    String methodName = "convert";
                    MethodType methodType = MethodType.methodType(byte[].class, new Class<?>[]{byte[].class, boolean.class});
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
            return transformNative(pluginClassLoader.getServer(), classBytes, pluginClassLoader.getApiVersion() != ApiVersion.LEGACY);
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
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
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
