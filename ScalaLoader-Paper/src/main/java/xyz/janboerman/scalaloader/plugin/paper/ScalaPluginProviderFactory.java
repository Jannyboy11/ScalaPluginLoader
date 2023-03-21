package xyz.janboerman.scalaloader.plugin.paper;

import com.destroystokyo.paper.utils.PaperPluginLogger;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.bootstrap.PluginProviderContextImpl;
import io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import io.papermc.paper.plugin.loader.PaperClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.provider.type.PluginTypeFactory;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import io.papermc.paper.plugin.provider.util.ProviderUtil;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class ScalaPluginProviderFactory {

    public PaperPluginParent build(JarFile file, ScalaPluginMeta configuration, Path source) throws Exception {
        Logger jul = PaperPluginLogger.getLogger(configuration);
        ComponentLogger logger = ComponentLogger.logger(jul.getName());
        PluginProviderContext context = PluginProviderContextImpl.of(configuration, logger);

        PaperClasspathBuilder builder = new PaperClasspathBuilder(context);

        if (configuration.getLoader() != null) {
            try (
                    PaperSimplePluginClassLoader simplePluginClassLoader = new PaperSimplePluginClassLoader(source, file, configuration, this.getClass().getClassLoader())
            ) {
                PluginLoader loader = ProviderUtil.loadClass(configuration.getLoader(), PluginLoader.class, simplePluginClassLoader);
                loader.classloader(builder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        PaperPluginClassLoader classLoader = builder.buildClassLoader(jul, source, file, configuration);
        return new PaperPluginParent(source, file, configuration, classLoader, context);
    }

    public ScalaPluginMeta create(JarFile jarFile) throws Exception {
        JarEntry paperPluginConfig = jarFile.getJarEntry("paper-plugin.yml");
        if (paperPluginConfig != null) {
            //TODO
        }

        if (paperPluginConfig == null) {
            JarEntry bukkitPluginConfig = jarFile.getJarEntry("plugin.yml");

            //TODO
        }

        //TODO scan the jar

        //TODO instantiate the plugin

        //TODO
        return null;
    }
}
