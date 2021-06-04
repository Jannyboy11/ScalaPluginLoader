package xyz.janboerman.scalaloader.dependency;

import java.io.File;
import java.util.*;
import java.util.logging.*;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.*;
import org.eclipse.aether.artifact.*;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.*;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.*;
import org.eclipse.aether.spi.connector.transport.*;
import org.eclipse.aether.transfer.*;
import org.eclipse.aether.transport.http.*;
//import org.eclipse.aether.transport.file.*;
import org.eclipse.aether.impl.*;
import org.eclipse.aether.graph.*;

import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;

/* Re-Implementation of:
 * https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/commits/146a7e4bd764990c56bb326643e92eb69f24d27e#src/main/java/org/bukkit/plugin/java/LibraryLoader.java
 * CraftBukkit commit: https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/commits/5bbb4a65a4a81dea32c29a5ecd6786a844036efb
 */
/**
 * This class is NOT part of the public API!
 * But it implements loading of libraries that are defined in the plugin.yml
 *
 * @see <a href="https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/plugin/PluginDescriptionFile.html#getLibraries()">PluginDescriptionFile.getLibraries()</a>
 */
public class PluginYamlLibraryLoader {

    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();
    private static final RemoteRepository CODE_MC = new RemoteRepository.Builder("CodeMC", "default", "https://repo.codemc.org/repository/maven-public/").build();

    private final Logger logger;
    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public PluginYamlLibraryLoader(Logger logger, File localRepoDir) {
        this.logger = logger;

        this.system = getRepositorySystem();
        this.session = getSession(system, logger, localRepoDir);
        this.repositories = system.newResolutionRepositories(session, Compat.listOf(MAVEN_CENTRAL, CODE_MC));
    }

    public Collection<File> getJarFiles(Map<String, Object> pluginYaml) throws ScalaPluginLoaderException {
        Object o = pluginYaml.get("libraries");
        if (!(o instanceof List)) return Compat.emptySet();

        final List list = (List) o;
        if (list.isEmpty()) return Compat.emptySet();

        final int size = list.size();
        logger.log(Level.INFO, "Loading {0} libraries... please wait", size);

        final List<Dependency> dependencies = new ArrayList<>(size);

        for (Object item : list) {
            if (item instanceof String) {
                String library = (String) item;

                Artifact artifact = new DefaultArtifact(library);
                Dependency dependency = new Dependency(artifact, "compile");
                dependencies.add(dependency);
            }
        }
        if (dependencies.isEmpty()) return Compat.emptySet();

        try {
            CollectRequest collectRequest = new CollectRequest((Dependency) null, dependencies, repositories);
            DependencyRequest request = new DependencyRequest(collectRequest, null);
            DependencyResult result = system.resolveDependencies(session, request);

            List<File> jarFiles = new ArrayList<>(dependencies.size());
            for (ArtifactResult artifactResult : result.getArtifactResults()) {
                jarFiles.add(artifactResult.getArtifact().getFile());
            }
            return jarFiles;

        } catch (DependencyResolutionException e) {
            throw new ScalaPluginLoaderException("Can't download dependencies", e);
        }
    }

    private static RepositorySystemSession getSession(RepositorySystem system, Logger logger, File localRepoDir) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, new LocalRepository(localRepoDir)));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferStarted(TransferEvent event) throws TransferCancelledException {
                logger.log(Level.INFO, "Downloading {0}", event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
            }
        });
        session.setReadOnly();

        return session;
    }

    private static RepositorySystem getRepositorySystem() {
        DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
        serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        //serviceLocator.addService(TransporterFactory.class, FileTransporterFactory.class);
        serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        return serviceLocator.getService(RepositorySystem.class);
    }

}
