package xyz.janboerman.scalaloader.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ClassMembers implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;

        String className = args[0];
        try {
            Class<?> clazz = Class.forName(className);
            sender.sendMessage(ChatColor.YELLOW + "Fields:");
            for (Field f : clazz.getFields()) {
                sender.sendMessage(ChatColor.YELLOW + formatField(f));
            }
            for (Constructor<?> c : clazz.getConstructors()) {
                sender.sendMessage(ChatColor.YELLOW + formatConstructor(c));
            }
            sender.sendMessage(ChatColor.YELLOW + "Methods:");
            for (Method m : clazz.getMethods()) {
                sender.sendMessage(ChatColor.YELLOW + formatMethod(m));
            }
        } catch (ClassNotFoundException e) {
            sender.sendMessage(ChatColor.RED + "Could not find class " + className + ".");
        }

        return false;
    }

    private static String formatMethod(Method method) {
        int modifiers = method.getModifiers();
        Class<?> returnType = method.getReturnType();
        Class<?>[] parameterTypes = method.getParameterTypes();

        return formatModifiers(modifiers) + " "
                + returnType.getName() + " "
                + method.getName()
                + Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", ", "(", ")"))
                + ";";
    }

    private static String formatField(Field field) {
        return formatModifiers(field.getModifiers()) + field.getType().getName() + " " + field.getName() + ";";
    }

    private static String formatConstructor(Constructor<?> constructor) {
        return formatModifiers(constructor.getModifiers()) + " <init>"
                + Arrays.stream(constructor.getParameterTypes()).map(Class::getName).collect(Collectors.joining(", ", "(", ")"))
                + ";";
    }

    private static String formatModifiers(int modifiers) {
        // This method does not cover all cases of modifiers, but it works well enough for now.

        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(modifiers)) {
            sb.append("public");
        } else if (Modifier.isProtected(modifiers)) {
            sb.append("protected");
        } else if (Modifier.isPrivate(modifiers)) {
            sb.append("private");
        } else {
            sb.append("<package-private>");
        }

        if (Modifier.isStatic(modifiers)) {
            sb.append(" static");
        }
        if (Modifier.isAbstract(modifiers)) {
            sb.append(" abstract");
        }
        if (Modifier.isFinal(modifiers)) {
            sb.append(" final");
        }

        return sb.toString();
    }
}
