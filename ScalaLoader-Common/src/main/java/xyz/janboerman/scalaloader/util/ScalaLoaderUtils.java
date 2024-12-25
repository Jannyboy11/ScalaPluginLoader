package xyz.janboerman.scalaloader.util;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.ScalaLibraryClassLoader;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal use only.
 */
public final class ScalaLoaderUtils {

    private ScalaLoaderUtils() {
    }

    public static <ScalaLoader extends JavaPlugin & IScalaLoader> void initConfiguration(ScalaLoader scalaLoader) {
        //ScalaLoader config stuff
        scalaLoader.saveDefaultConfig();
        PluginScalaVersion.register();
        ClassFile.register();
        FileConfiguration config = scalaLoader.getConfig();
        if (!config.isList("scala-versions")) {
            scalaLoader.getConfig().set("scala-versions", Arrays.stream(ScalaVersion.values()).map(PluginScalaVersion::fromScalaVersion).collect(Collectors.toList()));
            scalaLoader.saveConfig();
        }

        //ScalaPlugin config stuff
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.Primitives.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.NumericRange.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.UUID.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.BigInteger.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.BigDecimal.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.DateTime.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.Option.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.Either.registerWithConfigurationSerialization();
    }

    public static <ScalaLoader extends JavaPlugin & IScalaLoader> void initBStats(ScalaLoader scalaLoader) {
        final int pluginId = 9150;
        Metrics metrics = new Metrics(scalaLoader, pluginId);
        metrics.addCustomChart(new DrilldownPie("declared_scala_version", () -> {
            Map<String /*compat-release version*/, Map<String /*actual version*/, Integer /*amount*/>> stats = new HashMap<>();

            for (IScalaPlugin scalaPlugin : scalaLoader.getScalaPlugins()) {
                String scalaVersion = scalaPlugin.getDeclaredScalaVersion();
                String compatVersion = ScalaRelease.fromScalaVersion(scalaVersion).getCompatVersion();

                stats.computeIfAbsent(compatVersion, k -> new HashMap<>())
                        .compute(scalaVersion, (v, amount) -> amount == null ? 1 : amount + 1);
            }

            return stats;
        }));
        //TODO track used features of the ScalaLoader plugin -> ConfigurationSerializable api?, Event api? (could make these drilldowns!)
        //TODO track popular third-party libraries (once we include a third-party library loading api) (using advanced pie!)
    }

    /**
     * Tries to obtain an instance of the given class either by using an `object`'s singleton instance, or by calling the public no-args consturctor.
     * @param clazz the class for which to create an instance
     * @return the instance
     */
    public static <Super, Type extends Super> Type instantiate(Class<Type> clazz) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        boolean endsWithDollar = clazz.getName().endsWith("$");
        boolean hasStaticFinalModule$;
        Field module$Field;
        boolean hasPrivateConstructor;
        try {
            module$Field = clazz.getDeclaredField("MODULE$");
            int modifiers = module$Field.getModifiers();
            if (module$Field.getType().equals(clazz)
                    && (modifiers & Modifier.STATIC) == Modifier.STATIC
                    && (modifiers & Modifier.FINAL) == Modifier.FINAL) {
                hasStaticFinalModule$ = true;
            } else {
                hasStaticFinalModule$ = false;
            }
        } catch (NoSuchFieldException e) {
            hasStaticFinalModule$ = false;
            module$Field = null;
        }
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        hasPrivateConstructor = constructors.length == 1 && (constructors[0].getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE;

        //this seems to be how the scala compiler encodes 'object's. not sure if this is actually specified or an implementation detail.
        //in any case, it works good enough for me!
        boolean isObjectSingleton = endsWithDollar && hasStaticFinalModule$ && hasPrivateConstructor;

        if (isObjectSingleton) {
            //we found a scala singleton object.
            //the instance is already present in the MODULE$ field when this class is loaded.
            Object pluginInstance = module$Field.get(null);
            return clazz.cast(pluginInstance);
        } else {
            //we found are a regular class.
            //it should have a public zero-argument constructor
            Constructor<?> ctr = clazz.getConstructor();
            Object pluginInstance = ctr.newInstance();
            return clazz.cast(pluginInstance);
        }
    }

    /**
     * Tries to get the plugin instance from the scala plugin class.
     * This method is able to get the static instances from scala `object`s,
     * as well as it is able to create plugins using their public NoArgsConstructors.
     * @param clazz the plugin class
     * @param <P> the plugin type
     * @return the plugin's instance.
     * @throws ScalaPluginLoaderException when a plugin instance could not be created for the given class
     */
    public static <P extends IScalaPlugin> P createScalaPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
       try {
           return ScalaLoaderUtils.<IScalaPlugin, P>instantiate(clazz);
        } catch (IllegalAccessException e) {
            throw new ScalaPluginLoaderException("Could not access MODULE$ field or NoArgsConstructor of " + clazz.getName() + ", please make it public", e);
        } catch (InvocationTargetException e) {
            throw new ScalaPluginLoaderException("Error instantiating class " + clazz.getName() + ", its constructor threw something at us", e);
        } catch (NoSuchMethodException e) {
            throw new ScalaPluginLoaderException("Could not find NoArgsConstructor in class " + clazz.getName(), e);
        } catch (InstantiationException e) {
            throw new ScalaPluginLoaderException("Could not instantiate class " + clazz.getName(), e);
        }
    }

    public static void downloadFile(URL inputResourceLocation, File outputFile, String sha1hash) throws IOException {
        downloadFile(inputResourceLocation, outputFile);
        checkSha1Hash(outputFile, sha1hash);
    }

    /** @deprecated Use {@linkplain #downloadFile(URL, File, String)} instead.*/
    @Deprecated
    public static void downloadFile(URL inputResourceLocation, File outputFile) throws IOException {
        // TODO adjust call sites.

        ReadableByteChannel rbc = null;
        FileOutputStream fos = null;
        try {
            rbc = Channels.newChannel(inputResourceLocation.openStream());
            fos = new FileOutputStream(outputFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } finally {
            if (rbc != null) try { rbc.close(); } catch (IOException e) { e.printStackTrace(); }
            if (fos != null) try { fos.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }


    private static void checkSha1Hash(File outputFile, String sha1hash) throws IOException {
        if (sha1hash == null || sha1hash.isEmpty()) return;

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            try (FileInputStream fis = new FileInputStream(outputFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    sha1.update(buffer, 0, bytesRead);
                }
            }

            StringBuilder hashValue = new StringBuilder();
            for (byte b : sha1.digest()) {
                hashValue.append(String.format("%02x", b));
            }

            if (!sha1hash.equals(hashValue)) {
                throw new IOException("Unexpected hash for " + outputFile.getName() + ", expected: " + sha1hash + ", actual: " + hashValue);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Could not find SHA-1 MessageDigest.", e);
        }
    }

    /**
     * Get a (fresh or cached) {@link ScalaLibraryClassLoader} that loads standard library classes from a specific Scala version.
     * The classloader can either load classes from over the network directly, or use downloaded library archives (jar files).
     * @param scalaVersion the scala version
     * @return the class loader
     * @throws ScalaPluginLoaderException if a url is malformed.
     */
    public static ScalaLibraryClassLoader loadOrGetScalaVersion(Map<String, ScalaLibraryClassLoader> scalaLibraryClassLoaders,
                                                                PluginScalaVersion scalaVersion,
                                                                boolean download,
                                                                IScalaLoader scalaLoader) throws ScalaPluginLoaderException {

        //try to get from cache
        ScalaLibraryClassLoader scalaLibraryLoader = scalaLibraryClassLoaders.get(scalaVersion.getScalaVersion());
        if (scalaLibraryLoader != null) return scalaLibraryLoader;

        if (!download) {
            //load classes over the network
            scalaLoader.getLogger().info("Loading Scala " + scalaVersion + " libraries from over the network");
            try {
                Map<String, String> urlMap = scalaVersion.getUrls();
                URL[] urls = new URL[urlMap.size()];
                int i = 0;
                for (String url : urlMap.values()) {
                    urls[i++] = new URL(url);
                }
                scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), urls, scalaLoader.getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw new ScalaPluginLoaderException("Could not load scala libraries for version " + scalaVersion + " due to a malformed URL", e);
            }
        } else {
            //check if downloaded already (if not, do download)
            //then load classes from the downloaded jar

            File scalaLibsFolder = new File(scalaLoader.getDataFolder(), "scalalibraries");
            File versionFolder = new File(scalaLibsFolder, scalaVersion.getScalaVersion());
            versionFolder.mkdirs();

            File[] jarFiles = versionFolder.listFiles((dir, name) -> name.endsWith(".jar"));

            if (jarFiles.length == 0) {
                //no jar files found - download dem files
                scalaLoader.getLogger().info("Tried to load Scala " + scalaVersion + " libraries from disk, but they were not present. Downloading...");

                Map<String, String> sha1Map = scalaVersion.getSha1Hashes();
                Map<String, String> urlMap = scalaVersion.getUrls();
                jarFiles = new File[urlMap.size()];
                int i = 0;
                for (Map.Entry<String, String> entry : urlMap.entrySet()) {
                    String urlKey = entry.getKey();
                    String urlValue = entry.getValue();

                    String fileName;
                    if (urlKey.endsWith("-url")) {
                        fileName = urlKey.substring(0, urlKey.length() - 3) + scalaVersion.getScalaVersion() + ".jar";
                    } else if (urlKey.endsWith(".jar")) {
                        fileName = urlKey;
                    } else {
                        fileName = urlKey + "-" + scalaVersion.getScalaVersion() + ".jar";
                    }

                    File scalaRuntimeJarFile = new File(versionFolder, fileName);

                    try {
                        scalaRuntimeJarFile.createNewFile();
                    } catch (IOException e) {
                        throw new ScalaPluginLoaderException("Could not create new jar file", e);
                    }

                    try {
                        URL url = new URL(urlValue);
                        downloadFile(url, scalaRuntimeJarFile, sha1Map.get(urlKey));
                    } catch (MalformedURLException e) {
                        throw new ScalaPluginLoaderException("Invalid url for key: " + urlKey, e);
                    } catch (IOException e) {
                        throw new ScalaPluginLoaderException("Could not open or close channel", e);
                    }

                    jarFiles[i++] = scalaRuntimeJarFile;
                }
            }

            scalaLoader.getLogger().info("Loading Scala " + scalaVersion.getScalaVersion() + " libraries from disk");
            //load jar files.
            URL[] urls = new URL[jarFiles.length];
            for (int i = 0; i < jarFiles.length; i++) {
                try {
                    urls[i] = jarFiles[i].toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new ScalaPluginLoaderException("Could not load Scala libraries for version " + scalaVersion.getScalaVersion() + " due to a malformed URL", e);
                }
            }

            scalaLibraryLoader = new ScalaLibraryClassLoader(scalaVersion.getScalaVersion(), urls, scalaLoader.getClass().getClassLoader());
        }

        //cache the resolved scala library classloader
        scalaLibraryClassLoaders.put(scalaVersion.getScalaVersion(), scalaLibraryLoader);
        return scalaLibraryLoader;
    }

}
