package xyz.janboerman.scalaloader.paper.plugin;

import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public class PaperHacks {

    private PaperHacks() {}

    public static PaperPluginManagerImpl getPaperPluginManager() {
        PluginManager bukkitPluginManager = Bukkit.getPluginManager();
        if (bukkitPluginManager instanceof PaperPluginManagerImpl paperImpl) {
            return paperImpl;
        } else {
            return PaperPluginManagerImpl.getInstance();
        }
    }



}
