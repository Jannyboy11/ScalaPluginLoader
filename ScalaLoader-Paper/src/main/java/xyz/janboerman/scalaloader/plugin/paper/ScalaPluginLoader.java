package xyz.janboerman.scalaloader.plugin.paper;

import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import io.papermc.paper.plugin.provider.configuration.PaperPluginMeta;
import io.papermc.paper.plugin.provider.type.PluginTypeFactory;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

import org.jetbrains.annotations.NotNull;


/*  According to https://docs.papermc.io/paper/dev/getting-started/paper-plugins#loaders
 *  The purpose of a plugin loader is to create the (expected/dynamic) environment for the plugin to load into.
 */
public class ScalaPluginLoader implements PluginLoader, IScalaPluginLoader {

    //TODO should there be a ScalaPluginLoader instance per ScalaPlugin?
    //TODO there could be, but that is not strictly required.
    //TODO for now: YES.
    //TODO Paper's own implementation does exactly this though (see PaperPluginProviderFactory).
    //TODO We don't need to copy this behaviour necessarily, since the #classloader method is called for each plugin.

    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
    private static final RemoteRepository CODE_MC = new RemoteRepository.Builder("CodeMC", "default", "https://repo.codemc.org/repository/maven-public/").build();


    @Override
    public DebugSettings debugSettings() {
        return ScalaLoader.getInstance().getDebugSettings();
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
        mavenLibraryResolver.addRepository(MAVEN_CENTRAL);
        mavenLibraryResolver.addRepository(CODE_MC);

        //add scala libraries
        //TODO if the scala libraries were declared using @Scala, then resolve it using maven,
        //TODO OR if the scala libraries were declared in the plugin.yml or paper-plugin.yml with the scala-scalaVersion property:
        //TODO      mavenLibraryResolver.addDependency(new Dependency(new DefaultArtifact(gav), "compile"))
        //TODO if the scala libraries were delcared using @CustomScala, download them and add jar libraries to the classpath
        //TODO      classpathBuilder.addLibrary(new JarLibrary(donloadedFile.toPath())

        //TODO for the first and second approach, implement bytecode scanning in ScalaLibraryScanner
        //TODO for the second approach, make sure to use the same download location that the 'bukkit' ScalaLoader plugin uses.
        //TODO the folder to which these should be downloaded is: File scalaLibsFolder = new File(getScalaLoader().getDataFolder(), "scalalibraries");

        //add plugin-declared maven dependencies
        //TODO should we do this in the bootstrapper?
        for (String gav : scalaPluginMeta.getMavenDependencies()) {
            Artifact artifact = new DefaultArtifact(gav);
            Dependency dependency = new Dependency(artifact, "compile");
            mavenLibraryResolver.addDependency(dependency);
        }

        classpathBuilder.addLibrary(mavenLibraryResolver);
    }

}
