package xyz.janboerman.scalaloader.compat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
}
