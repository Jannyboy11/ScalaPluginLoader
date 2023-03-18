package xyz.janboerman.scalaloader.compat;

import xyz.janboerman.scalaloader.DebugSettings;

import java.util.Collection;

public interface IScalaPluginLoader {

    public Collection<? extends IScalaPlugin> getScalaPlugins();

    public DebugSettings debugSettings();

}