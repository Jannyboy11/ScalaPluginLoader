package xyz.janboerman.scalaloader.compat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public class Compat {

    private Compat() {}

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, bytesRead);
            }
            outputStream.flush();
            return outputStream.toByteArray();
        }
    }

    public static <T> List<T> listCopy(Collection<T> coll) {
        return new ArrayList<>(coll);
    }

    public static <T> Set<T> setCopy(Collection<T> coll) {
        return new LinkedHashSet<>(coll);
    }

    public static String stringRepeat(String base, int repeat) {
        StringJoiner stringJoiner = new StringJoiner("");
        for (int i = 0; i < repeat; ++i) {
            stringJoiner.add(base);
        }
        return stringJoiner.toString();
    }
}
