package xyz.janboerman.scalaloader.compat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * This class is NOT part of the public API!
 */
public class Compat {

    private Compat() {}

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) return null;
        return inputStream.readAllBytes();
    }

    public static <T> List<T> listCopy(Collection<T> coll) {
        if (coll == null) return null;
        return List.copyOf(coll);
    }

    public static <T> List<T> listOf(T... items) {
        if (items == null) return null;
        return List.of(items);
    }

    public static <T> List<T> singletonList(T item) {
        return List.of(item);
    }

    public static <T> List<T> emptyList() {
        return List.of();
    }

    public static <T> Set<T> setCopy(Collection<T> coll) {
        if (coll == null) return null;
        return Set.copyOf(coll);
    }

    public static <T> Set<T> setOf(T... items) {
        if (items == null) return null;
        return Set.of(items);
    }

    public static <T> Set<T> singletonSet(T item) {
        return Set.of(item);
    }

    public static <T> Set<T> emptySet() {
        return Set.of();
    }

    public static <K, V> Map<K, V> emptyMap() {
        return Map.of();
    }

    public static <K, V> Map<K, V> mapCopy(Map<K, V> map) {
        if (map == null) return null;
        return Map.copyOf(map);
    }

    public static <K, V> Map<K, V> singletonMap(K key, V value) {
        return Map.of(key, value);
    }

    public static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        if (entries == null) return null;
        return Map.ofEntries(entries);
    }

    public static <K, V> Map.Entry<K, V> mapEntry(K key, V value) {
        return Map.entry(key, value);
    }

    public static String stringRepeat(String base, int repeat) {
        if (base == null) return null;
        return base.repeat(repeat);
    }

    public static JarFile jarFile(File jarFile) throws IOException {
        if (jarFile == null) return null;
        return new JarFile(jarFile, true, ZipFile.OPEN_READ, JarFile.runtimeVersion());
    }

    public static String getPackageName(Class<?> clazz) {
        if (clazz == null) return null;
        return clazz.getPackageName();
    }
}
