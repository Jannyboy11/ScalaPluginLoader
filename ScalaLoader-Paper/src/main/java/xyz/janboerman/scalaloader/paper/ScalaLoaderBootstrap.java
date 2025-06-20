package xyz.janboerman.scalaloader.paper;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.paper.logging.ComponentLoggerWrapper;
import xyz.janboerman.scalaloader.paper.plugin.PluginJarScanResult;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginBootstrapClassLoader;
import xyz.janboerman.scalaloader.paper.plugin.ScalaPluginMeta;
import xyz.janboerman.scalaloader.paper.plugin.description.DescriptionPlugin;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency;
import xyz.janboerman.scalaloader.paper.transform.MainClassCallerMigrator;
import xyz.janboerman.scalaloader.plugin.ScalaCompatMap;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class ScalaLoaderBootstrap implements PluginBootstrap {

    /*@Override*/
    public void bootstrap(@NotNull PluginProviderContext context) {
        //context provides access to:
        //  - configuration     (PaperPluginMeta)
        //  - logger            (ComponentLogger)
        //  - data directory    (Path)

        ComponentLogger logger = context.getLogger();

        Path dataDir = context.getDataDirectory();
        Path scalaPluginsDir = dataDir.resolve("scalaplugins");
        Path librariesDir = dataDir.resolve("libraries");
        Path scalaLibrariesDir = dataDir.resolve("scalaLibraries");

        ScalaCompatMap<ScalaDependency> scalaVersionMap = new ScalaCompatMap<>();

        File[] scalaPluginFiles = scalaLibrariesDir.toFile().listFiles((File dir, String name) -> name.endsWith(".jar"));
        for (File scalaPluginFile : scalaPluginFiles) {
            // TODO Instantiate a bootstrapper based on the presence of the 'bootstrapper' property in the ScalaPluginDescription.
            // TODO in order to get the ScalaPluginDescription, we must first 'run' the instantiation of the Description plugin.

            try {
                // Scan the jar
                PluginJarScanResult scanResult = ScalaPluginLoading.read(Compat.jarFile(scalaPluginFile));

                //save scala version
                final ScalaDependency scalaDependency = scanResult.getScalaVersion();
                if (scalaDependency == null) {
                    logger.error("Could not find Scala dependency while bootstrapping plugin {}. Please annotate your main class with @Scala or @CustomScala.", scalaPluginFile.getName());
                    continue;
                }
                scalaVersionMap.add(scalaDependency);

                final ApiVersion apiVersion = scanResult.getApiVersion();

                //register the GetClassLoaderMigrator (so that every call to MyPluginMainClass.getClassLoader() will be replaced by MyPluginMainClass.classLoader())
                final String mainClassName = scanResult.getMainClass();
                scanResult.transformerRegistry.addUnspecificTransformer(visitor -> new MainClassCallerMigrator(visitor, mainClassName));

                //Now, we instantiate the DescriptionPlugin
                Logger julLogger = new ComponentLoggerWrapper(logger);
                var optionalDescriptionPlugin = ScalaPluginLoading.buildDescriptionPlugin(scalaPluginFile, scanResult, apiVersion, mainClassName, scalaDependency, julLogger, librariesDir.toFile());
                if (optionalDescriptionPlugin.isEmpty()) continue;
                final DescriptionPlugin descriptionPlugin = optionalDescriptionPlugin.get();
                //TODO do we need to store the DescriptionPlugin anywhere? I don't think so. The only reason we create the DescriptionPlugin is to obtain the bootstrapper.

                ScalaPluginDescription description = descriptionPlugin.getScalaDescription();
                String bootstrapperName = descriptionPlugin.getScalaDescription().getBootstrapperName();
                // We *finally* obtained the class name of the ScalaPlugin's bootstrapper class!
                if (bootstrapperName != null) {
                    // Instantiate the bootstrapper class in our current classloading context.
                    ClassLoader parentClassLoader = ScalaLoaderBootstrap.class.getClassLoader();

                    // TODO should we instantiate a ScalaPluginProvidingContext / BootstrapContext here? maybe!
                    Path scalaPluginPath = scalaPluginFile.toPath();
                    JarFile scalaPluginJarFile = Compat.jarFile(scalaPluginFile);
                    ScalaPluginMeta scalaPluginMeta = new ScalaPluginMeta(description);

                    // TODO which classloader should we use to load and instantiate the bootstrapper? Paper seems to use a PaperSimplePluginClassLoader.
                    // TODO probably, we should use our own classloader which supports our own bytecode modifications.
                    ScalaPluginBootstrapClassLoader bootstrapperClassLoader = new ScalaPluginBootstrapClassLoader(scalaPluginPath, scalaPluginJarFile, scalaPluginMeta, parentClassLoader, scanResult, julLogger);
                    // TODO Paper seems to use the classloader that is present in the PaperPluginParent. It's also the classloader used to load the PaperPluginProviderFactory.
                    // TODO it also seems that the PaperSimplePluginClassLoader is only used to instantiate the PluginLoader, not the Bootstrapper.

                } // else: bootstrapperName is null. The ScalaPlugin will be instantiated through the original ScalaLoader bootstrapping process.


                // TODO remove this old logic when we are sure we don't need it anymore.
//                descriptionPlugins.put(scalaPluginFile, descriptionPlugin);

//                //and set the description
//                ScalaPluginDescription description = descriptionPlugin.getScalaDescription();
//                final String pluginName;
//                if (description != null) {
//                    //ScalaPluginDescription constructor was used! :)
//                    description.readFromPluginYamlData(scanResult.pluginYaml);
//                    pluginName = description.getName();
//                } else {
//                    //No-args constructor was used. Get the description from the pluginYaml.
//                    pluginName = scanResult.pluginYaml.get("name").toString();
//                    String version = scanResult.pluginYaml.get("version").toString();
//                    description = new ScalaPluginDescription(pluginName, version);
//                    description.readFromPluginYamlData(scanResult.pluginYaml);
//                }
//                description.setMain(mainClassName);
//                description.setApiVersion(apiVersion.getVersionString());
//                description.setScalaVersion(scalaDependency.getVersionString());

            } catch (IOException | ScalaPluginLoaderException e) {
                logger.error("Could not bootstrap plugin: " + scalaPluginFile.getName(), e);
            }

            // TODO and then what? register the bootstrapper somewhere.
            // TODO either Paper itself must continue the loading of ScalaPlugins, or ScalaLoader itself must do it.
        }
    }






    //

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        bootstrap((PluginProviderContext) context);
    }

    // TODO also override createPlugin so that we can dependency-inject the scalaplugins directory (and libraries directory?) as constructor arguments.
    // TODO can we also inject the ScalaCompatMap? I don't think so because the ScalaLoader plugin can't use them at runtime (due to being loaded by the bootstrap classloader).

}
