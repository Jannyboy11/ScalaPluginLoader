package xyz.janboerman.scalaloader.compat;

import org.bukkit.Server;
import xyz.janboerman.scalaloader.plugin.description.ApiVersion;
import xyz.janboerman.scalaloader.plugin.runtime.ClassDefineResult;
import xyz.janboerman.scalaloader.plugin.runtime.ClassGenerator;

import java.io.File;

public interface IScalaPluginClassLoader {

    public File getPluginJarFile();

    public String getMainClassName();

    public ApiVersion getApiVersion();

    public ClassDefineResult getOrDefineClass(String className, ClassGenerator classGenerator, boolean persist);

    public Server getServer();

    public IScalaPlugin getPlugin();

    public IScalaPluginLoader getPluginLoader();
}
