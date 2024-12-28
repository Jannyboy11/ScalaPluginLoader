package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.loader.library.impl.JarLibrary;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import static xyz.janboerman.scalaloader.compat.Compat.listOf;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.compat.IScalaPluginLoader;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;

import org.jetbrains.annotations.NotNull;
import xyz.janboerman.scalaloader.paper.ScalaLoader;
import xyz.janboerman.scalaloader.plugin.description.ScalaVersion;
import xyz.janboerman.scalaloader.paper.plugin.description.ScalaDependency;
import xyz.janboerman.scalaloader.util.ScalaLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/*  According to https://docs.papermc.io/paper/dev/getting-started/paper-plugins#loaders
 *  The purpose of a plugin loader is to create the (expected/dynamic) environment for the plugin to load into.
 */
public class ScalaPluginLoader implements PluginLoader, IScalaPluginLoader {

    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
    private static final RemoteRepository CODE_MC = new RemoteRepository.Builder("CodeMC", "default", "https://repo.codemc.org/repository/maven-public/").build();

    @Override
    public DebugSettings debugSettings() {
        return ScalaLoader.getInstance().getDebugSettings();
    }

    @Override
    public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {

        ScalaPluginClasspathBuilder classpathBuilder = (ScalaPluginClasspathBuilder) pluginClasspathBuilder;
        ScalaPluginProviderContext context = classpathBuilder.getContext();
        ScalaPluginMeta scalaPluginMeta = context.getConfiguration();

        MavenLibraryResolver mavenLibraryResolver = new MavenLibraryResolver();
        mavenLibraryResolver.addRepository(MAVEN_CENTRAL);
        mavenLibraryResolver.addRepository(CODE_MC);

        //add scala libraries
        ScalaDependency scalaDependency = ScalaLoader.getInstance().getScalaVersions().getLatestVersion(scalaPluginMeta.getScalaVersion());
        if (scalaDependency instanceof ScalaDependency.Builtin builtinDep) {
            for (String gav : mavenDependencies(builtinDep.scalaVersion().getVersion()))
                addMavenDependency(mavenLibraryResolver, gav);
        } else if (scalaDependency instanceof ScalaDependency.Custom customDep) {
            try {
                for (File file : downloadScalaLibraries(customDep.scalaVersion(), customDep.urls(), customDep.sha1hashes(), ScalaLoader.getInstance()))
                    classpathBuilder.addLibrary(new JarLibrary(file.toPath()));
            } catch (IOException e) {
                ScalaLoader.getInstance().getLogger().log(Level.SEVERE, "Could not download scala libraries for version: " + customDep, e);
            }
        } else if (scalaDependency instanceof ScalaDependency.YamlDefined yamlDefinedDep) {
            for (String gav : mavenDependencies(yamlDefinedDep.scalaVersion()))
                addMavenDependency(mavenLibraryResolver, gav);
        }

        //add plugin-declared maven dependencies
        for (String gav : scalaPluginMeta.getMavenDependencies())
            addMavenDependency(mavenLibraryResolver, gav);

        classpathBuilder.addLibrary(mavenLibraryResolver);
    }

    private static void addMavenDependency(MavenLibraryResolver mavenLibraryResolver, String gav) {
        Artifact artifact = new DefaultArtifact(gav);
        Dependency dependency = new Dependency(artifact, "compile");
        mavenLibraryResolver.addDependency(dependency);
    }

    private static List<String> mavenDependencies(String scalaVersion) {
        boolean isScala2 = scalaVersion.startsWith("2.");
        if (isScala2)
            return listOf(
                    "org.scala-lang:scala-library:" + scalaVersion,
                    "org.scala-lang:scala-reflect:" + scalaVersion);

        boolean isScala3 = scalaVersion.startsWith("3.");
        if (isScala3)
            if ("3.0.0".equals(scalaVersion))
                return listOf(
                        "org.scala-lang:scala-library:" + ScalaVersion.getLatest_2_13().getVersion(),
                        "org.scala-lang:scala-reflect:" + ScalaVersion.getLatest_2_13().getVersion(),
                        "org.scala-lang:scala3-library_3.0.0-nonbootstrapped:3.0.0",
                        "org.scala-lang:tasty-core_3.0.0-nonbootstrapped:3.0.0");
            else
                return listOf(
                        "org.scala-lang:scala-library:" + ScalaVersion.getLatest_2_13().getVersion(),
                        "org.scala-lang:scala-reflect:" + ScalaVersion.getLatest_2_13().getVersion(),
                        "org.scala-lang:scala3-library_3:" + scalaVersion,
                        "org.scala-lang:tasty-core_3:" + scalaVersion);

        throw new RuntimeException("Unrecognised Scala version: " + scalaVersion);
    }

    private static File[] downloadScalaLibraries(String scalaVersion, Map<String, String> urls, Map<String, String> sha1hashes, ScalaLoader scalaLoader) throws IOException {
        // TODO shouldn't need ScalaLoader argument - a logger and a data directory suffice.

        File scalaLibsFolder = new File(scalaLoader.getDataFolder(), "scalalibraries");
        File versionFolder = new File(scalaLibsFolder, scalaVersion);
        versionFolder.mkdirs();

        File[] jarFiles = versionFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (jarFiles.length == 0) {
            //no jar files found - download dem files
            scalaLoader.getLogger().info("Tried to load Scala " + scalaVersion + " libraries from disk, but they were not present. Downloading...");

            Map<String, String> urlMap = urls;
            jarFiles = new File[urlMap.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : urlMap.entrySet()) {
                String fileKey = entry.getKey();
                String urlForKey = entry.getValue();

                String fileName;
                if (fileKey.endsWith("-url")) { //see PluginScalaVersion
                    fileName = fileKey.substring(0, fileKey.length() - 3) + scalaVersion + ".jar";
                } else if (fileKey.endsWith(".jar")) {
                    fileName = fileKey;
                } else {
                    fileName = fileKey + "-" + scalaVersion + ".jar";
                }

                File scalaRuntimeJarFile = new File(versionFolder, fileName);
                scalaRuntimeJarFile.createNewFile();
                URL url = new URL(urlForKey);
                ScalaLoaderUtils.downloadFile(url, scalaRuntimeJarFile, sha1hashes.get(fileKey));
                jarFiles[i++] = scalaRuntimeJarFile;
            }
        }

        return jarFiles;
    }

}
