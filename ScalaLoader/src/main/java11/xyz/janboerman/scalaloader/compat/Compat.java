package xyz.janboerman.scalaloader.compat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class Compat {

    private Compat() {}

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    public static <T> List<T> listCopy(Collection<T> coll) {
        return List.copyOf(coll);
    }

    public static <T> Set<T> setCopy(Collection<T> coll) {
        return Set.copyOf(coll);
    }

    public static String stringRepeat(String base, int repeat) {
        return base.repeat(repeat);
    }
}
