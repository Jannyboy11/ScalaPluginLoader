package xyz.janboerman.scalaloader.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import xyz.janboerman.scalaloader.ScalaLoader;
import xyz.janboerman.scalaloader.compat.Compat;
import xyz.janboerman.scalaloader.compat.IScalaLoader;
import xyz.janboerman.scalaloader.compat.IScalaPlugin;
import xyz.janboerman.scalaloader.compat.IScalaPluginClassLoader;
import xyz.janboerman.scalaloader.plugin.ScalaPlugin;
import xyz.janboerman.scalaloader.plugin.ScalaPluginClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class DumpClass implements TabExecutor {

    private static final String TEXTIFIED = "Textified";
    private static final String ASMIFIED = "ASMified";

    private final IScalaLoader scalaLoader;

    public DumpClass(IScalaLoader scalaLoader) {
        this.scalaLoader = scalaLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) return false;

        final String pluginInput = args[0];
        final String classFileInput = args[1];
        final String formatInput = args.length >= 3 && ASMIFIED.equalsIgnoreCase(args[2])
                ? ASMIFIED
                : TEXTIFIED;

        PluginManager pluginManager = scalaLoader.getServer().getPluginManager();
        Plugin plugin = pluginManager.getPlugin(pluginInput);
        if (plugin == null) {
            sender.sendMessage(ChatColor.RED + "Unknown plugin: " + pluginInput);
            return true;
        }

        try {
            File jarFile = getJarFile(plugin);
            if (jarFile == null) {
                sender.sendMessage(ChatColor.RED + "Could not detect plugin's jar file. Currently this command only supports ScalaPlugins and JavaPlugins.");
                return true;
            }

            JarFile jar = Compat.jarFile(jarFile);
            JarEntry jarEntry = jar.getJarEntry(classFileInput);
            if (jarEntry == null) {
                sender.sendMessage(ChatColor.RED + "Class file " + classFileInput + " does not exist in plugin " + plugin.getName());
                return true;
            }

            try (InputStream inputStream = jar.getInputStream(jarEntry)) {
                Printer printer;
                switch (formatInput) {
                    case ASMIFIED:
                        printer = new ASMifier();
                        break;
                    case TEXTIFIED:
                        printer = new Textifier();
                        break;
                    default:
                        assert false : "format was not ASMified or Textified?";
                        printer = null;
                        break;
                }
                dump(inputStream, printer);
            }

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
            scalaLoader.getLogger().log(Level.WARNING, "Unable to open jar file for plugin: " + plugin.getName(), e);
            sender.sendMessage(ChatColor.RED + "Could not read class file from plugin.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        PluginManager pluginManager = scalaLoader.getServer().getPluginManager();

        if (args.length == 0) {
            return Arrays.stream(pluginManager.getPlugins())
                    .map(Plugin::getName)
                    .collect(Collectors.toList());
        }

        else if (args.length == 1) {
            final String pluginNameInput = args[0];

            return Arrays.stream(pluginManager.getPlugins())
                    .map(Plugin::getName)
                    .filter(pluginName -> StringUtil.startsWithIgnoreCase(pluginName, pluginNameInput))
                    .collect(Collectors.toList());
        }

        else if (args.length == 2) {
            final String pluginNameInput = args[0];
            final String classFileInput = args[1];

            ArrayList<String> result = new ArrayList<>();
            Plugin plugin = pluginManager.getPlugin(pluginNameInput);

            //get jarfile from JavaPlugin or ScalaPlugin (best effort, technically there could be more options)
            File jarFile = null;
            try {
                jarFile = getJarFile(plugin);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                scalaLoader.getLogger().log(Level.WARNING, "Could not read plugin's jar file.", e);
                sender.sendMessage(ChatColor.RED + "Could not read plugin's jar file.");
            }

            //if we succeeded, then tab-complete for existing entries
            if (jarFile != null) {
                try {
                    JarFile jar = Compat.jarFile(jarFile);
                    Enumeration<? extends ZipEntry> enumeration = jar.entries();
                    while (enumeration.hasMoreElements()) {
                        ZipEntry entry = enumeration.nextElement();
                        String entryName = entry.getName();
                        if (entryName.endsWith(".class") && entryName.startsWith(classFileInput)) {
                            result.add(entryName);
                        }
                    }
                } catch (IOException e) {
                    scalaLoader.getLogger().log(Level.WARNING, "Could not get entries from jar file.", e);
                    sender.sendMessage(ChatColor.RED + "Could not get entries from jar file.");
                }
            } else {
                //neither a java nor scala plugin
                result.add(classFileInput);
            }

            return result;
        }

        else if (args.length == 3) {
            final String formatInput = args[2];

            if (formatInput.isEmpty()) return Compat.listOf(TEXTIFIED, ASMIFIED);
            if (StringUtil.startsWithIgnoreCase(TEXTIFIED, formatInput)) return Compat.singletonList(TEXTIFIED);
            else if (StringUtil.startsWithIgnoreCase(ASMIFIED, formatInput)) return Compat.singletonList(ASMIFIED);
            else return Compat.emptyList();
        }

        else {
            return Compat.emptyList();
        }
    }

    private static File getJarFile(Plugin plugin) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        File jarFile;

        if (plugin instanceof JavaPlugin) {
            JavaPlugin javaPlugin = (JavaPlugin) plugin;
            Method getFile = JavaPlugin.class.getDeclaredMethod("getFile");
            getFile.setAccessible(true);
            jarFile = (File) getFile.invoke(javaPlugin);
        } else if (plugin instanceof IScalaPlugin) {
            IScalaPlugin scalaPlugin = (IScalaPlugin) plugin;
            IScalaPluginClassLoader classLoader = scalaPlugin.classLoader();
            jarFile = classLoader.getPluginJarFile();
        }

        else {
            jarFile = null;
        }

        return jarFile;
    }

    private static void dump(InputStream classBytes, Printer printer) throws IOException {
        ClassReader debugReader = new ClassReader(classBytes);
        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, new PrintWriter(System.out));
        debugReader.accept(traceClassVisitor, 0);
    }

}
