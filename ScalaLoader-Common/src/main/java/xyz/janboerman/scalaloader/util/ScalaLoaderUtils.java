package xyz.janboerman.scalaloader.util;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.commands.DumpClass;
import xyz.janboerman.scalaloader.commands.ListScalaPlugins;
import xyz.janboerman.scalaloader.commands.ResetScalaUrls;
import xyz.janboerman.scalaloader.commands.SetDebug;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.plugin.PluginScalaVersion;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassFile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ScalaLoaderUtils {

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
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.Option.registerWithConfigurationSerialization();
        xyz.janboerman.scalaloader.configurationserializable.runtime.types.Either.registerWithConfigurationSerialization();
    }

    public static <ScalaLoader extends JavaPlugin & IScalaLoader> void initCommands(ScalaLoader scalaLoader) {
        scalaLoader.getCommand("resetScalaUrls").setExecutor(new ResetScalaUrls(scalaLoader));
        scalaLoader.getCommand("dumpClass").setExecutor(new DumpClass(scalaLoader));
        scalaLoader.getCommand("setDebug").setExecutor(new SetDebug(scalaLoader.getDebugSettings()));
        scalaLoader.getCommand("listScalaPlugins").setExecutor(new ListScalaPlugins());
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
     * Tries to get the plugin instance from the scala plugin class.
     * This method is able to get the static instances from scala `object`s,
     * as well as it is able to create plugins using their public NoArgsConstructors.
     * @param clazz the plugin class
     * @param <P> the plugin type
     * @return the plugin's instance.
     * @throws ScalaPluginLoaderException when a plugin instance could not be created for the given class
     */
    public static <P extends IScalaPlugin> P createScalaPluginInstance(Class<P> clazz) throws ScalaPluginLoaderException {
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

            try {
                Object pluginInstance = module$Field.get(null);
                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Couldn't access static field MODULE$ in class " + clazz.getName(), e);
            }
        } else {
            //we found are a regular class.
            //it should have a public zero-argument constructor

            try {
                Constructor<?> ctr = clazz.getConstructor();
                Object pluginInstance = ctr.newInstance();

                return clazz.cast(pluginInstance);
            } catch (IllegalAccessException e) {
                throw new ScalaPluginLoaderException("Could not access the NoArgsConstructor of " + clazz.getName() + ", please make it public", e);
            } catch (InvocationTargetException e) {
                throw new ScalaPluginLoaderException("Error instantiating class " + clazz.getName() + ", its constructor threw something at us", e);
            } catch (NoSuchMethodException e) {
                throw new ScalaPluginLoaderException("Could not find NoArgsConstructor in class " + clazz.getName(), e);
            } catch (InstantiationException e) {
                throw new ScalaPluginLoaderException("Could not instantiate class " + clazz.getName(), e);
            }
        }
    }

}
