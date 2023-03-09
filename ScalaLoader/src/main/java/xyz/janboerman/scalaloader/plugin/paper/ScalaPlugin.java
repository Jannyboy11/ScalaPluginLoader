package xyz.janboerman.scalaloader.plugin.paper;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginDescription;

public class ScalaPlugin extends JavaPlugin {

    private ScalaPluginDescription description; //TODO use ScalaPaperPluginMeta instead?

    public ScalaPlugin() {
        //TODO get description from classloader/bootstrapper/loader/whatever.
    }

    public ScalaPlugin(ScalaPluginDescription description) {
        this.description = description;
    }

}
