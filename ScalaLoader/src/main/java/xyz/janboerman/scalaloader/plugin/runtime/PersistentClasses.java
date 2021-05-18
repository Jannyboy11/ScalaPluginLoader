package xyz.janboerman.scalaloader.plugin.runtime;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PersistentClasses {

    private static final String FILE_NAME = "generated-classes.yml";
    private static final String CLASS_FILES = "class-files";

    private final ScalaPlugin plugin;
    private File saveFile;

    //this set is not meant tobe accessible from the outside world.
    //the only entity that is allowed to interact with PersistentClasses is the ScalaPluginClassLoader.
    //and it does so through the #save and #load methods.
    private final Set<ClassFile> classFiles = new HashSet<>();

    public PersistentClasses(ScalaPlugin plugin) {
        assert plugin != null : "plugin cannot be null";

        this.plugin = plugin;
        this.saveFile = new File(plugin.getDataFolder(), FILE_NAME);
    }

    public void save(ClassFile classFile) {
        this.classFiles.add(classFile);

        if (!saveFile.exists()) {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            try {
                saveFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(saveFile);
        List<ClassFile> classFiles = (List<ClassFile>) config.get(CLASS_FILES);

        this.classFiles.addAll(classFiles);
        config.set(CLASS_FILES, Compat.listCopy(this.classFiles));
        try {
            config.save(saveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Collection<ClassFile> load() {
        if (!saveFile.exists()) return Compat.emptySet();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(saveFile);
        List<ClassFile> classFiles = (List<ClassFile>) config.get(CLASS_FILES);

        return classFiles;
    }
}
