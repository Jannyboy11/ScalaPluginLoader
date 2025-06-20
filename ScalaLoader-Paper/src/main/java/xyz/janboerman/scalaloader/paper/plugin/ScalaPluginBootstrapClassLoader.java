package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.entrypoint.classloader.ClassloaderBytecodeModifier;
import io.papermc.paper.plugin.entrypoint.classloader.PaperSimplePluginClassLoader;
import org.bukkit.Bukkit;
import xyz.janboerman.scalaloader.compat.Platform;
import xyz.janboerman.scalaloader.plugin.ScalaPluginLoaderException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScalaPluginBootstrapClassLoader extends PaperSimplePluginClassLoader {

    private final PluginJarScanResult scanResult;
    private final Logger logger;

    public ScalaPluginBootstrapClassLoader(
            Path source,
            JarFile file,
            ScalaPluginMeta configuration,
            ClassLoader parentLoader,
            PluginJarScanResult pluginJarScanResult,
            Logger logger) throws IOException {
        super(source, file, configuration, parentLoader);
        this.scanResult = pluginJarScanResult;
        this.logger = logger;
    }



    /**
     * Transform bytecode suitable for the bootstrap ClassLoader.
     * <p>
     * The only bytecode transformations performed by this function are those which are present in the server software itself.
     * No bytecode transformations from ScalaLoader will be applied.
     */
    private byte[] transformBytecode(String className, byte[] byteCode) {
        //Paper-supported bytecode transformer via ServiceLoader api!
        try {
            byteCode = ClassloaderBytecodeModifier.bytecodeModifier().modify(this.configuration, byteCode);
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "ClassloaderBytecodeModifier could not transform class: " + className, e);
        }

        //CraftBukkit transformations
        try {
            byteCode = Platform.CRAFTBUKKIT.transformNative(Bukkit.getServer(), byteCode, getPluginName(scanResult), scanResult.getApiVersionString());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, "Server implementation could not transform class: " + className, e);
        }

        return byteCode;
    }

    private static String getPluginName(PluginJarScanResult scanResult) {
        if (scanResult.pluginYaml.get("name") instanceof String pluginName) {
            return pluginName;
        } else {
            try {
                String mainClassName = scanResult.getMainClass();
                String[] sections = mainClassName.split("\\.");
                return sections[sections.length - 1];
            } catch (ScalaPluginLoaderException e) {
                return "Fake_ScalaLoader_Bootstrapper";
            }
        }
    }

}
