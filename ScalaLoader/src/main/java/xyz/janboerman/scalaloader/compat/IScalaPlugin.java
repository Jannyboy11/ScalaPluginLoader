package xyz.janboerman.scalaloader.compat;

import org.bukkit.plugin.Plugin;
import xyz.janboerman.scalaloader.ScalaRelease;
import xyz.janboerman.scalaloader.event.EventBus;

import java.io.File;

public interface IScalaPlugin extends Plugin {

    public EventBus getEventBus();


    public String getScalaVersion();

    public ScalaRelease getScalaRelease();


    public String getDeclaredScalaVersion();

    public String getName();

    public File getConfigFile();

}
