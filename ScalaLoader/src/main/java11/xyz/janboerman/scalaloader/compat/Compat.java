package xyz.janboerman.scalaloader.compat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * This class is NOT part of the public API!
 */
public class Compat {

    private Compat() {}

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    public static <T> List<T> listCopy(Collection<T> coll) {
        return List.copyOf(coll);
    }

    public static <T> List<T> listOf(T... items) {
        return List.of(items);
    }

    public static <T> List<T> singletonList(T item) {
        return List.of(item);
    }

    public static <T> List<T> emptyList() {
        return List.of();
    }

    public static <T> Set<T> setCopy(Collection<T> coll) {
        return Set.copyOf(coll);
    }

    public static <T> Set<T> setOf(T... items) {
        return Set.of(items);
    }

    public static <T> Set<T> singletonSet(T item) {
        return Set.of(item);
    }

    public static <T> Set<T> emptySet() {
        return Set.of();
    }

    public static String stringRepeat(String base, int repeat) {
        return base.repeat(repeat);
    }

    public static JarFile jarFile(File jarFile) throws IOException {
        return new JarFile(jarFile, true, ZipFile.OPEN_READ, JarFile.runtimeVersion());
    }
}
