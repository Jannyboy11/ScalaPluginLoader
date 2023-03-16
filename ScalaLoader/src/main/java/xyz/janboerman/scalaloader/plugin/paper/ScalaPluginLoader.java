package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import io.papermc.paper.plugin.provider.configuration.type.DependencyConfiguration;
import io.papermc.paper.plugin.provider.type.PluginTypeFactory;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;
import xyz.janboerman.scalaloader.dependency.PluginYamlLibraryLoader;
import xyz.janboerman.scalaloader.event.EventBus;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;

public class ScalaPluginLoader implements PluginLoader, IScalaPluginLoader {

    private static final ScalaPluginLoader INSTANCE = new ScalaPluginLoader();

    private ScalaLoader scalaLoader;
    private EventBus eventBus;

    public static ScalaPluginLoader getInstance() {
        return INSTANCE;
    }

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        //TODO is this where we want to do the scanning? where are we called?

        //TODO this PluginLoader#classLoader is called by PaperPluginProviderFactory#build
        //TODO so we need to implement this method, if we use the same factory.

        PluginTypeFactory<PaperPluginParent, PaperPluginMeta> paperPluginTypeFactory = PaperPluginParent.FACTORY;

        //TODO we need to make sure we do call PaperClassPathBuilder.buildClassLoader!

        PluginProviderContext pluginProviderContext = classpathBuilder.getContext();
        ScalaPluginMeta scalaPluginMeta = (ScalaPluginMeta) pluginProviderContext.getConfiguration();

        MavenLibraryResolver mavenLibraryResolver = new MavenLibraryResolver();
        mavenLibraryResolver.addRepository(PluginYamlLibraryLoader.MAVEN_CENTRAL);
        mavenLibraryResolver.addRepository(PluginYamlLibraryLoader.CODE_MC);

        //add scala libraries
        //TODO if the scala libraries were declared using @Scala, then resolve it using maven,
        //TODO OR if the scala libraries were declared in the plugin.yml or paper-plugin.yml with the scala-version property:
        //TODO      mavenLibraryResolver.addDependency(new Dependency(new DefaultArtifact(gav), "compile"))
        //TODO if the scala libraries were delcared using @CustomScala, download them and add jar libraries to the classpath
        //TODO      classpathBuilder.addLibrary(new JarLibrary(donloadedFile.toPath())

        //TODO for the first and second approach, implement bytecode scaning in ScalaLibraryScanner
        //TODO for the second approach, make sure to use the same download location that the 'bukkit' ScalaLoader plugin uses.
        //TODO the folder to which these should be downloaded is: File scalaLibsFolder = new File(getScalaLoader().getDataFolder(), "scalalibraries");

        //add plugin-declared maven dependencies
        for (DependencyConfiguration dependencyConfig : scalaPluginMeta.getDependencies()) {
            Artifact artifact = null; //TODO: new DefaultArtifact(dependencyConfig.name());
            Dependency dependency = new Dependency(artifact, "compile");
            mavenLibraryResolver.addDependency(dependency);
        }

        classpathBuilder.addLibrary(mavenLibraryResolver);
    }

    //TODO much like the xyz.janboerman.scalaloader.plugin.ScalaPluginLoader,
    //TODO this class is responsible for scanning the jar file for classes, collecting ScanResults,
    //TODO and detecting the main class (in case a plugin.yml or paper-plugin.yml is absent)
    //TODO additionally, the scala standard library must be appended to the plugin's classpath.


    ScalaLoader getScalaLoader() {
        return scalaLoader == null ? scalaLoader = JavaPlugin.getPlugin(ScalaLoader.class) : scalaLoader;
    }

    public DebugSettings debugSettings() {
        return getScalaLoader().getDebugSettings();
    }

    //TODO getScalaPlugins()

    public EventBus getEventBus() {
        return eventBus == null ? eventBus = new EventBus(Bukkit.getPluginManager()) : eventBus;
    }



}
