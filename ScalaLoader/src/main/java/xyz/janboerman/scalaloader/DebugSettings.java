package xyz.janboerman.scalaloader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class DebugSettings {

    private static final String FILE_NAME = "debug.yml";
    private static final String CLASS_NAMES = "class-names";
    private static final String FORMAT = "format";
    private static final String ANALYSIS = "analysis"; //value would be List<String>: the methods that need analyzing

    public static final String TEXTIFIED = "Textified";
    public static final String ASMIFIED = "ASMified";

    private static final String LOG_MISSING_CODECS = "log-missing-codecs";
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private static final String ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED = "enabled-when-jvm-assertions-are-enabled";

    private enum MissingCodecLog {
        ENABLED, DISABLED, ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED;

        @Override
        public String toString() {
            switch (this) {
                case ENABLED: return DebugSettings.ENABLED;
                case DISABLED: return DebugSettings.DISABLED;
                case ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED: return DebugSettings.ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED;
                default: throw new AssertionError("unreachable");
            }
        }

        static MissingCodecLog fromString(String string) {
            if (string == null) return Default();
            switch (string) {
                case DebugSettings.ENABLED: return ENABLED;
                case DebugSettings.DISABLED: return DISABLED;
                case DebugSettings.ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED: return ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED;
                default: return Default();
            }
        }

        public boolean isEnabled() {
            switch (this) {
                case ENABLED: return true;
                case DISABLED: return false;
                default:
                    try {
                        assert false;
                        return false;
                    } catch (AssertionError ae) {
                        return true;
                    }
            }
        }

        static MissingCodecLog Default() {
            return ENABLED_WHEN_JVM_ASSERTIONS_ARE_ENABLED;
        }
    }

    private final IScalaLoader scalaLoader;
    private File saveFile;

    //Synchronized because the ScalaPluginClassLoader is parallel capable! Some classes may be loaded in a different thread than the server's primary thread!
    private final Set<String> classNames = Collections.synchronizedSet(new LinkedHashSet<>());
    private String format = TEXTIFIED; private final Object formatLock = new Object();
    private MissingCodecLog missingCodecLog = MissingCodecLog.Default();
    private boolean logMissingCodecs = true;

    public DebugSettings(IScalaLoader scalaLoader) {
        this.scalaLoader = scalaLoader;
        this.saveFile = new File(scalaLoader.getDataFolder(), FILE_NAME);
        if (saveFile.exists()) try {
            load();
        } catch (IOException e) {
            scalaLoader.getLogger().log(Level.WARNING, "Could not load debug classes.", e);
        }
    }

    public Set<String> debugClassLoads() {
        return Collections.unmodifiableSet(classNames);
    }

    public String getFormat() {
        synchronized (formatLock) {
            return format;
        }
    }

    public boolean isDebuggingClassLoadOf(String className) {
        return classNames.contains(className);
    }

    public boolean debugClass(String className) {
        classNames.add(className);
        try {
            save();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean undebugClass(String className) {
        classNames.remove(className);
        try {
            save();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean logMissingCodecs() {
        return logMissingCodecs;
    }

    public void load() throws IOException {
        if (saveFile != null && saveFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(saveFile);
            List<String> classNames = config.getStringList(CLASS_NAMES);
            this.classNames.clear();
            this.classNames.addAll(classNames);

            String format = config.getString(FORMAT, TEXTIFIED);
            synchronized (formatLock) {
                this.format = format;
            }

            this.missingCodecLog = MissingCodecLog.fromString(config.getString(LOG_MISSING_CODECS));
            this.logMissingCodecs = missingCodecLog.isEnabled();
        }
    }

    private void save() throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set(CLASS_NAMES, Compat.listCopy(classNames));
        config.set(FORMAT, getFormat());
        config.set(LOG_MISSING_CODECS, missingCodecLog.toString());
        config.save(getSaveFile());
    }

    private File getSaveFile() throws IOException {
        if (!saveFile.exists()) saveFile.createNewFile();

        return saveFile;
    }


}
