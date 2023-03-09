package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.DebugSettings;
import xyz.janboerman.scalaloader.compat.IScalaLoader;

import java.io.File;

public class ScalaLoader extends JavaPlugin implements IScalaLoader {

    public ScalaLoader() {
    }

    @Override
    public boolean isPaperPlugin() {
        return true;
    }

    @Override
    public DebugSettings getDebugSettings() {
        //TODO
        return null;
    }

    @Override
    public File getScalaPluginsFolder() {
        //TODO
        return null;
    }


    @Override
    public void onLoad() {
        //TODO load ScalaPlugins

    }

    @Override
    public void onEnable() {
        //TODO enable ScalaPlugins

    }

}
