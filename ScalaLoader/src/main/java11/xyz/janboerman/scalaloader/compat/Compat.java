package xyz.janboerman.scalaloader.compat;

import java.io.IOException;
import java.io.InputStream;

public class Compat {

    private Compat() {}

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

}
